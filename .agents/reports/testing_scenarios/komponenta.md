# Komponenta (cross-cutting record type) — testing scenario

**Key:** komponenta (entity)
**Scope:** the cross-cutting **Komponenty** record type — entity registration, search/facets/sorts, record pages per origin, permission behaviour, map, export, and conformance to the display specification (revision document with tracked changes and comments); issue [ARUP-CAS/aiscr-digiarchiv-2#127](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/127) drove it. Mechanisms this entity depends on — record-level gating, element-level protection, the export feature — are owned by their feature scenarios ([`permissions`](permissions.md), [`export`](export.md)); this scenario records only this entity's deviations from them.
**Principle:** concrete record ids are deliberately not embedded — record states drift. Use the discovery recipes below to find fresh candidates; a verification command may carry an identifier placeholder the recipe fills.

## Durable knowledge

> Amend this half where a run found it incomplete or wrong; never re-derive it.

### Environments and endpoints

- Record page: `https://digiarchiv-test.aiscr.cz/id/<ident_cely>`, data via `…/api/search/handle?id=<ident>` on `digiarchiv-test`.
- Entity search with facets: `…/api/search/query?entity=komponenta…`.
- Map mode: `mapa=true` (see the [`permissions`](permissions.md) and [`export`](export.md) scenarios for the surfaces' mechanics).
- Export: the four download formats of the [`export`](export.md) feature.

### Architecture and implementation facts

- The authoritative display specification is the revision document (`Digiarchiv_zaznamy_vypis_revize.docx`) — a Word file **with tracked changes and comments**. Reading it naively loses the deltas.
- **Spec comments (C0–C10)** flag items missing at review time; C6/C7/C8 (2023) give the xpath sources for SN/3D geometry.
- **Tracked insertion:** a leading space followed by `/ {extra_data/geom_system}` added to the *Primární EPSG* line — the 3D source variant (comment "Platí pro 3D (opomněl jsem)").
- **Extraction method (docx with tracked changes/comments):** unzip the docx, walk `word/document.xml` paragraph by paragraph — normal runs from `w:r/w:t`, insertions wrapped from `w:ins`, deletions from `w:del/w:delText`, comment anchors from `commentRangeStart/End` ids joined to `word/comments.xml` (id → author/date/text). A plain-text or pandoc-style conversion silently drops both.

### Feature or entity model

- **Origins (`komponenta_zdroj`):** `akce` (~131k), `dokument` (~81k), `samostatny_nalez` (~3.4k), `knihovna_3d` (~0.7k); parent ident in `komponenta_zdroj_ident_cely`. Total ~215k komponenty.
- **Registration:** in the client config `entities` list; icon `extension`; `exportFields.komponenta` defined (config defects komponenta-D08; no `sorts` entries, komponenta-D09).
- **Gating:** komponenta docs inherit `searchable` from the parent — the mechanism and its rules are owned by the [`permissions`](permissions.md) scenario.
- **Per-origin data paths on the record page (handle API):** AZ → `az_chranene_udaje`, parent `akce` fields, `pian` JSON (typ, přesnost, ZM10, geoms); SN → `samostatny_nalez_geom_system`, `samostatny_nalez_chranene_udaje` (geom/katastr/lokalizace), `samostatny_nalez` parent (duplicated 3×); 3D → `dokument_extra_data` (geom, odkaz), `komponenta_nalez_objekt/predmet` for Nálezy.
- **Display template (the spec's dedicated "Komponenty {komponenta}" section):** header line `ident_cely | období (přesná datace) jistota | areál`, `Původ` (parent with a quick link, i.e. proklik), `Přístupnost`, `Katastr (okres)` / `Další katastry` / `Aktivity` / `Poznámka`, a position block (**SN and 3D sources only** — not AZ), a PIAN block (AZ source), `Nálezy` (objekt/předmet lines + the SN druh line), and Související záznamy (parent idents). Sorting: poslední změna, katastr, ident_cely, okres+katastr, období (pořadí), areál. Access rule: inaccessible records show without `[chranene_udaje]`, are not searchable by them and not visible in the map. Indexation: only when the parent (AZ/dokument/SN) is archived and not deleted.

### Discovery recipes

1. **Per-origin samples:** `GET /api/search/query?entity=komponenta&rows=2&q=komponenta_zdroj:<akce|dokument|samostatny_nalez|knihovna_3d>`.
2. **Nálezy-bearing samples:** check the doc for `komponenta_nalez_objekt` / `komponenta_nalez_predmet` (complex fields; absent when the komponenta has none).
3. **Restricted (pr> A) samples for leak tests:** take any SN with pristupnost > A from the [`permissions`](permissions.md) discovery recipes, append the `-K001` suffix, or filter komponenty by a restricted parent's ident.
4. **Gating checks:** parent state via OAI (`GetRecord`), komponenta searchability via `/api/search/query?entity=komponenta&q=ident_cely:"…"`, landing via `/id/<ident>`.
5. **Record-page data:** `/api/search/handle?id=<komponenta>` — compare served fields against the display template above and against the parent's own page (the parent page is the protection ground truth).

## Current verification (2026-08-28, `digiarchiv-test` build with #127, milestone v4.1.0; browser-level checks maintainer-confirmed in the same session)

### Verified behaviour matrix

| Capability | Result |
| --- | --- |
| Entity search + facets (období, areál, aktivita, okres, katastr, typ nálezu…) | works |
| Parent gating (indexation rule) | works — restricted parent ⇒ not searchable, landing gated (mechanism: [`permissions`](permissions.md) scenario) |
| Protected-field search & map for pr> A komponenty | works — anonymous search doc has empty `katastr`, no `loc`, excluded from `mapa=true` |
| Map display | works — komponenty with coordinates in `mapa=true` mode |
| Export (all four formats) | works structurally, config defects (komponenta-D08) |
| PIAN block, Nálezy collapse, BibTeX, homepage tile, menu placement, origin colour-coding | working (browser-confirmed) |
| Primární EPSG for SN source | renders (spec comment C4 addressed for SN) |

### Known defects

- komponenta-D01 (**critical, element-level leak on the record page**): akce-origin — parent katastr shown to anonymous for pr> B parents (the parent's own page hides it); SN-origin — Poloha GML/WKT exact coordinates shown to anonymous for a pr=C record while katastr is correctly hidden (inconsistent). The handle API serves the full parent `chranene_udaje` (lokalizace, geom, katastr) to anonymous — the komponenta handle path lacks the secured-field filtering other entity searchers apply. This is komponenta's deviation from the element-level mechanism owned by the [`permissions`](permissions.md) scenario; search and map layers are protected, only the record detail leaks.
- komponenta-D02 (spec comment C3): Původ proklik broken — parent ident renders without a link (all origins).
- komponenta-D03 (spec comment C0): přesná datace — data exists in the index (and is exported) but is neither served by the handle API nor rendered.
- komponenta-D04 (spec comment C1): jistota — not indexed (facet empty); export column bound to a wrong field name (`lokalita_jistota`).
- komponenta-D05 (spec comment C10): SN druh line in Nálezy missing — the SN druh/specifikace/počet renders only in the parent card, not in the komponenta's Nálezy section.
- komponenta-D06: Katastr display malformed: akce-origin renders the value twice concatenated (two source fields, no separator/dedup — also for admin); knihovna_3d renders literal `(undefined)`.
- komponenta-D07: 3D records: Primární EPSG line absent — the tracked-changes amendment (`extra_data/geom_system`) either unimplemented or defeated by null data (sample had `geom_system` null while `geom_wkt@EPSG` carried the value) — re-test with a 3D record that has the field filled.
- komponenta-D08: Export config: období column bound to `samostatny_nalez_obdobi` (empty for akce/dokument origins — should be `komponenta_obdobi`); three duplicate `katastr` columns; the misnamed `lokalita_jistota` (see komponenta-D04).
- komponenta-D09: No sort options for komponenta in the config `sorts` array (the six spec sortings unavailable).
- komponenta-D10 (minor): SN parent field duplicated 3× in served data; stray space before the "Nálezy:" label.

### Corrections made during this run

- The position block (*Primární EPSG / Poloha*) is correctly **absent** for akce-origin komponenty — the template defines those lines only for SN and 3D sources. An earlier reading that expected PIAN-derived position lines for AZ was wrong.

## Verification log

| Date | Instance / build verified | What changed |
| --- | --- | --- |
| 2026-08-28 | `digiarchiv-test` (build with #127, milestone v4.1.0) | Initial verification; the point-in-time report was consolidated into this scenario when the corpus moved to this repository. |
