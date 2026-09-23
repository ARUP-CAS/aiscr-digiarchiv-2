# SearchServlet: action QUERY

Dokument popisuje akci `QUERY` servletu
`cz.inovatika.arup.digiarchiv.web4.SearchServlet`.

Endpoint:

```text
GET /search/query
POST /search/query
```

Servlet vrací JSON (`application/json;charset=UTF-8`). Název akce je brán z
`pathInfo`; `/search/query` se mapuje na enum hodnotu `QUERY`.

## Zpracování požadavku

1. `SearchServlet.Actions.QUERY` načte parametr `entity`.
2. Podle `entity` vybere implementaci `EntitySearcher` přes
   `SearchUtils.getSearcher(entity)`.
3. Neznámý typ entity vrátí:

   ```json
   {"error":"unrecognized entity"}
   ```

4. Konkrétní vyhledávač sestaví dotaz pro Solr, aplikuje společné parametry,
   filtrační parametry a entitní konfiguraci.
5. Výsledek se vrací jako odpověď služby Solr ve formátu JSON. Při chybě se vrací JSON s
   položkou `error`.

## Podporované hodnoty `entity`

`QUERY` technicky podporuje entity registrované v `SearchUtils.getSearcher`:

| Hodnota `entity` | Searcher |
| --- | --- |
| `akce` | `AkceSearcher` |
| `lokalita` | `LokalitaSearcher` |
| `projekt` | `ProjektSearcher` |
| `samostatny_nalez` | `SamostatnyNalezSearcher` |
| `knihovna_3d` | `DokumentSearcher("knihovna_3d")` |
| `let` | `LetSearcher` |
| `ext_zdroj` | `ExtZdrojSearcher` |
| `pian` | `PIANSearcher` |
| `adb` | `ADBSearcher` |
| `dokumentacni_jednotka` | `DokJednotkaSearcher` |
| `dokument_cast` | `DokumentCastSearcher` |
| `dokument` | `DokumentSearcher("dokument")` |
| `komponenta` | `KomponentaSearcher` |
| `vyskovy_bod` | `VyskovyBodSearcher` |

Poznámka: `server_config.json` obsahuje konfiguraci `fields.archeologicky_zaznam`,
ale tato hodnota není v současné implementaci `SearchUtils.getSearcher` přímo
registrována jako `entity` pro akci `QUERY`.

## Řídicí URL parametry

| Parametr | Výchozí hodnota | Popis |
| --- | --- | --- |
| `entity` | povinný | Typ entity pro výběr vyhledávače. |
| `q` | `*:*` | Hlavní fulltextový Solr dotaz. Pro přihlášené uživatele s vyšší přístupností se rozšiřuje o hledání v `text_all_D` pro jejich organizaci. |
| `rows` | `defaultRows` z klientské konfigurace | Počet záznamů na stránku. Ignoruje se při `mapa=true`, kde se použije `mapOptions.docsForMarker`. |
| `page` | `0` | Číslo stránky od nuly. Start se počítá jako `page * rows`. |
| `sort` | první vhodný záznam z `sorts` | Solr sort výraz, např. `datestamp desc`. |
| `mapa` | `false` | Režim mapy; mění počet a pole vrácených dokumentů podle konkrétního vyhledávače. |
| `vyber` | - | Prostorový filtr ve formátu `minLat,minLon,maxLat,maxLon`. |
| `loc_rpt` | - | Prostorový filtr ve formátu `minLat,minLon,maxLat,maxLon`; používá se i pro heatmapu. |
| `inFavorites` | nepřítomen | Pokud je parametr přítomen, omezí výsledky na nejvýše 100 oblíbených záznamů aktuálního uživatele. Hodnota parametru se nevyhodnocuje. |
| `inMuseion` | nepřítomen | Pokud je parametr přítomen, omezí výsledky na záznamy napojené na Museion. Hodnota parametru se nevyhodnocuje. |
| `noFacets` | `false` | Vypne facetování. |
| `onlyFacets` | `false` | Nastaví `rows=0`, vrací pouze facety a statistické informace. |
| `noStats` | `false` | Vypne statistiky služby Solr. |
| `isExport` | `false` | Některé vyhledávače podle něj upravují pole pro exportní režim. |

Parametry `mapa`, `noFacets`, `onlyFacets`, `noStats` a `isExport` se
vyhodnocují pomocí `Boolean.parseBoolean`, takže je aktivuje hodnota `true`.
Parametry `inFavorites` a `inMuseion` se naproti tomu aktivují samotnou
přítomností v požadavku; například `inFavorites=false` tedy filtr také zapne.

## Filtrovací parametry

Filtrování se aplikuje v `SolrSearcher.addFilters`.

Parametr je zpracován jako filtr, pokud splní alespoň jednu podmínku:

- název začíná na `f_`;
- název je uveden v klientské konfiguraci `urlFields`;
- název je uveden v klientské konfiguraci `filterFields`;
- název je datumové, číselné nebo rokové pole z `filterFields`.

Hodnoty je možné zadat opakováním parametru:

```text
/search/query?entity=dokument&f_obdobi=HES-000001&f_obdobi=HES-000002
```

U facetových filtrů se hodnota standardně cituje. Operátory lze připojit za
hodnotu pomocí dvojtečky:

| Sufix | Výklad |
| --- | --- |
| `:or` | OR, výchozí chování |
| `:and` | povinná hodnota (`+`) |
| `:not` | negace (`-hodnota AND *`) |

Příklad:

```text
/search/query?entity=dokument&f_obdobi=HES-000001:and&f_obdobi=HES-000002:not
```

Textová pole z `filterFields` se skládají bez lokálního `{!tag=...}` a u
položek uvedených v `server_config.json` v `securedFilters` se doplní přípona
přístupnosti (`_A`, `_B`, `_C`, `_D`).

## Speciální typy filtrů

| Typ | Formát hodnoty | Příklad |
| --- | --- | --- |
| Rok/číslo | `od,do`; prázdný začátek znamená `*` | `dokument_rok_vzniku=1990,2000` |
| Datum | `YYYY-MM-DD,YYYY-MM-DD`; `null` znamená otevřený interval | `projekt_datum_zahajeni=2020-01-01,null` |
| `obdobi_poradi` | `od,do` | `obdobi_poradi=100,200` |
| Prostor | `minLat,minLon,maxLat,maxLon` | `loc_rpt=48.5,12.3,51.0,18.8` |

## Parametry odvozené z konfigurace

### Serverová konfigurace

Serverová konfigurace je v:

```text
src/main/resources/cz/inovatika/arup/digiarchiv/web4/server_config.json
```

Za běhu ji může přepsat nebo doplnit sekce `server` z externího souboru
`CONFIG_DIR/config.json`.

Pro akci `QUERY` jsou důležité hlavně:

- `fields.common` - společná pole vrácená u entit;
- `fields.<entity>.header` a `fields.<entity>.detail` - pole vrácená vyhledávači;
- `fields.<entity>.facets` - facetová pole a jejich mapování;
- `fields.<entity>.full_text` - pole vstupující do fulltextového indexu;
- `securedFacets` - facety s příponou přístupnosti;
- `securedFilters` - filtry s příponou přístupnosti.

Alias facety je část před dvojtečkou. Například konfigurace
`f_autor:dokument_autor` znamená URL parametr `f_autor`.

Facetové parametry podle `server_config.json`:

| Konfigurační entita | Parametry |
| --- | --- |
| `dokument` | `f_pozorovatel`, `f_let_letiste_start`, `f_let_letiste_cil`, `f_let_pocasi`, `f_let_organizace`, `f_let_dohlednost`, `let_letiste_start`, `let_letiste_cil`, `let_pocasi`, `let_organizace`, `let_dohlednost`, `f_zachovalost`, `f_autor`, `f_organizace`, `f_typ_dokumentu_posudek`, `f_typ_dokumentu`, `f_jazyk_dokumentu`, `f_rada`, `f_ulozeni_originalu`, `f_material_dokumentu`, `f_obdobi`, `f_areal`, `f_aktivita`, `f_typ_nalezu`, `f_druh_nalezu`, `f_kategorie`, `f_specifikace`, `f_format`, `f_tvar`, `f_zeme`, `f_osoby`, `f_mimetype` |
| `archeologicky_zaznam` | `f_dj_typ`, `f_obdobi`, `f_areal`, `f_aktivita`, `f_typ_nalezu`, `f_druh_nalezu`, `f_kategorie`, `f_specifikace`, `f_ez_typ`, `f_ez_autor`, `f_ez_casopis`, `f_adb_typ_sondy`, `f_adb_podnet`, `adb_vyskovy_bod_typ`, `f_pian_presnost`, `f_pian_typ`, `f_pian_zm10` |
| `akce` | `f_kraj`, `f_okres`, `f_typ_vyzkumu`, `f_vedouci`, `f_organizace` |
| `lokalita` | `f_okres`, `f_typ_lokality`, `f_druh_lokality`, `f_jistota`, `f_lokalita_zachovalost` |
| `projekt` | `f_organizace`, `f_kraj`, `f_okres`, `f_katastr`, `f_vedouci`, `f_typ_vyzkumu`, `f_typ_projektu` |
| `samostatny_nalez` | `f_organizace`, `f_okres`, `f_katastr`, `f_obdobi`, `f_druh_nalezu`, `f_kategorie`, `f_specifikace`, `f_nalezce`, `f_nalezove_okolnosti`, `f_mimetype` |
| `komponenta` | `f_obdobi`, `f_areal`, `f_aktivita`, `f_typ_nalezu`, `f_druh_nalezu`, `f_kategorie`, `f_specifikace`, `f_kraj`, `f_kraj_rada`, `f_okres`, `f_katastr`, `f_vedouci`, `f_organizace`, `f_typ_vyzkumu`, `f_typ_lokality`, `f_druh_lokality`, `dokument_kategorie_dokumentu`, `f_typ_dokumentu`, `f_rada`, `f_tvar`, `stav`, `az_chranene_udaje`, `dokument_extra_data`, `dokument_cast_archeologicky_zaznam`, `f_dj_typ`, `f_adb_typ_sondy`, `f_adb_podnet`, `adb_vyskovy_bod_typ`, `f_pian_typ`, `f_pian_presnost`, `f_pian_zm10`, `pian_id`, `pian_ident_cely`, `lat`, `lng`, `loc`, `loc_rpt` |

Zabezpečené facety:

```text
f_katastr, adb_vyskovy_bod_typ, f_pian_zm10
```

Zabezpečené filtry:

```text
projekt_chu_lokalizace, projekt_chranene_udaje.hlavni_katastr,
projekt_chranene_udaje.dalsi_katastr, projekt_chranene_udaje_lokalizace,
projekt_chranene_udaje_parcelni_cislo, projekt_chranene_udaje.geom_gml,
projekt_chranene_udaje.geom_wkt, projekt_chranene_udaje_kulturni_pamatka_cislo,
projekt_chranene_udaje_kulturni_pamatka_popis, akce_lokalizace_okolnosti,
lokalita_nazev, f_uzivatelske_oznaceni, adb_uzivatelske_oznaceni_sondy,
adb_trat, adb_cislo_popisne, adb_parcelni_cislo, adb_poznamka, f_okres,
f_katastr, adb_vyskovy_bod_typ, akce_chranene_udaje_souhrn_upresneni,
lokalita_popis, lokalita_poznamka, samostatny_nalez_lokalizace
```

### Klientská konfigurace sloučená za běhu

`Options` načítá výchozí klientskou konfiguraci z `assets/config.json` a může ji
sloučit s externím `CONFIG_DIR/config.json`. Proto jsou pro URL filtry důležité
i položky `urlFields` a `filterFields`.

Výchozí `urlFields`:

```text
f_organizace, dokument_rok_vzniku, f_okres, f_katastr, f_obdobi, f_areal,
f_aktivita, f_druh_lokality, f_druh_nalezu, f_kategorie, f_autor,
f_vedlejsi_typ, f_hlavni_typ, f_material_dokumentu, f_jazyk_dokumentu, f_rada,
dokument_kategorie_dokumentu, dokument_licence, pristupnost, kategorie,
f_typ_dokumentu, pian_ident_cely, pian_id, obdobi_poradi,
lokalita_zachovalost, extra_data_datum_vzniku, datum_zverejneni, rok_vzniku,
komponenta_dokument_obdobi, adb_vyskovy_bod_typ, let_letiste_start,
let_letiste_cil, extra_data_format, let_organizace, let_pocasi,
let_dohlednost, tvar_tvar, typ, f_zachovalost, f_nahrada, f_zeme,
f_typ_lokality, f_typ_projektu, inv_cislo,
samostatny_nalez_predano_organizace, predmet_kategorie, soubor_distri,
f_pozorovatel
```

Výchozí seznam `filterFields` obsahuje textová, logická, datumová, číselná
a roční pole pro rozšířené hledání. Aktuální úplný seznam je v
`src/main/ng/src/assets/config.json`.

## Příklady

Základní fulltext v dokumentech:

```text
/search/query?entity=dokument&q=keramika
```

Dokumenty podle období a okresu:

```text
/search/query?entity=dokument&f_obdobi=HES-000001&f_okres=CZ0201
```

Projekty podle roku zahájení:

```text
/search/query?entity=projekt&projekt_datum_zahajeni=2020-01-01,2020-12-31
```

Pouze facety pro samostatné nálezy:

```text
/search/query?entity=samostatny_nalez&onlyFacets=true
```

Mapový dotaz v rozsahu:

```text
/search/query?entity=akce&mapa=true&loc_rpt=48.5,12.3,51.0,18.8
```
