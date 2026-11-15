# History location-data protection — testing scenario

**Key:** history-location-protection (feature)

**Scope:** protection of location names recorded in a record's history (`historie.poznamka` where `historie.typ_zmeny = "KAT"`) on the internal Digiarchiv endpoints and OAI-PMH, for the record types that carry cadastres in protected data: `samostatny_nalez`, `projekt`, and `archeologicky_zaznam` (`akce`, `lokalita`). The driving issue is [ARUP-CAS/aiscr-digiarchiv-2#1163](https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues/1163).

**Principle:** concrete record ids are deliberately not embedded — record states drift. Use the discovery recipes below to find fresh candidates; a verification command may carry an identifier placeholder the recipe fills.

## Durable knowledge

> Amend this half where a run found it incomplete or wrong; never re-derive it.

### Environments and endpoints

| Environment | Web | OAI-PMH | Build |
| --- | --- | --- | --- |
| Test | `https://digiarchiv-test.aiscr.cz` | `https://api-test.aiscr.cz/{2.0,2.1,2.2}/oai` | `main-MBFN4CJK.js`, embedded `v4.1.0-27-gffac9d9a` (commit `ffac9d9a`; includes the #1163 commits `a0633555`, `e9510854`, `1aa0a02f`). Verified 2026-11-15. |
| Production | `https://digiarchiv.aiscr.cz` | `https://api.aiscr.cz/{2.0,2.1,2.2}/oai` | Not probed in this run. The [`permissions`](permissions.md) scenario's last recorded production build is `v4.0.3-237-g96deec70-dirty` (2026-10-23); #1163 is on `dev` and its production deployment is not inferred. |

OAI identifiers use the environment's own domain prefix (`https://api-test.aiscr.cz/id/…`, `https://api.aiscr.cz/id/…`). Versioned endpoints serve the same filtered core and differ by XSLT; the KAT masking is applied before the version transform, so it holds on 2.0, 2.1 and 2.2. `ListRecords` pages carry 100 records and a `resumptionToken`.

Surfaces exercised:

- OAI-PMH `GetRecord` and `ListRecords` on 2.0/2.1/2.2.
- Internal `GET /api/search/handle?id=<ident>`, `GET /api/search/query?entity=<entity>&q=ident_cely:"<ident>"`, and `GET /api/search/id?id=<ident>`.
- The landing page is a shell; its record data comes from the handle API (see [`persistent-landing-pages`](persistent-landing-pages.md)).

### Architecture and implementation facts

- **OAI side.** `OAIRequest.filter` calls `transformHistorieKat` only when the raw record XML contains `<amcr:typ_zmeny>KAT</amcr:typ_zmeny>` and the record's `pristupnost` is greater than the caller's (`docPristupnost.compareToIgnoreCase(userPristupnost) > 0`). `historie_kat.xslt` is an identity transform with one rule per namespace (`amcr`, `amcr20`, `amcr21`, `amcr22`): it copies `historie[typ_zmeny='KAT']/poznamka` and replaces its text with `HTTP/1.1 403 Forbidden`. The transform runs before the version and `oai_dc` transforms, so all three versioned endpoints are covered.
- **Internal side.** `AkceSearcher.filterOne`, `LokalitaSearcher.filterOne`, `ProjektSearcher.filterOne` and `SamostatnyNalezSearcher.filter` call `doc.remove("historie")` after their role filter. `SamostatnyNalezSearcher.getSearchFields` projects `historie:[json]` because `isAllowed` reads the `SN01` author from it; the removal happens after `isAllowed`, so the access decision is unaffected. The other three searchers do not project `historie` in their search fields — the entity `fields` configuration has no `historie` entry — so their removal is defensive.
- **Discovery limits.** The `entities` core's `historie` field is `indexed="false"`, so it cannot be queried directly. `historie_typ_zmeny` is indexed, but a `historie_typ_zmeny:KAT` search-API query was treated as a text match in this run and returned the whole entity population; do not use it as a KAT discovery filter. Use OAI `ListRecords` scanning instead.
- **File history.** `soubor[].historie` is file-level history; its `typ_zmeny` values are file events (observed `SBR0`) and its `poznamka` is a file name. It is not the KAT location data and remains in the handle/id response.

### Feature or entity model

The protected-data rule is the standard `pristupnost` comparison: `A` anonym, `B` badatel, `C` archeolog, `D` archivář, `E` admin/ARÚ; the comparison is lexicographic and `E` is downgraded to `D` on the search path (see [`permissions`](permissions.md)). OAI masks the KAT `poznamka` when the record's role exceeds the caller's role; a record at or below the caller's role keeps the original value.

| Entity | `historie` model | Cadastre in protected data | In #1163 scope | Internal response |
| --- | --- | --- | --- | --- |
| `samostatny_nalez` | yes | yes (`chranene_udaje`) | yes | `historie` projected for `isAllowed`, then removed for every role |
| `projekt` | yes | yes (`chranene_udaje`) | yes | `historie` not projected; removal defensive |
| `archeologicky_zaznam` (`akce`, `lokalita`) | yes | yes (`chranene_udaje`) | yes | `historie` not projected; removal defensive |
| `adb` | no model field | cadastral-like `trat`/`parcela`, no `katastr` | no | not applicable |
| `pian` | yes | `loc`, no `katastr` | no | `historie` not projected |
| `dokument`, `ext_zdroj` | yes | no cadastre | no | not enumerated in this run |

The issue's "all records that have cadastres in protected data" therefore maps to `samostatny_nalez`, `projekt` and `archeologicky_zaznam`; the other types are out of scope by their data model.

### Discovery recipes

1. **Find KAT records in OAI.** `GET …/2.2/oai?verb=ListRecords&metadataPrefix=oai_amcr&set=<set>` with `set=archeologicky_zaznam` (akce and lokalita), `projekt`, or `samostatny_nalez`; follow `resumptionToken` for the next page. Inside each `<record>`, match `<amcr:typ_zmeny>KAT</amcr:typ_zmeny>` and read the record's `<amcr:pristupnost … id="HES-…">` (HES-000865=A, HES-000866=B, HES-000867=C, HES-000868=D). Pick one restricted candidate (B/C/D) and one public control (A). The test data has many KAT entries in the `archeologicky_zaznam` set, a single KAT record in `projekt`, and many in `samostatny_nalez`.
2. **Check OAI masking.** `GET …/{2.0|2.1|2.2}/oai?verb=GetRecord&identifier=<env-prefix>/id/<ident>&metadataPrefix=oai_amcr`. For each `historie` block with `typ_zmeny=KAT`, a restricted record must show `poznamka` = `HTTP/1.1 403 Forbidden`; a public control must show the original value. `ListRecords` applies the same filter.
3. **Check internal endpoint removal.** Request `/api/search/handle?id=<ident>`, `/api/search/query?entity=<entity>&q=ident_cely:"<ident>"` and `/api/search/id?id=<ident>`. The record-level `historie` key must be absent for the covered types; distinguish it from `soubor[].historie`.
4. **Role view.** OAI accepts Basic auth or a session. `GET /user/login?user=<email>&pwd=<pwd>` returns a `JSESSIONID` cookie; replay it on any surface. Test accounts and passwords are operator-provided per session and are never recorded here. In this run the supplied credentials were rejected (see the current verification).
5. **Production.** Probe production only with the operator's explicit approval; state what was compared. Data drifts, so never assume the same ident has the same state in both environments.

### Verification commands

```bash
# OAI page scan for KAT records (follow resumptionToken)
curl -s "https://api-test.aiscr.cz/2.2/oai?verb=ListRecords&metadataPrefix=oai_amcr&set=archeologicky_zaznam"
# OAI GetRecord (repeat for 2.0 and 2.1; use the environment's own prefix)
curl -s "https://api-test.aiscr.cz/2.2/oai?verb=GetRecord&identifier=https%3A%2F%2Fapi-test.aiscr.cz%2Fid%2F<IDENT>&metadataPrefix=oai_amcr"
# internal endpoints
curl -s "https://digiarchiv-test.aiscr.cz/api/search/handle?id=<IDENT>"
curl -s "https://digiarchiv-test.aiscr.cz/api/search/query?entity=<ENTITY>&q=ident_cely%3A%22<IDENT>%22"
curl -s "https://digiarchiv-test.aiscr.cz/api/search/id?id=<IDENT>"
# role session (operator-provided credentials; never record the password)
curl -s -D - "https://digiarchiv-test.aiscr.cz/user/login?user=<EMAIL>&pwd=<PASSWORD>" -o NUL
```

## Current verification (2026-11-15, test `digiarchiv-test.aiscr.cz` / `api-test.aiscr.cz`, build `v4.1.0-27-gffac9d9a`)

**Scope and limitations.** The run probed the test instance anonymously and attempted a B/C/D credential check. The supplied credentials were rejected by the test instance (`login_server_error` on `/user/login`; OAI Basic auth returned 401 "Invalid credentials"), so the entitled-user branch was not live-verified. Production was not probed. Concrete record ids are not embedded; re-derive candidates with recipe 1.

Grid legend: `✓` observed and matches the stated expectation · `✗` + finding id = deviation · `—` = not examined in this run.

### Verified behaviour matrix

| Capability | Result |
| --- | --- |
| OAI `GetRecord`, restricted `samostatny_nalez` (pr=D), KAT entry | `poznamka` = `HTTP/1.1 403 Forbidden` on 2.0/2.1/2.2 ✓ |
| OAI `GetRecord`, restricted `akce` (pr=C and pr=D), KAT entry | masked ✓ |
| OAI `GetRecord`, restricted `lokalita` (pr=C), KAT entry | masked ✓ |
| OAI `ListRecords`, `archeologicky_zaznam` and `samostatny_nalez` sets | restricted KAT records masked ✓ |
| OAI `GetRecord`, public `akce` (pr=A), KAT entries | original values retained (control) ✓ |
| Internal `handle`/`query`/`id`, restricted `akce`/`lokalita`/`samostatny_nalez` | record-level `historie` absent ✓ |
| Internal `handle`/`query`/`id`, public `akce`/`projekt` | record-level `historie` absent ✓ (not projected) |
| Internal `soubor[].historie` | file-level history remains; no KAT entries observed ✓ |
| Entitled-user OAI view | — (credentials rejected; maintainer-assisted) |
| Production | — (not probed; maintainer-assisted) |

### Known defects

- **history-location-protection-D01 — open, Low.** The internal endpoints remove the whole `historie` array for every role instead of masking only the KAT `poznamka` below the record's role. No data leak was found; the over-removal also drops non-sensitive history for entitled users on `samostatny_nalez` (the only covered type whose search projection carries `historie`). Confirm the intended API contract.

### Corrections made during this run

- The initial working assumption was that the internal-endpoint leak would be visible on `akce`, `lokalita` and `projekt` because `historie` was projected. Probe evidence corrected this: those searchers do not project `historie` (the entity `fields` configuration has no `historie` entry); only `samostatny_nalez` projects it for `isAllowed` and then removes it. The internal removal is effective for the SN path and defensive for the others.

### Maintainer-assisted and unverified

- **Entitled-user OAI view (maintainer-assisted).** A valid D/E test account is required to verify that a caller at or above the record's role receives the original KAT value. The supplied credentials were rejected at login; the public pr=A control proves the non-masking branch of the same condition, but the D/D case was not live-verified.
- **Production (maintainer-assisted).** Production was not probed; its #1163 state is not inferred from test. An anonymous comparison requires the operator's explicit approval.
- **Restricted `projekt` KAT candidate (unverified).** The test `projekt` set yielded one KAT record, and it was public (pr=A); no restricted `projekt` with a KAT entry was found. The OAI transform is entity-agnostic and was verified on restricted `archeologicky_zaznam` and `samostatny_nalez`; the internal `projekt` removal is verified as a no-op (no projection).
- No prior defects exist on this scenario; this is its first verification.

### Pre-existing observations

- `soubor[].historie` file-level history is served on the handle/id path and is outside the KAT scope; it was not removed by #1163.
- The `samostatny_nalez` `historie` projection was introduced for the #1151 access decision; #1163 removes it from the response after `isAllowed`. This is a correction of that projection's response side effect, not a new disclosure class.

## Verification log

| Date | Instance / build verified | What changed |
| --- | --- | --- |
| 2026-11-15 | Test `digiarchiv-test.aiscr.cz` / `api-test.aiscr.cz`, build `v4.1.0-27-gffac9d9a` | First verification: OAI KAT masking on restricted `samostatny_nalez`, `akce` and `lokalita` (2.0/2.1/2.2) and a public control; internal endpoints drop record-level `historie`; `D01` recorded (over-broad internal removal). |
