# ImageServlet

Dokument popisuje servlet
`cz.inovatika.arup.digiarchiv.web4.ImageServlet`, který zpřístupňuje náhledy
a distribuční soubory uložené v úložišti Fedora.

Servlet je namapován na cestu `/img/*` a podporuje metody `GET` i `POST`.
Akce se určuje z `pathInfo` bez ohledu na velikost písmen.

## Endpointy

| Akce | Endpoint | Význam |
| --- | --- | --- |
| `THUMB` | `/img/thumb` | Malý náhled souboru. |
| `MEDIUM` | `/img/medium` | Větší náhled souboru s kontrolou přístupových práv. |
| `FULL` | `/img/full` | Původní nebo jiný distribuční soubor. |

Všechny endpointy lze volat metodou `GET` i `POST`. Parametry se načítají
pomocí `HttpServletRequest.getParameter`, takže mohou být součástí URL nebo
formulářového těla požadavku.

## Parametry

| Parametr | Akce | Povinnost | Popis |
| --- | --- | --- | --- |
| `id` | všechny | ano | Identifikátor souboru, tedy hodnota `soubor.id` a indexovaného pole `soubor_id`. |
| `dist` | `FULL` | ano | Cesta distribuce. Musí přesně odpovídat položce `path` v poli `soubor.distribuce`. |
| `dist` | `THUMB`, `MEDIUM` | ne | Parametr se předává pomocné metodě, ale současná implementace jej nepoužívá. |
| `field` | `MEDIUM`, `FULL` | ne | Kontrola přístupu jej načte s výchozí hodnotou `dokument`, ale dále jej nepoužívá. Na výsledek proto nemá vliv. |

## Vyhledání souboru

Před načtením dat z úložiště Fedora servlet vyhledá odpovídající dokument
v Solr kolekci `entities`:

1. filtruje podle `soubor_id:"<id>"` a `searchable:true`;
2. výsledky řadí podle `datestamp desc`;
3. z prvního výsledku načte pole `entity` a `soubor`;
4. v poli `soubor` vybere položku, jejíž `id` se přesně shoduje s parametrem
   požadavku.

Jestliže dokument nebo odpovídající položka `soubor` neexistuje, metoda pro
vyhledání vrátí `null`. Výsledná odpověď potom závisí na zvolené akci.

## Akce `THUMB`

Příklad:

```text
GET /img/thumb?id=SOUBOR-ID
```

Akce je veřejná a neprovádí kontrolu přístupových práv. Z hodnoty `soubor.path`
sestaví cestu `<path>/thumb`, načte obrázek z úložiště Fedora a:

- převede jej do formátu PNG;
- přidá vodoznak `assets/img/logo-watermark-white.png`;
- nastaví `Content-Type: image/png`;
- nastaví hlavičku `Content-Disposition: filename=<id>`.

Průhlednost vodoznaku určuje serverová volba `watermark.alpha`. Výchozí
konfigurace repozitáře používá hodnotu `0`, takže vodoznak není viditelný.

Pokud parametr `id` chybí, záznam nelze najít, Fedora nevrátí obrázek nebo
načtená data nejsou platným obrázkem, servlet vrátí zástupný soubor
`assets/img/empty.png` s typem `image/png`. Stavový kód přitom zůstává zpravidla
`200 OK`.

## Akce `MEDIUM`

Příklad:

```text
GET /img/medium?id=SOUBOR-ID
```

Akce nejprve ověří přístupová práva. Při povoleném přístupu se chová stejně
jako `THUMB`, ale z úložiště Fedora načítá cestu `<path>/thumb-large`.
Výsledkem je opět obrázek PNG s vodoznakem.

Při zamítnutém přístupu vrací současná implementace stav `401 Unauthorized`
a tělo typu `text/html;charset=UTF-8`. Text stránky se však skládá z lokalizované
zprávy určené pro kód `403`. Jde o skutečné chování implementace, nikoli o
doporučené rozhraní.

Chybějící `id` a chyby při načítání náhledu vedou stejně jako u akce `THUMB`
k vrácení zástupného obrázku.

## Akce `FULL`

Příklad:

```text
GET /img/full?id=SOUBOR-ID&dist=original/file.pdf
```

Akce provede následující kroky:

1. zkontroluje omezení četnosti požadavků podle IP adresy a identifikátoru;
2. ověří přístupová práva;
3. vyhledá soubor v Solr;
4. v poli `soubor.distribuce` vyhledá položku s cestou shodnou s parametrem
   `dist`;
5. načte distribuční soubor z úložiště Fedora do dočasného souboru v adresáři
   `InitServlet.TEMP_DIR`;
6. zkopíruje obsah do odpovědi a dočasný soubor odstraní;
7. zaznamená stažení pomocí `LogAnalytics`.

Typ odpovědi se přebírá z hodnoty `mimetype` vybrané distribuce. Hlavička
`Content-Disposition` obsahuje pouze `filename=<filename>` bez direktivy
`attachment`, takže způsob zobrazení nebo stažení určuje klient podle typu
obsahu.

Na rozdíl od náhledových akcí se obsah nijak nepřevádí a nepřidává se do něj
vodoznak.

### Omezení četnosti požadavků

Akce `FULL` volá `AppState.canGetFileInterval` ještě před ověřením povinných
parametrů a přístupových práv.

- Je-li další požadavek povolen až později, servlet vrátí `429 Too Many Requests`,
  hlavičku `Retry-After` a anglickou textovou zprávu.
- Probíhá-li pro stejnou IP adresu jiné evidované stahování, servlet vrátí
  `429 Too Many Requests` bez hlavičky `Retry-After`.
- Identifikátory obsahující řetězec `thumb` jsou z omezení vyňaty.

Interval určuje serverová volba `requestInterval`. Výchozí konfigurace
repozitáře obsahuje hodnotu `500` ms; pokud volba chybí, kód používá
náhradní hodnotu `5000` ms.

`ImageServlet` sdílený stav pouze čte. Sám nevolá metody
`AppState.writeGetFileStarted` ani `AppState.writeGetFileFinished`; stav
stahování aktualizuje jiná část aplikace, zejména `HandleServlet`.

## Kontrola přístupových práv

Akce `MEDIUM` a `FULL` používají `ImageAccess.isAllowed(request, true)`.
Kontrola nejprve podle `soubor_id` vyhledá v kolekci `entities` pole
`pristupnost`, `organizace` a `entity`.

Přístup je povolen v těchto případech:

- soubor patří entitě `samostatny_nalez`; pro ni se další omezení neuplatňuje;
- hodnota `pristupnost` souboru je `A`;
- úroveň přístupu uživatele je alespoň stejná jako úroveň souboru;
- uživatel patří ke stejné organizaci a vyhoví doplňkové podmínce implementace
  pro úrovně `A` až `C`.

Akce `THUMB` tuto kontrolu neprovádí. U všech akcí však vlastní načtení souboru
vyžaduje, aby byl nadřazený záznam v indexu označen jako `searchable:true`.

## Odpovědi a chyby

| Situace | Odpověď současné implementace |
| --- | --- |
| Úspěšný `THUMB` nebo `MEDIUM` | `200 OK`, `image/png`. |
| Chybějící náhled nebo chyba jeho načtení | Zástupný obrázek PNG, zpravidla `200 OK`. |
| Zamítnutý přístup k `MEDIUM` | `401 Unauthorized`, stránka HTML. |
| Zamítnutý přístup k `FULL` | `401 Unauthorized`, prostý anglický text. |
| Distribuce `dist` u `FULL` neexistuje | `404 Not Found`, text `Distribuce not found`. |
| Překročené omezení četnosti | `429 Too Many Requests`. |
| Chybějící `id` u `FULL` | V současné implementaci `500 Internal Server Error`, protože se omezení četnosti vyhodnotí před kontrolou parametru. |
| Chybějící `dist` u jinak povoleného `FULL` | V současné implementaci `500 Internal Server Error`. |
| Neznámá akce, například `/img/unknown` | `500 Internal Server Error`. |
| Požadavek na `/img` bez další části cesty | `200 OK` s textem `action -> null`. |
| Neočekávaná `SecurityException` | `403 Forbidden`. |
| Ostatní neošetřené chyby | `500 Internal Server Error`. |

Pokud kontrola přístupu uspěje, ale následné vyhledání nenajde záznam
s `searchable:true`, může akce `FULL` skončit prázdnou odpovědí bez výslovného
chybového stavového kódu.

## Sestavení cesty do úložiště Fedora

Zdrojová cesta vzniká spojením hodnoty `soubor.path` s názvem náhledu nebo
s parametrem `dist`:

```text
<soubor.path>/thumb
<soubor.path>/thumb-large
<soubor.path>/<dist>
```

Obsahuje-li výsledná cesta řetězec `record`, servlet odstraní vše před jeho
prvním výskytem. Takto upravenou cestu připojí `FedoraUtils` k hodnotě
`fedora.api.point` a odešle autentizovaný požadavek `GET`.

## Související konfigurace a soubory

| Položka | Význam |
| --- | --- |
| `solrhost` | Základní adresa serveru Solr. |
| `fedora.api.point` | Základní adresa REST API úložiště Fedora. |
| `fedora.user`, `fedora.pwd` | Přihlašovací údaje používané pro HTTP Basic Authentication vůči úložišti Fedora. |
| `watermark.alpha` | Průhlednost vodoznaku náhledů. |
| `requestInterval` | Minimální interval mezi evidovanými stahováními pro jednu IP adresu. |
| `assets/img/logo-watermark-white.png` | Obrázek vodoznaku. |
| `assets/img/empty.png` | Zástupný obrázek vracený při chybě náhledu. |
| `InitServlet.TEMP_DIR` | Adresář dočasných souborů akce `FULL`. |

Výchozí serverová konfigurace se načítá ze souboru
`src/main/resources/cz/inovatika/arup/digiarchiv/web4/server_config.json`.
Za běhu ji může přepsat nebo doplnit sekce `server` z externího souboru
`CONFIG_DIR/config.json`.

## Další příklady

Malý náhled:

```text
/img/thumb?id=SOUBOR-ID
```

Větší náhled:

```text
/img/medium?id=SOUBOR-ID
```

Stažení obrazové distribuce:

```text
/img/full?id=SOUBOR-ID&dist=original/image.tif
```

Stažení dokumentu PDF:

```text
/img/full?id=SOUBOR-ID&dist=original/document.pdf
```
