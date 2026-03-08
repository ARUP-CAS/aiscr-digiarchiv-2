# Evidované chyby — Digitální archiv AMČR (aiscr-digiarchiv-2)

> Všechny záznamy jsou psány v češtině.
> Před přidáním nové chyby ověř existující GitHub Issues (aktuálně 60 otevřených).
>
> Stavy: `již evidováno (Issue #XXX)` | `rozšíření existujícího issue #XXX` | `nový kandidát na issue`
>
> Závažnosti: `Kritická` | `Vysoká` | `Střední` | `Nízká`

---

### BUG-001: Saxon 8.7 — kriticky zastaralý XSLT procesor bez bezpečnostních záplat

- **Soubor:** `web/pom.xml:125`
- **Závažnost:** Vysoká
- **GitHub Issue:** nový kandidát na issue
- **Popis:** Maven závislost `net.sf.saxon:saxon:8.7` pochází z roku 2006 (cca 18 let stará verze). Aktuální open-source verze je Saxon-HE 12.x. Absence bezpečnostních záplat za 18 let vývoje představuje bezpečnostní riziko při zpracování vstupního XML z Fedora API. AGENTS.md deklaruje použití XSLT 2.0/3.0 procesoru, ale Saxon 8.7 XSLT 3.0 nepodporuje a má jen částečnou XSLT 2.0 podporu.
- **Navrhovaná oprava:** Nahradit závislost za `net.sf.saxon:Saxon-HE:12.5` (open-source, zpětně kompatibilní pro XSLT 2.0, plná XSLT 3.0 podpora). Po upgradu ověřit existující XSLT transformace.
- **Task:** T02

---

### BUG-002: javax.mail namespace nekompatibilní s Jakarta EE 11

- **Soubor:** `web/pom.xml:78`
- **Závažnost:** Střední
- **GitHub Issue:** nový kandidát na issue
- **Popis:** Závislost `javax.mail:mail:1.4.7` používá starý `javax` namespace. Projekt cílí na Jakarta EE 11, která používá `jakarta.*` namespace. Na moderních application serverech (Tomcat 10+, WildFly 27+) může dojít ke classloading konfliktu nebo nefunkčnímu odesílání e-mailů.
- **Navrhovaná oprava:** Nahradit za `jakarta.mail:jakarta.mail-api` + implementaci (Angus Mail: `org.eclipse.angus:angus-mail`). Aktualizovat i `org.apache.commons:commons-email`, který staví na javax.mail.
- **Task:** T02

---

<!-- Záznamy přidávají agenti po dokončení jednotlivých tasků -->

---

### BUG-003: Překlep multiValued="fslse" v entities schématu — nedefinované chování indexace

- **Soubor:** `solr/entities/conf/managed-schema:157`
- **Závažnost:** Kritická
- **GitHub Issue:** nový kandidát na issue
- **Popis:** Pole `samostatny_nalez_projekt` má `multiValued="fslse"` — překlep místo `"false"`. Solr přijímá neplatný boolean atribut a jeho chování závisí na verzi. Může způsobit neočekávané multi-valued chování (ukládání více hodnot do single-value pole) nebo selhání indexace záznamu samostatného nálezu s chybou parse výjimky.
- **Navrhovaná oprava:** Opravit na `multiValued="false"`. Po opravě provést reindexaci kolekce entities (nebo alespoň záznamy typu `samostatny_nalez`).
- **Task:** T03

---

### BUG-004: FedoraHarvester.indexModels() — hardcodovaný limit 10 000 000 záznamů bez stránkování

- **Soubor:** `web/src/main/java/cz/inovatika/arup/digiarchiv/web4/fedora/FedoraHarvester.java:263`
- **Závažnost:** Vysoká
- **GitHub Issue:** nový kandidát na issue
- **Popis:** Metoda `indexModels()` nastavuje `max_results=10000000` a načítá celou odpověď Fedora API jako jeden `JSONObject` do paměti JVM. Při rozsáhlé databázi (tisíce dokumentů s metadaty) to vede k `OutOfMemoryError`. Paradoxně `checkDatestamp()` ve stejné třídě správně používá `CursorMark` pro iteraci — ale `indexModels()` toto neimplementuje. Plná reindexace (triggered např. po migraci) tak může shodit aplikační server.
- **Navrhovaná oprava:** Implementovat cursor-based pagination pomocí `CursorMarkParams` (stejně jako `checkDatestamp()`), nebo offset-based stránkování s `batchSize=1000`.
- **Task:** T03

---

### BUG-005: luceneMatchVersion=9.4 vs. solr-solrj 9.10.1 — konfigurační nesoulad ve všech 14 kolekcích

- **Soubor:** `solr/entities/conf/solrconfig.xml:38` (a dalších 13 souborů solrconfig.xml)
- **Závažnost:** Vysoká
- **GitHub Issue:** nový kandidát na issue
- **Popis:** Všechny `solrconfig.xml` soubory deklarují `<luceneMatchVersion>9.4</luceneMatchVersion>`, zatímco klientská knihovna `solr-solrj` má verzi 9.10.1 (viz `web/pom.xml:119`). Pokud Solr server běží ve verzi 9.10.x (odpovídající klientovi), stará `luceneMatchVersion` způsobuje: (1) neaktivaci Lucene 9.5–9.10 optimalizací indexu a tokenizace; (2) potenciální rozdíly v chování analyzerů při plné reindexaci vs. dotazování. Žádná migration note neexistuje.
- **Navrhovaná oprava:** Zjistit aktuální verzi Solr serveru. Aktualizovat `luceneMatchVersion` na shodnou hodnotu ve všech 14 `solrconfig.xml`. Následně provést plnou reindexaci všech kolekcí.
- **Task:** T03
