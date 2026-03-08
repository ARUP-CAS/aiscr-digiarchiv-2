# Refactoring backlog — Digitální archiv AMČR (aiscr-digiarchiv-2)

> Všechny záznamy jsou psány v češtině.
> Strukturální zlepšení objevená během auditu.

---

## Vysoká priorita

<!-- Architektonické problémy, bezpečnostní dluhy, Solr výkon -->

### [T01] Absence automatizovaných testů
- **Zjištění:** Adresář `web/src/test` neexistuje. Projekt nemá žádné automatizované Java testy.
- **Dopad:** Regresní změny nelze automaticky odhalit; CI pipeline nemůže ověřit správnost kódu.
- **Návrh:** Přidat unit testy pro SearchUtils, IndexUtils, FormatUtils, FedoraUtils.
  Zvážit integrační testy Solr dotazů.

## Střední priorita

<!-- Optimalizace, XSLT refaktoring, Docker build -->

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

### [T01] Překlep v názvu Java balíčku: `imagging`
- **Zjištění:** Balíček `cz.inovatika.arup.digiarchiv.web4.imagging` obsahuje překlep.
- **Dopad:** Matoucí pro vývojáře, zjevné v importech a dokumentaci.
- **Návrh:** Přejmenovat balíček na `imaging` (IDE refactor → rename package).

### [T01] Nekonzistentní base package v ThumbnailsGenerator
- **Zjištění:** ThumbnailsGenerator používá `cz.incad.arup`, hlavní aplikace `cz.inovatika.arup`.
- **Dopad:** Ztěžuje orientaci v kódové základně; naznačuje historicky odlišný původ modulu.
- **Návrh:** Sjednotit base package (při příležitosti větší refaktorace modulu).
