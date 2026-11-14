# Komponenta (cross-cutting record type) — testing scenario

**Key:** komponenta (entity)
**Scope:** the cross-cutting **Komponenty** record type — entity registration, search/facets/sorts, record pages per origin, permission behaviour, map, export, and conformance to the display specification (revision document with tracked changes and comments); issue [ARUP-CAS/aiscr-digiarchiv-2#127](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/127) drove it. Mechanisms this entity depends on — record-level gating, element-level protection, the export feature — are owned by their feature scenarios ([`permissions`](permissions.md), [`export`](export.md)); this scenario records only this entity's deviations from them.
**Principle:** concrete record ids are deliberately not embedded — record states drift. Use the discovery recipes below to find fresh candidates; a verification command may carry an identifier placeholder the recipe fills.

## Durable knowledge

> Amend this half where a run found it incomplete or wrong; never re-derive it.

### Environments and endpoints

- Record page: `https://digiarchiv-test.aiscr.cz/id/<ident_cely>`, data via `…/api/search/handle?id=<ident>` on `digiarchiv-test`; restricted records 404 the landing page for anonymous visitors (record-level gate, [`permissions`](permissions.md)).
- Entity search with facets: `…/api/search/query?entity=komponenta&q=…`; the QUERY action is addressed as a path segment (`/api/search/query?…`) — path-style params under `/api/search/<params>` hit the servlet's action enum and 500.
- Map mode: `mapa=true` (see the [`permissions`](permissions.md) and [`export`](export.md) scenarios for the surfaces' mechanics); komponenty with coordinates are served with `loc`, `lat`/`lng` stats, and the `pian` block including full geometry.
- Export: the four download formats of the [`export`](export.md) feature.
- Deployed frontend templates are verifiable without a browser: the served page shell names the module-preloaded chunks, the `/id/:id` route lazy-loads the document chunk, and the entity components (komponenta included) are compiled into one of the preloaded chunks. Fetch that chunk and search the compiled template with a UTF-8-aware search — Windows code-page tools mangle the diacritics in the i18n keys, so a code-page search silently finds nothing.
- Deployed build identity is readable without any version endpoint: the served `main-*.js` bundle embeds git-build metadata (`{raw: "v4.0.3-221-g75788eaf", hash: "g75788eaf", distance, tag}` — search the bundle for `hash:"g`). The page-map chunk carrying the map-view implementation is referenced from `main.js` chunk URLs and is lazy-loaded, not modulepreloaded.

### Architecture and implementation facts

- The authoritative display specification is the revision document (`Digiarchiv_zaznamy_vypis_revize.docx`) — a Word file **with tracked changes and comments**. Reading it naively loses the deltas.
- **Spec comments (C0–C10)** flag items missing at review time; C6/C7/C8 (2023) give the xpath sources for SN/3D geometry.
- **Tracked insertion:** a leading space followed by `/ {extra_data/geom_system}` added to the *Primární EPSG* line — the 3D source variant (comment "Platí pro 3D (opomněl jsem)").
- **Extraction method (docx with tracked changes/comments):** unzip the docx, walk `word/document.xml` paragraph by paragraph — normal runs from `w:r/w:t`, insertions wrapped from `w:ins`, deletions from `w:del/w:delText`, comment anchors from `commentRangeStart/End` ids joined to `word/comments.xml` (id → author/date/text). A plain-text or pandoc-style conversion silently drops both.
- **Indexing (`Komponenta.java`):** SN-derived komponenty are **virtual index documents** — `fromSamostatnyNalez` deep-copies the SN document, sets `ident_cely` to `<SN ident>-K001`, records the parent in `komponenta_zdroj`/`komponenta_zdroj_ident_cely`, strips only the `soubor*` fields, and re-commits; the copy therefore carries the SN's protected fields (`samostatny_nalez_chranene_udaje`, katastr, exact geometry) into the komponenta index, where the search/handle path masks them per role instead of the index dropping them. AZ-origin komponenty receive `az_chranene_udaje` facet fields copied from the parent root, suffix-gated per the client config field lists. `filterOAI` returns `true` unconditionally. Where the parent chain carries a PIAN, `pristupnost` is re-read from the PIAN document (the PIAN chain rule from issue #127), so a komponenta inherits the PIAN's access level.
- **Územní příslušnost facet (`f_kraj_rada`):** declared in the client config facet list; the indexed values are the two region codes `C` (Čechy) and `M` (Morava a Slezsko), translated client-side from the i18n `f_kraj_rada.*` value map (the facets component renders `'f_kraj_rada.' + facet.name`; the used-facets chip renders the same prefix). The value originates as the okres `rada_id`, indexed on every entity indexing path (`ArcheologickyZaznam`, `DokumentCast`, `NeidentAkce`, `Projekt`, `SamostatnyNalez`); komponenta docs inherit it through the parent chain, and facet counts cover only docs that carry the field.
- **Frontend template (`komponenta.component.html`):** geometry rows render only for SN (`samostatny_nalez_chranene_udaje.geom_*`) and 3D (`dokument_extra_data.geom_*`) sources, each guarded on data presence; the PIAN block renders from the `pian` array (passed with `[inKomponenta]="true"`); the AMCR quick-link ident is `ident_cely.substr(0, ident_cely.lastIndexOf("-"))` — the parent ident — and the persistent-link badge likewise uses the SN parent ident for SN-origin komponenty. The období header binds `komponenta_obdobi.id` with a client-side `heslar` lookup (`obdobi_druha`), so a null `value` in a served search doc has no display consequence.
- **Map integration (`map-view.component.ts`):** komponenty are routed by `entity === 'komponenta'` into the by-loc marker path (`setMarkersByLoc`): markers are built from the embedded `pian[0]` block (ident, přesnost, typ, `pian_chranene_udaje`), and the geometry shape layer is added from `pian[0].pian_chranene_udaje.geom_wkt.value` (`addShapeLayer`). Clicking a card title or the map button navigates to `/map` with `mapa=true&mapId=<ident>`; the map view then resolves the record (`getHandle`/`getMarkerById` — both endpoints serve komponenta ids with `loc_rpt` and the `pian` block) and zooms on the record's `loc_rpt` bounds. The card-level map proklik button (`result-actions`) renders whenever the result carries `chranene_udaje`, `loc_rpt`, or `loc`.

### Feature or entity model

- **Origins (`komponenta_zdroj`):** `akce` (~129k), `dokument` (~81k), `samostatny_nalez` (~3.4k), `knihovna_3d` (~0.7k); parent ident in `komponenta_zdroj_ident_cely`. Around 172k carry objekt/předmet nálezy (`komponenta_typ_nalezu:*`).
- **Registration:** in the client config `entities` list; icon `extension`; `exportFields.komponenta` defined; `sorts` entries present — `komponenta_obdobi_poradi` and `komponenta_areal_poradi` (the heslo `poradi` order agreed for areál/období), `katastr_sort`, `datestamp`, and `okres_sort` in both directions. The poradi sort fields are populated on all indexing paths (akce, SN, dokument).
- **Gating:** komponenta docs inherit `searchable` from the parent — the mechanism and its rules are owned by the [`permissions`](permissions.md) scenario.
- **Per-origin data paths on the record page (handle API):** AZ → `az_chranene_udaje`, parent `akce` fields, `pian` JSON (typ, přesnost, ZM10, geoms); SN → `samostatny_nalez_geom_system`, `samostatny_nalez_chranene_udaje` (geom/katastr/lokalizace), top-level `presna_datace` alias (from `samostatny_nalez_presna_datace`), parent ident in two served field locations (the embedded `samostatny_nalez` block feeding the Původ row and `komponenta_zdroj_ident_cely`); 3D → `dokument_extra_data` (geom, odkaz, `geom_system`), `komponenta_nalez_objekt/predmet` for Nálezy.
- **Display template (the spec's dedicated "Komponenty {komponenta}" section):** header line `ident_cely | období (přesná datace) jistota | areál`, `Původ` (parent with a quick link, i.e. proklick), `Přístupnost`, `Katastr (okres)` / `Další katastry` / `Aktivity` / `Poznámka`, a position block (**SN and 3D sources only** — not AZ), a PIAN block (AZ source), `Nálezy` (objekt/předmet lines + the SN druh line), and Související záznamy (parent idents). Sorting: poslední změna, katastr, ident_cely, okres+katastr, období (pořadí), areál. Access rule: inaccessible records show without `[chranene_udaje]`, are not searchable by them and not visible in the map. Indexation: only when the parent (AZ/dokument/SN) is archived and not deleted.

### Discovery recipes

1. **Per-origin samples:** `GET /api/search/query?entity=komponenta&rows=2&q=komponenta_zdroj:<akce|dokument|samostatny_nalez|knihovna_3d>`.
2. **Nálezy-bearing samples:** `GET /api/search/query?entity=komponenta&rows=2&q=komponenta_typ_nalezu:*` — the field is set only when the komponenta has objekt/předmet nálezy.
3. **Restricted (pristupnost > A) samples for leak tests:** take any SN with pristupnost > A from the [`permissions`](permissions.md) discovery recipes, append the `-K001` suffix, or filter komponenty by a restricted parent's ident. Verify the parent's current state before use — an unarchived parent (ident prefix `X-`) keeps its komponenty correctly out of anonymous search, and a nonexistent ident is indistinguishable from a gated one on numFound alone; check the parent record itself.
4. **Gating checks:** parent state via OAI (`GetRecord`), komponenta searchability via `/api/search/query?entity=komponenta&q=ident_cely:"…"`, landing via `/id/<ident>`.
5. **Record-page data:** `/api/search/handle?id=<komponenta>` — compare served fields against the display template above and against the parent's own page (the parent page is the protection ground truth).
6. **Facet-value checks:** `GET /api/search/query?entity=komponenta&rows=0&facet=true&facet.limit=-1&facet.field=<field>` — read `facet_counts.facet_fields` and compare the returned values against the i18n value map for the field.

## Current verification (2026-09-15, `digiarchiv-test` build `v4.0.3-221-g75788eaf`, frontend bundle `main-MSW4TEIM.js`; the maintainer confirmed the four browser-level aspects in the same session)

Regression pass on issue #127 responding to five newly reported defects (the maintainer's comment with screenshots; fixes indicated in the thread — PIAN geometries added, Územní příslušnost indexed). Anonymous API probes plus compiled-bundle inspection; browser-level outcomes maintainer-confirmed in-session.

### Verified behaviour matrix

| Capability | Result |
| --- | --- |
| Územní příslušnost facet values (komponenta-D18) | works — `f_kraj_rada` returns exactly `C` (85,338) and `M` (17,447), no garbage values; the i18n value map (`f_kraj_rada.C/M`) and the facets-component translation case are present in the deployed bundle |
| Map proklik on komponenta cards (komponenta-D15) | works — entity search results carry `loc_rpt` (live probe), satisfying the `result-actions` render condition; the `showInMap` binding is present in the deployed komponenta template chunk; the `mapId` → handle → zoom chain resolves (handle API serves `loc_rpt`); click-through maintainer-confirmed |
| Map list click → detail + zoom (komponenta-D16) | works — `/api/search/id` resolves a komponenta ident with `loc_rpt` and the `pian` block; the deployed map-view chunk carries the `entity === "komponenta"` by-loc branch (marker from `pian[0]`, hit icon, zoom on `loc_rpt`); click-through maintainer-confirmed |
| PIAN geometries on the komponenta map (komponenta-D17) | works — the map query embeds full `pian` geometry per doc (`pian_chranene_udaje.geom_gml`, `geom_wkt` — complete EPSG:4326 polygons; 19 docs in the probe area), and the deployed map-view chunk carries the komponenta `addShapeLayer` branch from `pian[0].pian_chranene_udaje.geom_wkt.value`; drawing maintainer-confirmed |
| Expanded-detail display of the Odkazy / persistent-link menu (komponenta-D14) | works — the persistent-link wrapper, the Odkazy menu (Persistentní odkaz, API, Zobrazit v AMČR), and the citation menu structure are present in the deployed komponenta template chunk; visual render maintainer-confirmed |
| Původ proklick (komponenta-D02) | holds — re-verified in the new compiled bundle (`komponenta_zdroj_ident_cely` routerLink binding present) |
| Entity search + facets | works — entity search responds with facets incl. the new `f_kraj_rada`; sorts respond as configured |
| PIAN chain rule (issue #127) | holds — handle response carries `pian_ident_cely` and the PIAN block with its own `pristupnost` |

### Known defects

None open. All eighteen recorded defects (komponenta-D01 through komponenta-D18) are resolved:

- komponenta-D14 — **fixed**: the expanded-detail display (Odkazy menu, persistent-link items) is structurally present in the deployed template chunk; the visual render was maintainer-confirmed in-session on this build.
- komponenta-D15 — **fixed**: the map proklik renders on komponenta results (loc_rpt served; template binding present in the deployed bundle) and the click-through was maintainer-confirmed.
- komponenta-D16 — **fixed**: clicking a komponenta in the map list resolves the record and zooms (id/handle endpoints serve komponenta ids; the by-loc komponenta branch is in the deployed map-view); click-through maintainer-confirmed.
- komponenta-D17 — **fixed**: full PIAN geometries are served in the komponenta map query and the geometry shape-layer branch is deployed; drawing maintainer-confirmed.
- komponenta-D18 — **fixed**: `f_kraj_rada` (Územní příslušnost) is indexed and returns exactly the two legitimate region codes with plausible counts; the client translates them to Čechy / Morava a Slezsko.
- komponenta-D01 through komponenta-D13 — **fixed as of the 2026-09-14 pass** (build `v4.0.3-215-g20163f8a`); not re-examined in this pass except komponenta-D02 (re-verified in the new bundle, see the matrix). See the 2026-09-14 log entry for per-defect evidence.

### Corrections made during this run

None — no earlier claim required correction.

## Verification log

| Date | Instance / build verified | What changed |
| --- | --- | --- |
| 2026-08-28 | `digiarchiv-test` (build with #127, milestone v4.1.0) | Initial verification; the point-in-time report was consolidated into this scenario when the corpus moved to this repository. |
| 2026-09-11 | `digiarchiv-test` (build `v4.0.3-211-g467f7386`, frontend bundle `main-TZ76JKMW.js`) | Regression pass on issue #127: komponenta-D02–D08 resolved (D07 data-limited), D09/D10 partial, D01 open and extended to the search and export surfaces, D11/D12 newly recorded, D13 minted after deeper ordering probes; browser-level checks maintainer-confirmed in the same session. |
| 2026-09-14 | `digiarchiv-test` (build `v4.0.3-215-g20163f8a`, frontend bundle `main-ZGSUZQS2.js`) | Regression pass on issue #127 responding to maintainer feedback: komponenta-D01 fixed on all surfaces (anonymous/B masked, C pr-legitimate, map clean), D09 fixed (config variant), D11/D12 fixed (export columns), D13 fixed (poradi on the dokument path), D07 resolved (geom_system data now exists), D10 fixed with a changed shape (two served locations); D02–D06 and D08 re-confirmed on the new build; the D01 example-record citation error corrected. |
| 2026-09-15 | `digiarchiv-test` (build `v4.0.3-221-g75788eaf`, frontend bundle `main-MSW4TEIM.js`) | Regression pass on issue #127 responding to five newly reported defects: komponenta-D14–D18 minted and verified fixed on the new build (Územní příslušnost facet values clean, map proklik, map-list click, PIAN geometries, expanded-detail display); browser-level aspects maintainer-confirmed in the same session; D01–D13 not re-examined except D02 (re-verified in the new compiled bundle). |
