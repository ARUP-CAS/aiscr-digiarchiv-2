# Komponenta (cross-cutting record type) — testing scenario

**Key:** komponenta (entity)
**Scope:** the cross-cutting **Komponenty** record type — entity registration, search/facets/sorts, record pages per origin, permission behaviour, map, export, and conformance to the display specification (revision document with tracked changes and comments); issue [ARUP-CAS/aiscr-digiarchiv-2#127](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/127) drove it. Mechanisms this entity depends on — record-level gating, element-level protection, the export feature — are owned by their feature scenarios ([`permissions`](permissions.md), [`export`](export.md)); this scenario records only this entity's deviations from them.
**Principle:** concrete record ids are deliberately not embedded — record states drift. Use the discovery recipes below to find fresh candidates; a verification command may carry an identifier placeholder the recipe fills.

## Durable knowledge

> Amend this half where a run found it incomplete or wrong; never re-derive it.

### Environments and endpoints

- Record page: `https://digiarchiv-test.aiscr.cz/id/<ident_cely>`, data via `…/api/search/handle?id=<ident>` on `digiarchiv-test`; restricted records 404 the landing page for anonymous visitors (record-level gate, [`permissions`](permissions.md)).
- Entity search with facets: `…/api/search/query?entity=komponenta&q=…`.
- Map mode: `mapa=true` (see the [`permissions`](permissions.md) and [`export`](export.md) scenarios for the surfaces' mechanics); komponenty with coordinates are served with `loc`, `lat`/`lng` stats, and the `pian` block.
- Export: the four download formats of the [`export`](export.md) feature.
- Deployed frontend templates are verifiable without a browser: the served page shell names the module-preloaded chunks, the `/id/:id` route lazy-loads the document chunk, and the entity components (komponenta included) are compiled into one of the preloaded chunks. Fetch that chunk and search the compiled template with a UTF-8-aware search — Windows code-page tools mangle the diacritics in the i18n keys, so a code-page search silently finds nothing.

### Architecture and implementation facts

- The authoritative display specification is the revision document (`Digiarchiv_zaznamy_vypis_revize.docx`) — a Word file **with tracked changes and comments**. Reading it naively loses the deltas.
- **Spec comments (C0–C10)** flag items missing at review time; C6/C7/C8 (2023) give the xpath sources for SN/3D geometry.
- **Tracked insertion:** a leading space followed by `/ {extra_data/geom_system}` added to the *Primární EPSG* line — the 3D source variant (comment "Platí pro 3D (opomněl jsem)").
- **Extraction method (docx with tracked changes/comments):** unzip the docx, walk `word/document.xml` paragraph by paragraph — normal runs from `w:r/w:t`, insertions wrapped from `w:ins`, deletions from `w:del/w:delText`, comment anchors from `commentRangeStart/End` ids joined to `word/comments.xml` (id → author/date/text). A plain-text or pandoc-style conversion silently drops both.
- **Indexing (`Komponenta.java`):** SN-derived komponenty are **virtual index documents** — `fromSamostatnyNalez` deep-copies the SN document, sets `ident_cely` to `<SN ident>-K001`, records the parent in `komponenta_zdroj`/`komponenta_zdroj_ident_cely`, strips only the `soubor*` fields, and re-commits; the copy therefore carries the SN's protected fields (`samostatny_nalez_chranene_udaje`, katastr, exact geometry) into the komponenta index. AZ-origin komponenty receive `az_chranene_udaje` facet fields copied from the parent root, suffix-gated per the client config field lists. `filterOAI` returns `true` unconditionally. Where the parent chain carries a PIAN, `pristupnost` is re-read from the PIAN document (the PIAN chain rule from issue #127), so a komponenta inherits the PIAN's access level.
- **Frontend template (`komponenta.component.html`):** geometry rows render only for SN (`samostatny_nalez_chranene_udaje.geom_*`) and 3D (`dokument_extra_data.geom_*`) sources, each guarded on data presence; the PIAN block renders from the `pian` array; the AMCR quick-link ident is `ident_cely.substr(0, ident_cely.lastIndexOf("-"))` — the parent ident — and the persistent-link badge likewise uses the SN parent ident for SN-origin komponenty.

### Feature or entity model

- **Origins (`komponenta_zdroj`):** `akce` (~129k), `dokument` (~81k), `samostatny_nalez` (~3.4k), `knihovna_3d` (~0.7k); parent ident in `komponenta_zdroj_ident_cely`. Around 172k carry objekt/předmet nálezy (`komponenta_typ_nalezu:*`).
- **Registration:** in the client config `entities` list; icon `extension`; `exportFields.komponenta` defined; `sorts` entries present — `komponenta_obdobi_poradi` and `komponenta_areal_poradi` (the heslo `poradi` order agreed for areál/období), `katastr_sort`, `datestamp`, and `okres_sort` whose asc variant includes komponenta while the desc variant omits it (komponenta-D09). The poradi sort fields are populated on the akce and SN indexing paths; the dokument path leaves them unset (komponenta-D13).
- **Gating:** komponenta docs inherit `searchable` from the parent — the mechanism and its rules are owned by the [`permissions`](permissions.md) scenario.
- **Per-origin data paths on the record page (handle API):** AZ → `az_chranene_udaje`, parent `akce` fields, `pian` JSON (typ, přesnost, ZM10, geoms); SN → `samostatny_nalez_geom_system`, `samostatny_nalez_chranene_udaje` (geom/katastr/lokalizace), top-level `presna_datace` alias (from `samostatny_nalez_presna_datace`), parent ident in three served field locations (komponenta-D10); 3D → `dokument_extra_data` (geom, odkaz), `komponenta_nalez_objekt/predmet` for Nálezy.
- **Display template (the spec's dedicated "Komponenty {komponenta}" section):** header line `ident_cely | období (přesná datace) jistota | areál`, `Původ` (parent with a quick link, i.e. proklick), `Přístupnost`, `Katastr (okres)` / `Další katastry` / `Aktivity` / `Poznámka`, a position block (**SN and 3D sources only** — not AZ), a PIAN block (AZ source), `Nálezy` (objekt/předmet lines + the SN druh line), and Související záznamy (parent idents). Sorting: poslední změna, katastr, ident_cely, okres+katastr, období (pořadí), areál. Access rule: inaccessible records show without `[chranene_udaje]`, are not searchable by them and not visible in the map. Indexation: only when the parent (AZ/dokument/SN) is archived and not deleted.

### Discovery recipes

1. **Per-origin samples:** `GET /api/search/query?entity=komponenta&rows=2&q=komponenta_zdroj:<akce|dokument|samostatny_nalez|knihovna_3d>`.
2. **Nálezy-bearing samples:** `GET /api/search/query?entity=komponenta&rows=2&q=komponenta_typ_nalezu:*` — the field is set only when the komponenta has objekt/předmet nálezy.
3. **Restricted (pr> A) samples for leak tests:** take any SN with pristupnost > A from the [`permissions`](permissions.md) discovery recipes, append the `-K001` suffix, or filter komponenty by a restricted parent's ident.
4. **Gating checks:** parent state via OAI (`GetRecord`), komponenta searchability via `/api/search/query?entity=komponenta&q=ident_cely:"…"`, landing via `/id/<ident>`.
5. **Record-page data:** `/api/search/handle?id=<komponenta>` — compare served fields against the display template above and against the parent's own page (the parent page is the protection ground truth).

## Current verification (2026-09-11, `digiarchiv-test` build `v4.0.3-211-g467f7386`, frontend bundle `main-TZ76JKMW.js`; browser-level checks maintainer-confirmed in the same session)

### Verified behaviour matrix

| Capability | Result |
| --- | --- |
| Entity search + facets (období, areál, aktivita, okres, katastr, typ nálezu, jistota) | works — `komponenta_jistota` facet populated (206,529 true / 4,728 false) — komponenta-D04 resolved |
| Přesná datace served and rendered (spec comment C0) | works — served on the handle API and rendered in the header line — komponenta-D03 resolved |
| Jistota indexed, faceted, exported (spec comment C1) | works — komponenta-D04 resolved |
| SN druh line in the Nálezy section (spec comment C10) | works — druh/specifikace/počet/poznámka served and rendered — komponenta-D05 resolved |
| Původ proklick (spec comment C3) | works — parent ident renders as a link to `/id/<komponenta_zdroj_ident_cely>` (verified in the deployed compiled template) — komponenta-D02 resolved |
| Katastr rendering (akce doubling, 3D `(undefined)`) | works — branches guarded per origin, whole line gated on `okres` (deployed compiled template) — komponenta-D06 resolved |
| Primární EPSG for SN source | renders |
| Primární EPSG for 3D source (spec comment C4 amendment) | implemented — guarded template rows exist in source and the deployed compiled template, but no 3D record with `geom_system` filled exists in current data — komponenta-D07 resolved as data-limited, positive render unverified |
| Sorts (datestamp, katastr, ident_cely, okres+katastr) | works server-side in both directions, including `okres_sort desc, katastr_sort desc` typed directly; the `okres_sort desc` config variant omits komponenta (komponenta-D09 residual) |
| Sorts (období, areál by heslo `poradi`) | **fails corpus-wide** (komponenta-D13) — correct for akce-origin (both fields, both directions) and SN-origin (období), but the poradi fields are unpopulated on the dokument path, so the unordered ~80.6k dokument-origin block sorts ahead of (asc) or behind (desc) every correctly ordered doc |
| Export field bindings (období, katastr dedup, jistota) | works — komponenta-D08 resolved; new defects komponenta-D11 and komponenta-D12 |
| Parent gating (indexation rule) | works — restricted parent ⇒ komponenta landing 404 for anonymous; the nearchivované sweep is clean (no unarchived records reachable) |
| Protected-field handling for anonymous | **fails** on the handle API, entity search, and CSV export (komponenta-D01, extended); the map layer stays clean — restricted komponenty remain excluded from `mapa=true` |
| Map display | works — komponenty with coordinates served in `mapa=true` with `loc` and the PIAN block |
| PIAN chain rule (issue #127) | works — `pian_ident_cely` carried on the komponenta per the issue's spec; access level inherited from the PIAN |
| PIAN block, Nálezy collapse, BibTeX, homepage tile, menu placement, origin colour-coding | working (browser-confirmed this run; the Nálezy panel confirmed on a nálezy-bearing record) |

### Known defects

- komponenta-D01 (**critical, element-level leak, extended to search and export**): still present. Anonymous handle, entity search, and CSV export all serve protected data of restricted parents: the handle API serves the full `samostatny_nalez_chranene_udaje`/`az_chranene_udaje` (lokalizace, exact GML/WKT geometry, katastr) for pr=C parents; the anonymous search doc carries the protected katastr and geometry; the anonymous CSV export serves katastr. The credentialed badatel level (B) is masked on handle, search, and export; the archeolog level (C) legitimately sees. Source mechanism: `fromSamostatnyNalez` deep-copies the SN document without stripping `samostatny_nalez_chranene_udaje`, the komponenta handle path lacks the secured-field filtering other entity searchers apply, and `filterOAI` is a no-op. Also recorded as permissions-D11 in the [`permissions`](permissions.md) scenario.
- komponenta-D09 (residual): the `okres_sort desc` config variant omits komponenta, so the descending okres+katastr sorting is not selectable for the entity in the UI; the sorting itself works when typed directly (`okres_sort desc, katastr_sort desc`).
- komponenta-D10 (partial): the SN parent ident still occurs in three served field locations (two embedded blobs plus `komponenta_zdroj_ident_cely`); the stray space before the "Nálezy:" label is resolved — panel titles use the standard single-space pattern, browser-confirmed.
- komponenta-D11: export `komponenta_presna_datace` column empty for SN-origin rows — the virtual document carries the value under the top-level `presna_datace` alias (from `samostatny_nalez_presna_datace`), while the export column binds `komponenta_presna_datace`, which the SN derivation path never sets; the record page shows the value.
- komponenta-D12: export `show_in_AMCR` column emits the virtual `-K001` ident for SN-origin komponenty — the UI applies the parent-ident exception (`ident_cely_api` strips the suffix), the export does not.
- komponenta-D13: období/areál poradi sorting is broken for dokument-origin komponenty — `komponenta_obdobi_poradi`/`komponenta_areal_poradi` are not populated on the dokument indexing path, so the corpus-wide sort places the entire unordered dokument-origin block (~80.6k docs, ~38% of the corpus) ahead of (asc) or behind (desc) the correctly ordered akce- and SN-origin docs. Verified in the anonymous search API: akce-origin sorts correctly in both directions and both fields (asc → paleolit / sídliště; desc → datace neurčena), SN-origin období sorts correctly (desc → datace neurčena first), while the unfiltered and dokument-filtered queries return chronologically mixed first pages. SN-derived komponenty additionally serve `komponenta_obdobi` with an id but a null value in search docs; the display consequence was not examined.

### Corrections made during this run

- The matrix first recorded období/areál sorting as working from the config's sort entries and well-formed responses; order inspection after operator feedback ("strange mixed results") showed the mixing and revised the row — komponenta-D13 minted.
- The 2026-08-28 row "anonymous search doc has empty `katastr`" no longer holds: the anonymous search doc now carries the protected katastr (and geometry). The search layer leaks too — folded into komponenta-D01.
- The 2026-08-28 note that "search and map layers are protected, only the record detail leaks" is corrected: the export layer also leaks (katastr in the anonymous CSV); the map layer stays clean.

## Verification log

| Date | Instance / build verified | What changed |
| --- | --- | --- |
| 2026-08-28 | `digiarchiv-test` (build with #127, milestone v4.1.0) | Initial verification; the point-in-time report was consolidated into this scenario when the corpus moved to this repository. |
| 2026-09-11 | `digiarchiv-test` (build `v4.0.3-211-g467f7386`, frontend bundle `main-TZ76JKMW.js`) | Regression pass on issue #127: komponenta-D02–D08 resolved (D07 data-limited), D09/D10 partial, D01 open and extended to the search and export surfaces, D11/D12 newly recorded, D13 minted after deeper ordering probes; browser-level checks maintainer-confirmed in the same session. |
