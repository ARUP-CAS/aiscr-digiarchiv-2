# Refactoring backlog — Digitální archiv AMČR (aiscr-digiarchiv-2)

> Všechny záznamy jsou psány v češtině.
> Strukturální zlepšení objevená během auditu.

---

## Vysoká priorita

<!-- Architektonické problémy, bezpečnostní dluhy, Solr výkon -->

### [T02] Saxon 8.7 — kriticky zastaralý XSLT procesor
- **Zjištění:** `net.sf.saxon:saxon:8.7` (vydán 2006) — aktuální je Saxon-HE 12.x. Chybí ~18 let bezpečnostních záplat.
- **Dopad:** Bezpečnostní riziko; Saxon 8.7 nepodporuje XSLT 3.0 (rozpor s AGENTS.md).
- **Návrh:** Upgradovat na `net.sf.saxon:Saxon-HE:12.5`; ověřit kompatibilitu existujících XSLT.

### [T02] God třídy: SolrSearcher (1 048 ř.) a FedoraHarvester (1 024 ř.)
- **Zjištění:** Obě třídy mají nadměrné zodpovědnosti (query building, access control, HTTP komunikace, transformace dat vše pohromadě).
- **Dopad:** Nízká testovatelnost, vysoká komplexita změn, riziko regresí.
- **Návrh:** Extrahovat zodpovědnosti do specializovaných tříd (Strategy/Command pattern).

### [T02] Absence automatizovaných testů
- **Zjištění:** Adresář `web/src/test` neexistuje. Projekt nemá žádné automatizované Java testy.
- **Dopad:** Regresní změny nelze automaticky odhalit; CI pipeline nemůže ověřit správnost kódu.
- **Návrh:** Přidat unit testy pro SearchUtils, IndexUtils, FormatUtils, FedoraUtils.
  Zvážit integrační testy Solr dotazů.

## Střední priorita

<!-- Optimalizace, XSLT refaktoring, Docker build -->

### [T02] javax.mail v Jakarta EE 11 projektu
- **Zjištění:** `javax.mail:mail:1.4.7` používá starý `javax` namespace; Jakarta EE 11 očekává `jakarta.mail`.
- **Dopad:** Možný classloading konflikt na moderním application serveru (Tomcat 11+, WildFly 27+).
- **Návrh:** Nahradit `jakarta.mail:jakarta.mail-api` + Angus Mail implementace.

### [T02] EOL závislost: Apache XML-RPC
- **Zjištění:** `org.apache.xmlrpc:xmlrpc-client:3.1.3` — end-of-life od roku 2016.
- **Dopad:** Bez bezpečnostních aktualizací; zbytečná závislost.
- **Návrh:** Identifikovat použití v kódu a nahradit `java.net.http`.

### [T02] Žádný parent POM ani BOM
- **Zjištění:** Verze sdílených závislostí (`pdfbox`, `commons-io`, `org.json`, `solr-solrj`) jsou spravovány samostatně v `web/pom.xml` i `ThumbnailsGenerator/pom.xml`.
- **Dopad:** Ruční synchronizace verzí; riziko nekonzistentních verzí při upgradu.
- **Návrh:** Přidat root parent POM s `dependencyManagement` sekcí.

### [T02] ThumbnailsGenerator cílí na Java 1.8
- **Zjištění:** `maven.compiler.source/target = 1.8`, hlavní aplikace cílí na Java 17.
- **Dopad:** Nekonzistentní runtime; Java 1.8 je EOL (bez bezpečnostních záplat od Oracle).
- **Návrh:** Upgradovat na Java 17.

### [T02] Duplicita HTTP klientů v main aplikaci
- **Zjištění:** `org.apache.httpcomponents:httpclient:4.5.14` v Maven + `java.net.http` v kódu.
- **Dopad:** Zbytečná závislost navyšuje WAR; dva různé paradigmaty HTTP komunikace.
- **Návrh:** Standardizovat na `java.net.http`.

### [T01] Chybějící managed-schema pro Solr kolekci `oai`
- **Zjištění:** `solr/oai/conf/` neobsahuje `managed-schema` (ostatní kolekce ho mají).
- **Dopad:** Nejasné schéma kolekce; riziko neočekávaného chování při indexaci.
- **Návrh:** Ověřit záměrnost; doplnit explicitní schéma nebo komentář do solrconfig.xml.

### [T01] Duplikovaný solrconfig.xml ve 10 kolekcích
- **Zjištění:** 10 ze 14 Solr kolekcí sdílí identický `solrconfig.xml`
  (SHA-256: `5ce00b28...`).
- **Dopad:** Každá změna musí být propagována do 10 souborů ručně.
- **Návrh:** Zvážit Solr configset inheritance nebo generování konfigurace v CI.

### [T01] Chybějící Docker/infrastrukturní konfigurace v repozitáři
- **Zjištění:** Žádné Dockerfile ani docker-compose soubory v repozitáři.
- **Dopad:** Lokální spuštění aplikace není možné bez externích zdrojů; ztěžuje onboarding.
- **Návrh:** Přidat vývojový `docker-compose.yml` nebo dokumentovat odkaz na infrastrukturní repozitář.

## Nízká priorita

<!-- Kosmetické úpravy, dokumentace, minor code quality -->

### [T02] Axios nadbytečný vedle Angular HttpClient
- **Zjištění:** `axios:^1.11.0` v npm závislostech — duplicita s Angular HttpClient.
- **Návrh:** Ověřit využití; migrovat na Angular HttpClient a odstranit axios.

### [T02] Moment.js v maintenance mode
- **Zjištění:** `moment:^2.30.1` — Moment.js je v maintenance mode od roku 2020.
- **Návrh:** Migrovat na `date-fns` nebo nativní Angular `DatePipe`.

### [T02] Version range pro thumbnailator
- **Zjištění:** `net.coobird:thumbnailator:[0.4,0.5)` — nedeterministický build.
- **Návrh:** Zafixovat konkrétní verzi (např. `0.4.20`).

### [T02] @ngu/carousel verze pro Angular 20 v Angular 21 aplikaci
- **Zjištění:** `@ngu/carousel:^20.0.1` — carousel pro Angular 20, aplikace je Angular 21.
- **Návrh:** Upgradovat na verzi kompatibilní s Angular 21 nebo nahradit.

### [T02] Duplicita path validation v ApiServlet a InitServlet
- **Zjištění:** Obě třídy implementují vlastní normalizaci cest.
- **Návrh:** Extrahovat do sdílené utility třídy `PathUtils`.

### [T01] Překlep v názvu Java balíčku: `imagging`
- **Zjištění:** Balíček `cz.inovatika.arup.digiarchiv.web4.imagging` obsahuje překlep.
- **Dopad:** Matoucí pro vývojáře, zjevné v importech a dokumentaci.
- **Návrh:** Přejmenovat balíček na `imaging` (IDE refactor → rename package).

### [T01] Nekonzistentní base package v ThumbnailsGenerator
- **Zjištění:** ThumbnailsGenerator používá `cz.incad.arup`, hlavní aplikace `cz.inovatika.arup`.
- **Dopad:** Ztěžuje orientaci v kódové základně; naznačuje historicky odlišný původ modulu.
- **Návrh:** Sjednotit base package (při příležitosti větší refaktorace modulu).
