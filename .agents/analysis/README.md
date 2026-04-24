# .agents/analysis

Strojově čitelné výstupy jednotlivých analytických tasků (JSON souhrny).

Každý soubor `_analysis.json` odpovídá jednomu tasku z `review_config.yaml`
(`T01`–`T10`) a slouží jako dlouhodobá paměť pro další agenty i pro lidské
reviewery.

## Typické soubory

- `repository_map.json` — strukturální mapa repozitáře (T01).
- `dependency_graph.json` — graf interních a externích závislostí (T02).
- `solr_analysis.json` — analýza Solr schémat, konfigurací a indexovacích vzorů (T03).
- `xslt_analysis.json` — analýza XSLT transformací a vazby na AMČR API (T04).
- `docker_analysis.json` — analýza Dockerfile a docker-compose konfigurací (T05).
- `security_analysis.json` — shrnutí bezpečnostních zjištění (T06).
- `frontend_analysis.json` — analýza vlastního TypeScript/JS/SCSS kódu (T07).
- `documentation_analysis.json` — stav dokumentace a Javadoc pokrytí (T08).
- `cicd_analysis.json` — CI/CD pipeline a GitHub Actions (T09).
- `scripts_analysis.json` — build a deployment skripty (T10).

## Účel

- zachytit zjištění v kompaktní podobě (pro další běhy agentů),
- umožnit porovnání stavů v čase (před/po refaktoringu),
- sloužit jako vstup pro finální audit (T11) a pro tvorbu refactoring backlogu.
