# T02 — Návrhy na zlepšení promptu

**Task:** T02 — Graf interních a externích závislostí
**Datum:** 2026-03-08

---

## Co v promptu chybělo / bylo nejasné

1. **Hloubka analýzy interních závislostí:** Prompt zadává analýzu Java package dependencies, ale neurčuje, zda stačí na úrovni balíčků nebo je potřeba mapovat konkrétní třídy. Bylo by užitečné explicitně požadovat analýzu god tříd (LOC > 500) a jejich interních vazeb.

2. **Sledování EOL závislostí:** Prompt neurequiruje explicitní kontrolu EOL stavu závislostí. Doporučuji přidat pokyn: *"Pro každou Maven/npm závislost ověř, zda projekt není end-of-life (EOL) nebo v maintenance mode."*

3. **Namespace kompatibilita Jakarta EE:** Projekt cílí na Jakarta EE 11, ale tato skutečnost není v T02 sekci zohledněna. Doporučuji přidat instrukci: *"Ověřit, zda všechny Maven závislosti používají `jakarta.*` namespace (ne starý `javax.*`) v souladu s deklarovanou Jakarta EE verzí."*

4. **Version range závislosti:** Prompt neřeší detekci nedeterministických range verzí v Maven (typ `[0.4,0.5)`). Doporučuji přidat: *"Označit závislosti s range verzemi jako rizikové pro reprodukovatelnost buildu."*

---

## Co by příštímu agentovi pomohlo

1. **Přidat do known_facts v review_config.yaml:**
   - `saxon_version: "8.7"` — kriticky zastaralý, cílová verze `Saxon-HE 12.x`
   - `jakarta_ee_version: "11.0.0"` — všechny závislosti musí používat `jakarta.*` namespace
   - `java_version: "17"` — hlavní aplikace; ThumbnailsGenerator stále na Java 1.8

2. **Sekce T03 (Solr):** Při analýze Solr schémat ověřit soulad s `solr-solrj:9.10.1` — verze klienta musí odpovídat verzi Solr serveru.

3. **Sekce T04 (XSLT):** Při analýze XSLT souborů ověřit, zda soubory využívají konstrukty XSLT 3.0 (které Saxon 8.7 nepodporuje) — pokud ano, upgrade Saxon je urgentní.

---

## Jaké soubory nebo adresáře by stálo za to přidat do known_facts / important_files

- `web/src/main/ng/angular.json` — build konfigurace Angular (production/development profily, optimalizace)
- `web/src/main/ng/tsconfig.json` — TypeScript strict mode nastavení
- `web/src/main/webapp/META-INF/context.xml` — deployment descriptor pro Tomcat

---

## Poznámky k procesu

- Analýza Java interních závislostí byla provedena čtením import sekcí klíčových tříd — automatizace přes `jdeps` by byla efektivnější.
- Saxon verze 8.7 je uvedena v pom.xml jako `net.sf.saxon:saxon` (starý Maven artifactId); novější verze Saxon-HE mají artifactId `net.sf.saxon:Saxon-HE`.
