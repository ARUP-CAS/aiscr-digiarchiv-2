# Digitální archiv AMČR

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.8329064.svg)](https://doi.org/10.5281/zenodo.8329064) [![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

Digitální archiv Archeologické mapy České republiky (AMČR) je webová aplikace určená k prohlížení informací o archeologických výzkumech, lokalitách a nálezech, provozovaná Archeologickými ústavy AV ČR v Praze a Brně.  
Archivy těchto institucí obsahují dokumentaci terénních archeologických výzkumů na území ČR od r. 1919 do současnosti.  
Databáze AMČR a související dokumenty tvoří největší soubor archeologických dat dotýkajících se Česka.

Digitální archiv AMČR obsahuje různé druhy záznamů — jednotlivé archeologické dokumenty (texty, terénní fotografie, letecké snímky, mapy a plány, digitální data), projekty, terénní akce, archeologické lokality, evidenci samostatných nálezů i knihovnu 3D modelů.  
Data a popisné údaje jsou průběžně přebírány z AMČR, s níž je Digitální archiv propojen také uživatelskými účty.

Obsažená data jsou zveřejňována v souladu s politikou otevřeného přístupu k informacím a se souhlasem držitelů autorských práv.  
Většina dat je přístupná každému uživateli, dokumenty jsou obvykle dostupné po registraci uživatelského účtu a menší část údajů slouží pouze odborníkům z oprávněných archeologických organizací.

Digitální archiv AMČR je součástí Archeologického informačního systému České republiky (AIS CR) zapsaného do Cestovní mapy velkých výzkumných infrastruktur ČR.

> Do verze v1.x.x byla aplikace vyvíjena v repozitáři: https://github.com/ARUP-CAS/aiscr-digiarchiv/

---

## Odkazy

- **Produkční aplikace:** https://digiarchiv.aiscr.cz/
- **AMČR (zdrojový systém):** https://amcr.aiscr.cz/
- **AMČR API:** https://api.aiscr.cz/
- **Uživatelská nápověda:** https://amcr-help.aiscr.cz/
- **AIS CR:** https://www.aiscr.cz/

---

## Technologický stack

| Vrstva | Technologie |
| --- | --- |
| Backend | Java, Spring |
| Vyhledávání | Apache Solr |
| Datové transformace | XSLT 2.0/3.0 (Saxon) |
| Frontend | TypeScript, SCSS, HTML (Thymeleaf) |
| Infrastruktura | Docker, Docker Compose |
| CI/CD | GitHub Actions |
| Generování náhledů | ThumbnailsGenerator (standalone Java) |

---

## Struktura repozitáře

```text
aiscr-digiarchiv-2/
├── .github/              # GitHub Actions CI/CD workflows, CODEOWNERS
├── ThumbnailsGenerator/  # Standalone Java utilita pro generování náhledů
├── solr/                 # Konfigurace Apache Solr (schémata, configsets)
├── web/                  # Hlavní webová aplikace
│   ├── src/main/java/        # Java backend (Spring)
│   ├── src/main/resources/   # Konfigurace, XSLT transformace
│   ├── src/main/webapp/      # Frontend (TypeScript, SCSS, HTML šablony)
│   └── src/test/             # Unit a integrační testy
├── docs_agents/          # Dokumentace a konfigurace pro AI agenty
├── AGENTS.md             # Pravidla a instrukce pro AI coding agenty
├── CITATION.cff          # Citační metadata
└── README.md             # Tento soubor
```

---

## Vývojové prostředí

### Prerekvizity

- Java 17+
- Maven 3.8+ (nebo Gradle — ověřte přítomnost `web/pom.xml` vs. `web/build.gradle`)
- Node.js 18+ a npm (pro TypeScript a SCSS build)
- Docker a Docker Compose

### Spuštění lokálně

```bash
# 1. Spustit Solr
docker compose up solr -d

# 2. Sestavit a spustit webovou aplikaci
cd web
mvn clean package
mvn spring-boot:run

# 3. Sestavit frontend (je-li potřeba odděleně)
npm install
npm run build
```

### Konfigurace prostředí

Citlivé hodnoty (API klíče, hesla, connection strings) se **nikdy necommitují**.  
Použijte proměnné prostředí nebo `.env` soubor (viz `.gitignore`).

---

## Testování

```bash
# Unit testy (Java)
cd web && mvn test

# Integrační testy
cd web && mvn verify

# TypeScript build check
npm run build
```

---

## Větve a workflow

| Větev | Prostředí | Popis |
| --- | --- | --- |
| `dev` | Staging | Aktivní vývoj, integrace nových funkcí |
| `main` | Stabilní | Stabilizovaný kód, příprava na release |

Nové funkce a opravy se vyvíjejí v `dev`.  
Do `main` se merguje teprve po stabilizaci — výhradně lidským reviewerem.  
Release je vytvářen z tagovaného commitu na `main` (sémantické verzování `v4.x.x`).

Viz `CONTRIBUTING.md` pro podrobný vývojový postup.

---

## AI agenti

Repozitář obsahuje konfiguraci pro AI coding agenty (OpenAI Codex, Claude Code a další):

- `AGENTS.md` — pravidla a konvence pro agenty
- `docs_agents/` — průběžné audity, analýzy a backlog

---

## Citace

```markdown
AIS CR: Digitální archiv AMČR. [cit. YYYY-MM-DD].
Dostupné z: https://digiarchiv.aiscr.cz/
DOI: 10.5281/zenodo.8329064
```

Viz také `CITATION.cff`.

---

## Licence

[GPL-3.0](LICENSE)
© [Archeologický ústav AV ČR, Praha, v.v.i.](https://www.arup.cas.cz/)
© [Archeologický ústav AV ČR, Brno, v.v.i.](https://www.arub.cz/)
