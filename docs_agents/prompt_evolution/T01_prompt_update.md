# T01 — Návrhy na zlepšení promptu

**Datum:** 2026-03-08
**Task:** T01 — Mapování struktury repozitáře

---

## Co v promptu chybělo / bylo nejasné

1. **Frontend lokace:** PROMPT.md zmiňuje `web/src/main/webapp/` jako frontend, ale skutečný
   Angular frontend se nachází v `web/src/main/ng/`. Pro budoucí agenty je důležité vědět,
   že `webapp/` obsahuje pouze minimální JSP/web descriptor soubory, zatímco vlastní frontend
   je v `web/src/main/ng/`.

2. **Docker konfigurace:** PROMPT.md předpokládá existenci docker-compose souborů
   (`docker-compose*.yml`) v kořeni repozitáře. Ty zde však neexistují. PROMPT.md by měl
   obsahovat upozornění, že infrastrukturní soubory mohou být v externím repozitáři.

3. **Absence testů:** PROMPT.md zmiňuje `web/src/test` jako důležitý adresář, ale ten
   neexistuje. Tato skutečnost by měla být zachycena v inicializační sekvenci — agent by měl
   ověřit existenci adresáře a zaznamenat absenci.

4. **Počet Java souborů:** Pro agenty by bylo užitečné vědět, že repozitář obsahuje 88 Java
   souborů (ne stovky) — pomáhá odhadnout zátěž pro T02–T06.

---

## Co by příštímu agentovi pomohlo

1. Vědět, že frontend je Angular aplikace v `web/src/main/ng/`, ne v `web/src/main/webapp/`.
2. Vědět, že Docker konfigurace není součástí tohoto repozitáře.
3. Vědět, že neexistují žádné automatizované testy (zásadní kontext pro T06, T08, T09).
4. Vědět, že existuje 14 Solr kolekcí (ne jen několik) — T03 pravděpodobně bude vyžadovat
   split na sub-tasky (T03a–T03n).

---

## Jaké soubory nebo adresáře by stálo za to přidat do important_files/directories v review_config.yaml

- `web/src/main/ng/` — Angular frontend (přidat do `important_directories`)
- `web/src/main/ng/package.json` — npm konfigurace (přidat do `important_files`)
- `web/src/main/ng/tsconfig.json` — TypeScript konfigurace (přidat do `important_files`)
- `ThumbnailsGenerator/pom.xml` — přidat do `important_files`
