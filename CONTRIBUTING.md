# Přispívání do projektu — Digitální archiv AMČR

Děkujeme za zájem o přispívání do projektu!  
Tento dokument popisuje vývojový postup, konvence a pravidla pro přispěvatele.

---

## Větve a prostředí

| Větev | Prostředí | Pravidlo |
| --- | --- | --- |
| `dev` | Staging | Základna pro veškerý vývoj. Vždy větvete od `dev`. |
| `main` | Stabilní | Merguje výhradně lidský reviewer. Nevytvářejte PR přímo do `main`. |

```text
dev  ←  feature/<issue>
dev  ←  bugfix/<issue>
dev  ←  agents/{agent_name}/<topic>   # větve generované AI agenty
main ←  (pouze humans, po stabilizaci dev)
```

---

## Pojmenování větví

| Typ | Vzor | Příklad |
| --- | --- | --- |
| Nová funkce | `feature/<issue>` | `feature/142-fulltext-filter` |
| Oprava chyby | `bugfix/<issue>` | `bugfix/98-solr-timeout` |
| Agentní obsah | `agents/{agent_name}/<topic>` | `agents/codex/solr-audit` |
| Hotfix na main | `hotfix/<issue>` | `hotfix/200-critical-security` |

---

## Postup pro přispěvatele

1. **Vytvořte issue** (nebo najděte existující) popisující problém nebo funkci.
2. **Větvete od `dev`:**

   ```bash
   git checkout dev
   git pull origin dev
   git checkout -b feature/<číslo-issue>
   ```

3. **Implementujte změnu** — dodržujte konvence popsané níže a v `AGENTS.md`.
4. **Napište nebo aktualizujte testy:**

   ```bash
   cd web && mvn test
   npm run build
   ```

5. **Vytvořte Pull Request** do větve `dev`.

---

## Formát Pull Requestu

PR musí obsahovat:

- **Odkaz na issue:** `Closes #<číslo>` nebo `Refs #<číslo>`
- **Motivace:** proč je změna potřebná
- **Popis změny:** co bylo změněno a jak
- **Testování:** co bylo spuštěno, co prošlo, co nešlo spustit

Použijte **Draft PR**, pokud práce není připravena k review.

**Nevytvářejte PR do `main`** — mergování do `main` je výhradně v kompetenci maintainerů.

---

## Commit zprávy

Formát:

```markdown
[typ] stručný popis (#číslo-issue)
```

Povolené typy:

| Typ | Kdy použít |
| --- | --- |
| `feat` | Nová funkce |
| `fix` | Oprava chyby |
| `refactor` | Refactoring bez změny chování |
| `test` | Přidání nebo úprava testů |
| `docs` | Pouze dokumentace |
| `chore` | Build, závislosti, CI konfigurace |
| `style` | Formátování, bez logické změny |
| `perf` | Optimalizace výkonu |

Příklady:

```markdown
[feat] Přidat fulltextový filtr podle typu záznamu (#142)
[fix] Opravit timeout Solr dotazu při prázdném výsledku (#98)
[docs] Aktualizovat README — sekce Testování (#0)
```

---

## Konvence kódu

### Java

- **Constructor injection** ve Spring komponentách — nikoli field injection (`@Autowired` na polích).
- **Logování výjimek** — výjimky vždy logujte před re-throw nebo zpracováním; nikdy nespolykejte.
- **Parametrizované dotazy** — JPQL / SQL nesmí obsahovat string concatenation s uživatelským vstupem.
- **Javadoc v češtině** pro všechny veřejné třídy a metody v modulu `web/`.
- Styl: standardní `@param`, `@return`, `@throws` — bez Google-style sekcí (`Args:`, `Returns:`).
- Popis musí být konkrétní k chování kódu, ne generické šablony.

### TypeScript / JavaScript

- `strict: true` v `tsconfig.json` — nevypínat.
- Vyhýbejte se `any`; definujte explicitní typy a rozhraní.
- Preferujte `async/await` před callback patternem.
- Žádný `console.log` v produkčním kódu.

### XSLT

- Každý soubor musí mít **hlavičkový komentář** s popisem účelu a vstupním namespace.
- XSLT 2.0 / 3.0 (Saxon) — nevracejte se k XSLT 1.0 vzorům.
- Namespace prefixy musí být konzistentní napříč všemi soubory.
- Každá změna musí být validována proti vzorovým AMČR API datům.

### SCSS

- Pojmenování tříd podle **BEM** metodologie.
- Nové hodnoty barev a rozměrů patří do `_variables.scss`.
- Žádné inline styly v HTML / Thymeleaf šablonách.

---

## Testy před odesláním PR

**Minimum:**

```bash
# Java kompilace a unit testy
cd web && mvn compile -q && mvn test

# TypeScript / SCSS build
npm run build
```

**Při změnách XSLT:**  
Validujte transformace proti vzorovým XML datům z AMČR API.

**Při změnách Solr schématu:**  
Spusťte lokální Solr a proveďte smoke-test dotaz.  
Uveďte v PR, zda změna vyžaduje reindexaci.

Výsledky testů vždy popište v PR (`co bylo spuštěno`, `co prošlo`, `co nešlo spustit`).

---

## Zvláštní pravidla

### Solr schéma

Změny v `solr/` mohou vyžadovat **reindexaci** existujících dat.  
Každý PR se změnou schématu musí v popisu explicitně uvést, zda je reindexace potřebná.

### AMČR API kompatibilita

XSLT transformace jsou úzce svázány s formátem odpovědí AMČR API (`aiscr-webamcr`).  
Sledujte changelog AMČR a po každé API změně upstream otestujte transformace.

### Bezpečnost

- Nikdy necommitujte přihlašovací údaje, API klíče ani hesla.
- Nové REST endpointy musí mít příslušné Spring Security anotace.
- Uživatelský vstup validujte před předáním do XSLT i Solr dotazů.

---

## AI agenti

Větve generované AI agenty (`agents/{agent_name}/<topic>`) se větvují od `dev`  
a mergují do `dev` výhradně po lidském review.

Podrobnosti viz `AGENTS.md` a `.agents/prompts/review_codebase.md`.

### Jak spustit review session

Otevřete nový kontext AI agenta a jako první zprávu vložte:

```
Přečti .agents/prompts/review_codebase.md a pokračuj v review.
```

Agent si načte `AGENTS.md`, stav z `.agents/config/review_cache.json` a zahájí
další čekající task dle registru v `.agents/prompts/review_codebase.md`.

---

## Kontakt

- **Issues:** https://github.com/ARUP-CAS/aiscr-digiarchiv-2/issues
- **Archeologický ústav AV ČR, Praha:** amcr@arup.cas.cz
