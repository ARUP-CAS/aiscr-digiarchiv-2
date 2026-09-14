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
- **Indexing (`Komponenta.java`):** SN-derived komponenty are **virtual index documents** — `fromSamostatnyNalez` deep-copies the SN document, sets `ident_cely` to `<SN ident>-K001`, records the parent in `komponenta_zdroj`/`komponenta_zdroj_ident_cely`, strips only the `soubor*` fields, and re-commits; the copy therefore carries the SN's protected fields (`samostatny_nalez_chranene_udaje`, katastr, exact geometry) into the komponenta index, where the search/handle path masks them per role instead of the index dropping them. AZ-origin komponenty receive `az_chranene_udaje` facet fields copied from the parent root, suffix-gated per the client config field lists. `filterOAI` returns `true` unconditionally. Where the parent chain carries a PIAN, `pristupnost` is re-read from the PIAN document (the PIAN chain rule from issue #127), so a komponenta inherits the PIAN's access level.
- **Frontend template (`komponenta.component.html`):** geometry rows render only for SN (`samostatny_nalez_chranene_udaje.geom_*`) and 3D (`dokument_extra_data.geom_*`) sources, each guarded on data presence; the PIAN block renders from the `pian` array; the AMCR quick-link ident is `ident_cely.substr(0, ident_cely.lastIndexOf("-"))` — the parent ident — and the persistent-link badge likewise uses the SN parent ident for SN-origin komponenty. The období header binds `komponenta_obdobi.id` with a client-side `heslar` lookup (`obdobi_druha`), so a null `value` in a served search doc has no display consequence.

### Feature or entity model

- **Origins (`komponenta_zdroj`):** `akce` (~129k), `dokument` (~81k), `samostatny_nalez` (~3.4k), `knihovna_3d` (~0.7k); parent ident in `komponenta_zdroj_ident_cely`. Around 172k carry objekt/předmet nálezy (`komponenta_typ_nalezu:*`).
- **Registration:** in the client config `entities` list; icon `extension`; `exportFields.komponenta` defined; `sorts` entries present — `komponenta_obdobi_poradi` and `komponenta_areal_poradi` (the heslo `poradi` order agreed for areál/období), `katastr_sort`, `datestamp`, and `okres_sort` in both directions. The poradi sort fields are populated on all indexing paths (akce, SN, dokument).
- **Gating:** komponenta docs inherit `searchable` from the parent — the mechanism and its rules are owned by the [`permissions`](permissions.md) scenario.
- **Per-origin data paths on the record page (handle API):** AZ → `az_chranene_udaje`, parent `akce` fields, `pian` JSON (typ, přesnost, ZM10, geoms); SN → `samostatny_nalez_geom_system`, `samostatny_nalez_chranene_udaje` (geom/katastr/lokalizace), top-level `presna_datace` alias (from `samostatny_nalez_presna_datace`), parent ident in two served field locations (the embedded `samostatny_nalez` block feeding the Původ row and `komponenta_zdroj_ident_cely`); 3D → `dokument_extra_data` (geom, odkaz, `geom_system`), `komponenta_nalez_objekt/predmet` for Nálezy.
- **Display template (the spec's dedicated "Komponenty {komponenta}" section):** header line `ident_cely | období (přesná datace) jistota | areál`, `Původ` (parent with a quick link, i.e. proklick), `Přístupnost`, `Katastr (okres)` / `Další katastry` / `Aktivity` / `Poznámka`, a position block (**SN and 3D sources only** — not AZ), a PIAN block (AZ source), `Nálezy` (objekt/předmet lines + the SN druh line), and Související záznamy (parent idents). Sorting: poslední změna, katastr, ident_cely, okres+katastr, období (pořadí), areál. Access rule: inaccessible records show without `[chranene_udaje]`, are not searchable by them and not visible in the map. Indexation: only when the parent (AZ/dokument/SN) is archived and not deleted.

### Discovery recipes

1. **Per-origin samples:** `GET /api/search/query?entity=komponenta&rows=2&q=komponenta_zdroj:<akce|dokument|samostatny_nalez|knihovna_3d>`.
2. **Nálezy-bearing samples:** `GET /api/search/query?entity=komponenta&rows=2&q=komponenta_typ_nalezu:*` — the field is set only when the komponenta has objekt/předmet nálezy.
3. **Restricted (pr> A) samples for leak tests:** take any SN with pristupnost > A from the [`permissions`](permissions.md) discovery recipes, append the `-K001` suffix, or filter komponenty by a restricted parent's ident. Verify the parent's current state before use — an unarchived parent (ident prefix `X-`) keeps its komponenty correctly out of anonymous search, and a nonexistent ident is indistinguishable from a gated one on numFound alone; check the parent record itself.
4. **Gating checks:** parent state via OAI (`GetRecord`), komponenta searchability via `/api/search/query?entity=komponenta&q=ident_cely:"…"`, landing via `/id/<ident>`.
5. **Record-page data:** `/api/search/handle?id=<komponenta>` — compare served fields against the display template above and against the parent's own page (the parent page is the protection ground truth).

## Current verification (2026-09-14, `digiarchiv-test` build `v4.0.3-215-g20163f8a`, frontend bundle `main-ZGSUZQS2.js`; B/C role sessions operator-supplied in session, browser-level checks with the maintainer in the same session)

### Verified behaviour matrix

| Capability | Result |
| --- | --- |
| Entity search + facets (období, areál, aktivita, okres, katastr, typ nálezu, jistota) | works — `komponenta_jistota` facet populated (206,529 true / 4,728 false) — komponenta-D04 holds |
| Přesná datace served and rendered (spec comment C0) | works — served on the handle API for both akce- and SN-origin records — komponenta-D03 holds |
| Jistota indexed, faceted, exported (spec comment C1) | works — komponenta-D04 holds |
| SN druh line in the Nálezy section (spec comment C10) | works — druh/specifikace/počet/poznámka served on the handle API — komponenta-D05 holds |
| Původ proklick (spec comment C3) | works — `routerLink "/id/"+komponenta_zdroj_ident_cely` re-verified in the new compiled bundle — komponenta-D02 holds |
| Katastr rendering (akce doubling, 3D `(undefined)`) | works — guarded per-origin rows re-verified in the new compiled bundle; served okres/katastr data clean — komponenta-D06 holds |
| Primární EPSG for SN source | renders — `samostatny_nalez_geom_system` served |
| Primární EPSG for 3D source (spec comment C4 amendment) | works — 3D-origin komponenty now carry `dokument_extra_data.geom_system` (EPSG:4326) served by the handle API, and the guarded template row is present in the new compiled bundle — komponenta-D07 resolved; the positive browser render is maintainer-assisted (see below) |
| Sorts (datestamp, katastr, ident_cely, okres+katastr) | works server-side in both directions; the `okres_sort desc, katastr_sort desc` config variant now lists komponenta — komponenta-D09 fixed (matches the maintainer's UI screenshot) |
| Sorts (období, areál by heslo `poradi`) | works corpus-wide — the poradi fields are populated on the dokument path too (0 of 80,578 dokument-origin docs missing either field), and first pages order correctly in both directions (asc → paleolit; desc → datace neurčena) — komponenta-D13 fixed |
| Export field bindings | works — `komponenta_presna_datace` populated for SN-origin rows (komponenta-D11 fixed); `show_in_AMCR` uses the parent ident for SN-origin and the komponenta ident for other origins (komponenta-D12 fixed); období/katastr-dedup/jistota bindings correct (komponenta-D08 holds) |
| Parent gating (indexation rule) | works — unarchived parents keep komponenty out of anonymous search (numFound 0); no komponenta docs with `stav` 0–2 or `searchable:false` anonymously reachable |
| Protected-field handling for anonymous and B | works on the handle API, entity search, and CSV export — no `chranene_udaje` block, no katastr, no geometry served below the record's pristupnost; role-suffixed aliases in effect (`katastr:f_katastr_<ROLE>`, `loc_rpt_<ROLE>`); C sees pr-legitimate content; the map layer stays clean (restricted komponenta filtered from `mapa=true`) — komponenta-D01 fixed |
| Map display | works — komponenty with coordinates served in `mapa=true` with `loc` and the PIAN block; restricted ones excluded |
| PIAN chain rule (issue #127) | works — `pian_ident_cely` carried on the komponenta; access level inherited from the PIAN |
| PIAN block, Nálezy collapse, BibTeX, homepage tile, menu placement, origin colour-coding | browser-level — maintainer-assisted this run (the maintainer is performing them; outcomes below reflect what was reported back) |
| SN-origin search docs with `komponenta_obdobi` id-but-null value | no display consequence — the header binds `komponenta_obdobi.id` with a client-side heslar lookup, re-verified in the new compiled bundle |

### Known defects

None open. All thirteen recorded defects (komponenta-D01 through komponenta-D13) are resolved on this build:

- komponenta-D01 — **fixed**: the handle API, entity search, and CSV export no longer serve the restricted parents' protected data below the record's pristupnost — anonymous and badatel (B) are masked on all three surfaces (no chranene block, no katastr, no geometry), archeolog (C) sees pr-legitimate content, and the map layer stays clean.
- komponenta-D02 — **fixed (holds)**: Původ proklick re-verified in the new compiled bundle.
- komponenta-D03 — **fixed (holds)**: přesná datace served for both origins.
- komponenta-D04 — **fixed (holds)**: jistota facet counts unchanged.
- komponenta-D05 — **fixed (holds)**: the SN druh line fields are served.
- komponenta-D06 — **fixed (holds)**: guarded katastr rows in the new bundle; served data clean.
- komponenta-D07 — **resolved**: 3D-origin data now carries `geom_system` (previously no 3D record had it), and the guarded template row is present; the positive browser render is a maintainer-assisted check (browser required).
- komponenta-D08 — **fixed (holds)**: export bindings verified in the served CSV.
- komponenta-D09 — **fixed**: the `okres_sort desc` config variant lists komponenta, so the descending okres+katastr sort is selectable in the UI (the maintainer's screenshot showed the same).
- komponenta-D10 — **fixed (changed shape)**: the SN parent ident now occurs in two served field locations — the embedded `samostatny_nalez` block that feeds the Původ row and `komponenta_zdroj_ident_cely` — down from three; each remaining location serves a distinct rendered purpose.
- komponenta-D11 — **fixed**: the export `komponenta_presna_datace` column is populated for SN-origin rows.
- komponenta-D12 — **fixed**: the export `show_in_AMCR` column emits the parent ident for SN-origin komponenty; other origins keep the komponenta ident by design.
- komponenta-D13 — **fixed**: the poradi fields are populated on the dokument indexing path, and corpus-wide období/areál ordering is correct in both directions.

### Corrections made during this run

- The 2026-09-11 issue comment cited `M-91030078A-K003` as the komponenta-D01 example record; that ident does not exist — the record family is `X-M-91030078A` (unarchived parent, so its komponenty are correctly not searchable: anonymous numFound 0). The maintainer's failed re-verification ("such an ident does not exist") is explained by that citation error, not by the defect being unverifiable; fresh recipe candidates confirm the leak itself is fixed on this build. Recipe 3 now carries the parent-state verification step that would have caught the citation.
- The 2026-09-11 same-day disagreement between this scenario (D01 open) and the permissions scenario (permissions-D11 fixed on build `v4.0.3-211-g467f7386`) is resolved in favour of the permissions record: the fix holds on build `v4.0.3-215-g20163f8a`.
- The anonymous export shape changed since the 2026-09-11 permissions record: the CSV row for a restricted komponenta is now served with the protected columns masked (katastr empty) rather than the record being filtered out of the export entirely; both shapes leak nothing. The masking ladder was re-verified per role in this run.

## Verification log

| Date | Instance / build verified | What changed |
| --- | --- | --- |
| 2026-08-28 | `digiarchiv-test` (build with #127, milestone v4.1.0) | Initial verification; the point-in-time report was consolidated into this scenario when the corpus moved to this repository. |
| 2026-09-11 | `digiarchiv-test` (build `v4.0.3-211-g467f7386`, frontend bundle `main-TZ76JKMW.js`) | Regression pass on issue #127: komponenta-D02–D08 resolved (D07 data-limited), D09/D10 partial, D01 open and extended to the search and export surfaces, D11/D12 newly recorded, D13 minted after deeper ordering probes; browser-level checks maintainer-confirmed in the same session. |
| 2026-09-14 | `digiarchiv-test` (build `v4.0.3-215-g20163f8a`, frontend bundle `main-ZGSUZQS2.js`) | Regression pass on issue #127 responding to maintainer feedback: komponenta-D01 fixed on all surfaces (anonymous/B masked, C pr-legitimate, map clean), D09 fixed (config variant), D11/D12 fixed (export columns), D13 fixed (poradi on the dokument path), D07 resolved (geom_system data now exists), D10 fixed with a changed shape (two served locations); D02–D06 and D08 re-confirmed on the new build; the D01 example-record citation error corrected. |
