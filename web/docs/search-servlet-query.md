# SearchServlet: action QUERY

Dokument popisuje akci `QUERY` servletu
`cz.inovatika.arup.digiarchiv.web4.SearchServlet`.

Endpoint:

```text
GET /search/query
POST /search/query
```

Servlet vraci JSON (`application/json;charset=UTF-8`). Nazev akce je bran z
`pathInfo`; `/search/query` se mapuje na enum hodnotu `QUERY`.

## Zpracovani pozadavku

1. `SearchServlet.Actions.QUERY` nacte parametr `entity`.
2. Podle `entity` vybere implementaci `EntitySearcher` pres
   `SearchUtils.getSearcher(entity)`.
3. Neznamy typ entity vrati:

   ```json
   {"error":"unrecognized entity"}
   ```

4. Konkretni searcher sestavi Solr dotaz, aplikuje spolecne parametry,
   filtracni parametry a entitni konfiguraci.
5. Vysledek se vraci jako Solr JSON response. Pri chybe se vraci JSON s
   polozkou `error`.

## Podporovane hodnoty `entity`

`QUERY` technicky podporuje entity registrovane v `SearchUtils.getSearcher`:

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

Poznamka: `server_config.json` obsahuje konfiguraci `fields.archeologicky_zaznam`,
ale tato hodnota neni v soucasne implementaci `SearchUtils.getSearcher` primo
registrovana jako `entity` pro akci `QUERY`.

## Ridici URL parametry

| Parametr | Vychozi hodnota | Popis |
| --- | --- | --- |
| `entity` | povinny | Typ entity pro vyber searcheru. |
| `q` | `*:*` | Hlavni fulltextovy Solr dotaz. Pro prihlasene uzivatele s vyssi pristupnosti se rozsiruje o hledani v `text_all_D` pro jejich organizaci. |
| `rows` | `defaultRows` z klientske konfigurace | Pocet zaznamu na stranku. Ignoruje se pri `mapa=true`, kde se pouzije `mapOptions.docsForMarker`. |
| `page` | `0` | Cislo stranky od nuly. Start se pocita jako `page * rows`. |
| `sort` | prvni vhodny zaznam z `sorts` | Solr sort vyraz, napr. `datestamp desc`. |
| `mapa` | `false` | Rezim mapy; meni pocet a pole vracenych dokumentu podle konkretniho searcheru. |
| `vyber` | - | Prostorovy filtr ve formatu `minLat,minLon,maxLat,maxLon`. |
| `loc_rpt` | - | Prostorovy filtr ve formatu `minLat,minLon,maxLat,maxLon`; pouziva se i pro heatmapu. |
| `inFavorites` | - | Omezi vysledky na oblibene zaznamy aktualniho uzivatele. |
| `inMuseion` | - | Omezi vysledky na zaznamy napojene na Museion. |
| `noFacets` | `false` | Vypne facetovani. |
| `onlyFacets` | `false` | Nastavi `rows=0`, vraci pouze facet/statisticke informace. |
| `noStats` | `false` | Vypne Solr stats. |
| `isExport` | `false` | Nektere searchery podle nej upravuji pole pro exportni rezim. |

Boolean parametry se vyhodnocuji pres `Boolean.parseBoolean`, tedy aktivni
hodnota je retezec `true`.

## Filtrovaci parametry

Filtrovani se aplikuje v `SolrSearcher.addFilters`.

Parametr je zpracovan jako filtr, pokud splni alespon jednu podminku:

- nazev zacina na `f_`;
- nazev je uveden v klientcke konfiguraci `urlFields`;
- nazev je uveden v klientcke konfiguraci `filterFields`;
- nazev je datumove, ciselne nebo rokove pole z `filterFields`.

Hodnoty je mozne zadat opakovanim parametru:

```text
/search/query?entity=dokument&f_obdobi=HES-000001&f_obdobi=HES-000002
```

U facetovych filtru se hodnota standardne cituje. Operatory lze pripojit za
hodnotu pomoci dvojtecky:

| Sufix | Vyklad |
| --- | --- |
| `:or` | OR, vychozi chovani |
| `:and` | povinna hodnota (`+`) |
| `:not` | negace (`-hodnota AND *`) |

Priklad:

```text
/search/query?entity=dokument&f_obdobi=HES-000001:and&f_obdobi=HES-000002:not
```

Textova pole z `filterFields` se skladaji bez lokalniho `{!tag=...}` a u
polozek uvedenych v `server_config.json` v `securedFilters` se doplni suffix
pristupnosti (`_A`, `_B`, `_C`, `_D`).

## Specialni typy filtru

| Typ | Format hodnoty | Priklad |
| --- | --- | --- |
| Rok/cislo | `od,do`; prazdny zacatek znamena `*` | `dokument_rok_vzniku=1990,2000` |
| Datum | `YYYY-MM-DD,YYYY-MM-DD`; `null` znamena otevreny interval | `projekt_datum_zahajeni=2020-01-01,null` |
| `obdobi_poradi` | `od,do` | `obdobi_poradi=100,200` |
| Prostor | `minLat,minLon,maxLat,maxLon` | `loc_rpt=48.5,12.3,51.0,18.8` |

## Parametry odvozene z konfigurace

### Serverova konfigurace

Serverova konfigurace je v:

```text
src/main/resources/cz/inovatika/arup/digiarchiv/web4/server_config.json
```

Pro akci `QUERY` jsou dulezite hlavne:

- `fields.common` - spolecna pole vracena u entit;
- `fields.<entity>.header` a `fields.<entity>.detail` - pole vracena searchery;
- `fields.<entity>.facets` - facetova pole a jejich mapovani;
- `fields.<entity>.full_text` - pole vstupujici do fulltextoveho indexu;
- `securedFacets` - facety se suffixem pristupnosti;
- `securedFilters` - filtry se suffixem pristupnosti.

Facetovy alias je cast pred dvojteckou. Napriklad konfigurace
`f_autor:dokument_autor` znamena URL parametr `f_autor`.

Facetove parametry podle `server_config.json`:

| Konfiguracni entita | Parametry |
| --- | --- |
| `dokument` | `f_pozorovatel`, `f_let_letiste_start`, `f_let_letiste_cil`, `f_let_pocasi`, `f_let_organizace`, `f_let_dohlednost`, `let_letiste_start`, `let_letiste_cil`, `let_pocasi`, `let_organizace`, `let_dohlednost`, `f_zachovalost`, `f_autor`, `f_organizace`, `f_typ_dokumentu_posudek`, `f_typ_dokumentu`, `f_jazyk_dokumentu`, `f_rada`, `f_ulozeni_originalu`, `f_material_dokumentu`, `f_obdobi`, `f_areal`, `f_aktivita`, `f_typ_nalezu`, `f_druh_nalezu`, `f_kategorie`, `f_specifikace`, `f_format`, `f_tvar`, `f_zeme`, `f_osoby`, `f_mimetype` |
| `archeologicky_zaznam` | `f_dj_typ`, `f_obdobi`, `f_areal`, `f_aktivita`, `f_typ_nalezu`, `f_druh_nalezu`, `f_kategorie`, `f_specifikace`, `f_ez_typ`, `f_ez_autor`, `f_ez_casopis`, `f_adb_typ_sondy`, `f_adb_podnet`, `adb_vyskovy_bod_typ`, `f_pian_presnost`, `f_pian_typ`, `f_pian_zm10` |
| `akce` | `f_kraj`, `f_okres`, `f_typ_vyzkumu`, `f_vedouci`, `f_organizace` |
| `lokalita` | `f_okres`, `f_typ_lokality`, `f_druh_lokality`, `f_jistota`, `f_lokalita_zachovalost` |
| `projekt` | `f_organizace`, `f_kraj`, `f_okres`, `f_katastr`, `f_vedouci`, `f_typ_vyzkumu`, `f_typ_projektu` |
| `samostatny_nalez` | `f_organizace`, `f_okres`, `f_katastr`, `f_obdobi`, `f_druh_nalezu`, `f_kategorie`, `f_specifikace`, `f_nalezce`, `f_nalezove_okolnosti`, `f_mimetype` |
| `komponenta` | `f_obdobi`, `f_areal`, `f_aktivita`, `f_typ_nalezu`, `f_druh_nalezu`, `f_kategorie`, `f_specifikace`, `f_kraj`, `f_okres`, `f_katastr`, `f_vedouci`, `f_organizace`, `f_typ_vyzkumu`, `f_typ_lokality`, `f_druh_lokality`, `dokument_kategorie_dokumentu`, `f_typ_dokumentu`, `f_rada`, `f_tvar`, `az_chranene_udaje`, `dokument_extra_data`, `f_dj_typ`, `f_adb_typ_sondy`, `f_adb_podnet`, `adb_vyskovy_bod_typ` |

Zabezpecene facety:

```text
f_katastr, adb_vyskovy_bod_typ, f_pian_zm10
```

Zabezpecene filtry:

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

### Klientska konfigurace sloucena za behu

`Options` nacita vychozi klientskou konfiguraci z `assets/config.json` a muze ji
sloucit s externim `CONFIG_DIR/config.json`. Proto jsou pro URL filtry dulezite
i polozky `urlFields` a `filterFields`.

Vychozi `urlFields`:

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
samostatny_nalez_predano_organizace, predmet_kategorie
```

Vychozi `filterFields` jsou textova, boolean, datumova, ciselna a rokova pole
pro rozsirene hledani. Aktualni uplny seznam je v
`src/main/ng/src/assets/config.json`.

## Priklady

Zakladni fulltext v dokumentech:

```text
/search/query?entity=dokument&q=keramika
```

Dokumenty podle obdobi a okresu:

```text
/search/query?entity=dokument&f_obdobi=HES-000001&f_okres=CZ0201
```

Projekty podle roku zahajeni:

```text
/search/query?entity=projekt&projekt_datum_zahajeni=2020-01-01,2020-12-31
```

Pouze facety pro samostatne nalezy:

```text
/search/query?entity=samostatny_nalez&onlyFacets=true
```

Mapovy dotaz v rozsahu:

```text
/search/query?entity=akce&mapa=true&loc_rpt=48.5,12.3,51.0,18.8
```
