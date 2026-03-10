# AMCR Digital Archive

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.8329064.svg)](https://doi.org/10.5281/zenodo.8329064) [![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

The Digital Archive of the Archaeological Map of the Czech Republic (AMCR) is a web application designed for browsing information about archaeological excavations, sites, and finds, operated by the Institutes of Archaeology of the Czech Academy of Sciences in Prague and Brno.  
The archives of these institutions contain documentation of archaeological fieldwork conducted in the territory of the Czech Republic from 1919 to the present.  
The AMCR database and related documents constitute the largest collection of archaeological data concerning the Czech Republic.

The AMCR Digital Archive contains various types of records — individual archaeological documents (texts, field photographs, aerial images, maps and plans, digital data), projects, fieldwork events, archaeological sites, records of individual finds, as well as a library of 3D models.  
Data and descriptive metadata are continuously imported from AMCR, with which the Digital Archive is also connected through shared user accounts.

The contained data are published in accordance with the open access policy and with the consent of copyright holders.  
Most data are accessible to all users, documents are usually available after user registration, and a smaller portion of information is accessible only to professionals from authorized archaeological organizations.

The AMCR Digital Archive is part of the Archaeological Information System of the Czech Republic (AIS CR), included in the Roadmap of Large Research Infrastructures of the Czech Republic.

> Up to version v1.x.x the application was developed in the repository: https://github.com/ARUP-CAS/aiscr-digiarchiv/

---

## Links

- **Production application:** https://digiarchiv.aiscr.cz/
- **AMCR** (source system): https://amcr.aiscr.cz/
- **AMCR API:** https://api.aiscr.cz/
- **User documentation:** https://amcr-help.aiscr.cz/
- **AIS CR:** https://www.aiscr.cz/

---

## Technology stack

| Layer | Technology |
| --- | --- |
| Backend | Java, Spring |
| Search | Apache Solr |
| Data transformations | XSLT 2.0/3.0 (Saxon) |
| Frontend | TypeScript, SCSS, HTML (Thymeleaf) |
| Infrastructure | Docker, Docker Compose |
| CI/CD | GitHub Actions |
| Preview generation | ThumbnailsGenerator (standalone Java) |

---

## Repository structure

```text
aiscr-digiarchiv-2/
├── .github/              # GitHub Actions CI/CD workflows, CODEOWNERS
├── ThumbnailsGenerator/  # Standalone Java utility for preview generation
├── solr/                 # Apache Solr configuration (schemas, configsets)
├── web/                  # Main web application
│   ├── src/main/java/        # Java backend (Spring)
│   ├── src/main/resources/   # Configuration, XSLT transformations
│   ├── src/main/webapp/      # Frontend (TypeScript, SCSS, HTML templates)
│   └── src/test/             # Unit and integration tests
├── .agents/              # Documentation and configuration for AI agents
├── AGENTS.md             # Rules and instructions for AI coding agents
├── CITATION.cff          # Citation metadata
└── README.md             # This file
```

---

## Development environment

### Prerequisites

- Java 17+
- Maven 3.8+ (or Gradle — verify presence of `web/pom.xml` vs. `web/build.gradle`)
- Node.js 18+ and npm (for TypeScript and SCSS build)
- Docker and Docker Compose

### Running locally

```bash
# 1. Start Solr
docker compose up solr -d

# 2. Build and run the web application
cd web
mvn clean package
mvn spring-boot:run

# 3. Build frontend (if required separately)
npm install
npm run build
```

### Environment configuration

Sensitive values (API keys, passwords, connection strings) are **never committed**.  
Use environment variables or a `.env` file (see `.gitignore`).

---

## Testing

```bash
# Unit tests (Java)
cd web && mvn test

# Integration tests
cd web && mvn verify

# TypeScript build check
npm run build
```

---

## Branches and workflow

| Branch | Environment | Description |
| --- | --- | --- |
| `dev` | Staging | Active development, integration of new features |
| `main` | Stable | Stabilized code, preparation for release |

New features and fixes are developed in `dev`.  
Merges into `main` happen only after stabilization — exclusively by a human reviewer.  
Releases are created from tagged commits on `main` (semantic versioning `v4.x.x`).

See `CONTRIBUTING.md` for the detailed development workflow.

---

## AI agents

The repository includes configuration for AI coding agents (OpenAI Codex, Claude Code, and others):

- `AGENTS.md` — rules and conventions for agents
- `.agents/` — ongoing audits, analyses, and backlog

---

## Citation

```markdown
AIS CR: AMCR Digital Archive. [cit. YYYY-MM-DD].
Available at: https://digiarchiv.aiscr.cz/
DOI: 10.5281/zenodo.8329064
```

See also `CITATION.cff`.

---

## License

[GPL-3.0](LICENSE)  
© [Institute of Archaeology of the CAS, Prague](https://www.arup.cas.cz/)  
© [Institute of Archaeology of the CAS, Brno](https://www.arub.cz/)
