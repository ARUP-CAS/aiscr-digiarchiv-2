# T03 — Návrhy zlepšení promptu

**Datum:** 2026-03-08

---

## Co fungovalo dobře

1. **Prioritizace kolekcí** — instrukce prioritizovat `entities`, `work`, `heslar`, `soubor` a ostatní shrnout stručněji byla efektivní. Umožnila hloubkovou analýzu klíčového schématu bez překročení limitů.
2. **Explicitní seznam God tříd** (SolrSearcher, FedoraHarvester) v kontextu sessions — agent se mohl ihned zaměřit na správné soubory.
3. **max_files_per_task: 20** — dostatečné pro 14 configsetů + klíčové Java soubory, pokud se prioritizuje.

---

## Problémy zjištěné při provádění

### 1. Limit lines nezohledňuje velká schémata

Schéma `entities/managed-schema` má 1030+ řádků. Při `max_lines_per_task: 6000` byla nutná selektivní extrakce pomocí `grep` — plné čtení by limit vyčerpalo jediným souborem. Prompt by mohl explicitně doporučit grep-first přístup pro velká schémata.

**Návrh do PROMPT.md:**
```
Pro entities managed-schema (>1000 řádků): nejprve grep '<field\|<copyField\|<uniqueKey',
pak cíleně čti sekce dle potřeby. Nečti celý soubor najednou.
```

### 2. Chybí instrukce pro detekci version mismatch s Dockerem

Prompt zmiňuje "mismatch solr-solrj verze vs. verze Solr serveru v konfiguraci nebo Docker files". Repozitář nemá Docker soubory (T01 to potvrdil). Prompt by měl explicitně říct, jak postupovat při absenci Dockeru: zkontrolovat `pom.xml` + `solrconfig.xml`.

**Návrh:**
```
Pokud Docker soubory neexistují (viz known_facts z T01), porovnej
solr-solrj verzi v pom.xml s luceneMatchVersion v solrconfig.xml.
```

### 3. Chybí instrukce pro detekci "field written in Java but missing in schema"

Prompt říká "pole v dotazech která chybí v schématu", ale neuvádí, jak zkontrolovat pole ZAPISOVANÁ v Java kódu. Klíčovou technikou je extrakce field názvů z `addField()` volání a porovnání se schématem.

**Návrh:**
```
Pro detekci Java→Solr mismatch:
1. Extrahuj field names z addField() volání: grep -oP '"[a-z_]+"' model/*.java
2. Porovnej s explicitními a dynamickými poli schématu.
Zaměř se na pole s prefixem adb_*, az_*, dokument_* v ArcheologickyZaznam.
```

### 4. Instrukce o "stored fields nikdy nevracené" je obtížně ověřitelná bez runtime dat

Analyticky lze identifikovat stored fields, ale určit "nikdy nevracené" vyžaduje znalost všech `setFields()` volání v kódu. Prompt by měl uznat tuto limitaci a doporučit heuristiku.

**Návrh:**
```
Pro "stored but never returned" proveď heuristiku:
grep -rn "setFields" — pole, která nejsou nikde v setFields() a nemají obvious use
(nejsou v /search handleru, v result transformaci) jsou kandidáty.
Tato detekce je přibližná; označuj jako "potenciálně nevyužité".
```

### 5. Chybí instrukce pro kontrolu přístupové bezpečnosti Solr kolekcí

Kolekce `uzivatel` obsahuje email, telefon, hesla — citlivá data indexovaná bez ochrany. Prompt by mohl zahrnout kontrolu citlivých polí v kolekcích s uživatelskými daty.

**Návrh:**
```
Pro kolekce uzivatel, uzivatel_ui, organizations: zkontroluj, zda citlivá pole
(email, telefon, heslo, adresa) mají indexed=false nebo jsou chráněna
field-level security. Eskaluj do bugs.md při nálezu.
```

---

## Doporučení pro prompt T04 (XSLT analýza)

Na základě zjištění T03: Saxon 8.7 (z T02) zpracovává XSLT transformace Fedora API odpovědí. T04 by měl explicitně prověřit, zda XSLT soubory obsahují konstrukty nekompatibilní se Saxonem 8.7 (zejm. XSLT 2.0 funkce jako `xs:`, `for-each-group`, `sequence`).
