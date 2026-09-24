# Record printing (print view) — testing scenario

**Key:** print (feature)
**Scope:** the record print view at `/print/:id` and the print action on record cards — the timing that gates `window.print()` on loaded content, the print stylesheet, and the Chrome pagination behaviour they produce. Driven by ARUP-CAS/aiscr-digiarchiv-2#953.
**Principle:** concrete record ids are deliberately not embedded — record states drift. Use the discovery recipes below to find fresh candidates; a verification command may carry an identifier placeholder the recipe fills.

## Durable knowledge

> Amend this half where a run found it incomplete or wrong; never re-derive it.

### Environments and endpoints

- Test instance: `https://digiarchiv-test.aiscr.cz/` — the served HTML is the CSR shell (`<app-root></app-root>` empty), so the print view's content is **client-rendered**: anonymous probes see the shell, the referenced bundles, and the API, never the rendered print output. Visual print behaviour needs a browser (maintainer-assisted).
- Production: `https://digiarchiv.aiscr.cz/` — the #953 fix is unreleased dev work (the test build describes from `v4.0.3`), so the production comparison is not meaningful until release; the originally reported broken records live only on production.
- Print URL shape: `https://digiarchiv-test.aiscr.cz/print/<IDENT>?lang=cs`; the record page `https://digiarchiv-test.aiscr.cz/id/<IDENT>?lang=cs` carries the print button on its result card.
- Bundle names are content-hashed (`main-<hash>.js`, `styles-<hash>.css`) and change per build: resolve the current names from the served page's `<script>`/`<link>` tags each run.
- Test deployments are routinely built from a local working tree (`dirty:true` in the embedded build identity is expected practice, not an anomaly — see the version-footer scenario).

### Architecture and implementation facts

- Routing: both `/id/:id` and `/print/:id` resolve to `DocumentComponent` (`web/src/main/ng/src/app/app.routes.ts`). The print variant is detected with the standalone `isActive('print', this.router, …)` in `ngOnInit`/`ngAfterViewInit`, which sets `state.printing`.
- Print data path: `DocumentComponent.search()` loads the record through `GET /api/search/handle?id=<IDENT>`; in print mode `state.loading` is held true (`state.loading.set(state.printing())`) until related content completes, so the loading bar stays visible instead of the page flashing half-rendered.
- Print gating: `tryPrint()` polls every 1 s until `state.loading()` and `state.imagesLoading` clear, then waits another 1 s and calls `AppService.print()` → `window.print()`. `RelatedComponent` sets `state.loading` true early in print mode and fetches related records via `getRecords(true)`; `Entity.getFullId()` clears loading after the detail fetch. This gating is the core of the #953 fix: the original defect was `window.print()` firing before related records and images finished, producing empty or partial print output in Chrome.
- Detail expansion: `_detailExpanded` is an Angular signal (commit `68493cdc`); document children (dokumentacni jednotka, komponenta, dokument, pian) render expanded in the print view through `inDocument`/`isChild`.
- Print stylesheet (`web/src/main/ng/src/scss/_app-print.scss`, compiled into the styles bundle): `@media print` sets `@page { margin: 0 }`, `body, main { height: auto !important; zoom: 95% }`, hides `header`, `footer`, and card actions, drops entity-card left borders and card padding, and unsets the related-items panels' max-height/overflow. The `zoom` plus `@page` margin is the Chrome pagination half of the fix (final values after `544d7851`; an intermediate `zoom: 85%` shipped in `fd7c9c12`).
- Post-print state: `AppService.print()` resets `state.printing` and `state.loading` to false immediately after `window.print()` (commit `96deec70` un-commented the resets); `AppState` also clears `printing` in `resetState(true)`.
- SSR guard: `ngAfterViewInit` checks `isBrowser` before triggering print (`c04b7128`), so the server render does not attempt `window.print()`.
- Fix history under the #953 label: `c04b7128` (2026-06-08, isBrowser guard), `2bc56b54` (2026-09-16, related-records loading hold), `199acd37` (2026-09-23, loading hold in entity/getFullId, standalone `isActive`; the same commit also carries permission-model changes for projekt owned by the permissions scenario), `68493cdc` (2026-09-23, `_detailExpanded` signal, print delay), `fd7c9c12` (2026-09-24, `zoom: 85%`, state resets commented out), `544d7851` (2026-09-24, `@page { margin: 0 }`, `zoom: 95%` — the final state the maintainer calls the durable solution), `96deec70` (2026-09-24, print-D01 fix — the `printing`/`loading` signal resets restored).

### Feature or entity model

- Expected print output: the info card with the persistent link and the "Citujte jako" citation, the record card with detail fully expanded (children included), no application chrome (navbar, footer, card actions), and complete pagination — no blank content pages.
- Record variance in the original defect: records with more related content (more children to fetch) hit the timing window where `window.print()` fired early; short records loaded fast enough to print complete. Firefox tolerated the old timing, which made the defect look Chrome-only.
- The print button lives on the result card (`app-result-actions`) and navigates to `/print/:id`.

### Discovery recipes

1. Find candidate records: `GET /api/search/query?entity=akce&rows=5`, keep the docs whose `entity` is `akce`, and take their `ident_cely`. The same query shape works for other entities (`dokument`, `lokalita`, `projekt`, `samostatny_nalez`, `knihovna_3d`).
2. Verify the print data path: `GET /api/search/handle?id=<IDENT>` returns the record with its children (dokumentacni jednotka, pian, …).
3. Verify the print route: `GET /print/<IDENT>?lang=cs` returns the CSR shell (HTTP 200) referencing the current bundles.
4. Verify the deployed fix: from the served page take `styles-<hash>.css` and `main-<hash>.js`; search the styles bundle for `@media print{@page{margin:0}` and `zoom:95%`; search the main bundle for `raw:"` to read the embedded build identity and tie the deployment to a base commit.
5. Visual print behaviour (Chrome and Firefox): open `/print/<IDENT>?lang=cs` in the browser, let the print dialog auto-trigger (or press Ctrl+P), and print to PDF — this needs a browser and is maintainer-assisted for an anonymous run.

### Verification commands

Identifier-free, re-runnable against the test instance (rate-limit gently; no credentials needed — the print view is anonymous):

```text
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/search/query?entity=akce&rows=5"
# take a doc with entity=akce, then:
curl.exe -s "https://digiarchiv-test.aiscr.cz/api/search/handle?id=<IDENT>"
curl.exe -s -o <scratch>/print.html "https://digiarchiv-test.aiscr.cz/print/<IDENT>?lang=cs"
# from print.html take styles-<hash>.css and main-<hash>.js, then:
curl.exe -s -o <scratch>/styles.css "https://digiarchiv-test.aiscr.cz/styles-<hash>.css"
curl.exe -s -o <scratch>/main.js "https://digiarchiv-test.aiscr.cz/main-<hash>.js"
# styles.css must contain: @media print{@page{margin:0}  ...  zoom:95%
# main.js must contain: raw:"v…-g<hash>-dirty"  (embedded build identity)
```

## Current verification (2026-09-24, digiarchiv-test.aiscr.cz, regression pass, main-Q5FOPDUF.js + unchanged styles-KBEEYJB3.css, embedded build v4.0.3-237-g96deec70, anonymous session)

> Regression pass after the print-D01 fix (`96deec70`); the prior verification of the same day (styles-KBEEYJB3.css, main-SZUGF3W7.js, build v4.0.3-235-g544d7851-dirty, operator-performed Chrome browser check) is superseded and recorded in the verification log.

### Verified behaviour matrix

| Capability | Result |
| --- | --- |
| The print-D01 fix is deployed | verified — main-Q5FOPDUF.js embeds build identity `v4.0.3-237-g96deec70` (base commit `96deec70`, clean build), and the deployed compiled code resets the signals after printing: `window.print(),this.state.printing.set(!1),this.state.loading.set(!1)` in chunk-XUIHYJEX.js |
| The print button still enters the print flow only on demand | verified in compiled code — chunk-N23ABHEU.js result-actions `print()` sets `printing` true only on the explicit print action before navigating to `/id/:id` |
| Print stylesheet still carries the #953 print rules | verified — the styles bundle hash is unchanged (styles-KBEEYJB3.css, the same content verified earlier this day: `@media print{@page{margin:0}…zoom:95%}`) |
| Print data path returns the record with children | verified — `GET /api/search/handle?id=C-200810832A` returns HTTP 200 with the akce and its children; `GET /print/C-200810832A?lang=cs` returns HTTP 200 (2026-09-24, new build) |
| Print output complete and correctly paginated in Chrome | not examined this run — requires a browser; recorded as maintainer-assisted below |
| Print dialog no longer auto-opens when navigating from `/print/:id` to another record page (print-D01 behavioural confirmation) | not examined this run — requires a browser; recorded as maintainer-assisted below |
| Firefox print on the fixed build | not examined — no browser check performed this run |
| Production print state | not examined — unreleased milestone work; the production comparison becomes meaningful once the fix is released |

### Maintainer-assisted checks

- Chrome browser check: print `https://digiarchiv-test.aiscr.cz/print/<IDENT>?lang=cs` to PDF on the new build and confirm the output is still complete and correctly paginated (recipe 1 fills `<IDENT>`).
- Chrome browser check: from a print page, navigate in the same tab to another record page and confirm the print dialog does not auto-open — the behavioural confirmation of print-D01's fix.

### Known defects

- print-D01 (**fixed**): **The `printing` signal was never reset after a print, so a later in-app navigation could auto-trigger the print dialog again.** Fixed in commit `96deec70` (2026-09-24): `AppService.print()` resets `printing` and `loading` immediately after `window.print()`. Verified deployed — the compiled code in chunk-XUIHYJEX.js carries the resets and the embedded build identity names `96deec70` as the base; the browser-level confirmation remains a maintainer-assisted check this run. Originally recorded 2026-09-24 (resets commented out in `fd7c9c12`, cleared only by `resetState(true)`).

### Corrections made during this run

- None — the regression pass found no claim of the prior verification to correct.

## Verification log

| Date | Instance / build verified | What changed |
| --- | --- | --- |
| 2026-09-24 | digiarchiv-test.aiscr.cz (styles-KBEEYJB3.css, main-SZUGF3W7.js; embedded build v4.0.3-235-g544d7851-dirty) | First verification of #953: the complete fix chain verified as deployed on the test instance (print rules in the styles bundle, build base at the final #953 commit); Chrome print confirmed complete by an operator-performed browser check on two records; one defect recorded (print-D01 stale printing signal) and one candidate finding withdrawn on maintainer review (permission changes in `199acd37` — not a finding). Firefox and production not examined. |
| 2026-09-24 | digiarchiv-test.aiscr.cz (styles-KBEEYJB3.css unchanged, main-Q5FOPDUF.js; embedded build v4.0.3-237-g96deec70) | Regression pass after the print-D01 fix `96deec70`: new build deployed with the signal resets restored in the compiled code; print stylesheet unchanged; data path re-verified; print-D01 walked to fixed (code- and bundle-demonstrated; browser confirmation marked maintainer-assisted — no browser check this run). |
