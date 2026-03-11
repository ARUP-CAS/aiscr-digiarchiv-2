# Claude Code Automation Recommendations

Doporučení pro automatizace Claude Code s důrazem na **sdílená pravidla v AGENTS.md a související struktuře** — ne v `.cursor/`, aby konfigurace nebyla v `.gitignore` a byla verzována.

---

## Stav aplikace (Applied)

| Položka | Stav | Kde |
|--------|------|-----|
| Sdílená pravidla v AGENTS.md / .agents/ | ✅ Aplikováno | `AGENTS.md` — sekce „Shared automation and rules (no .cursor)“ |
| Kdy volat skills (doc, gh-fix-ci, gh-address-comments) | ✅ Přeconfigurováno | `AGENTS.md` — pouze gh-fix-ci a gh-address-comments doporučeny; `doc` popsán jako .docx-only, tento repo nemá .docx |
| Doporučené subagenty (code-reviewer, security-reviewer) | ✅ Aplikováno | `AGENTS.md` — nová podsekce „Recommended subagents“ |
| Pre-PR skript (sdílené „hook“ chování) | ✅ Aplikováno | `scripts/pre-pr-check.sh`, `scripts/pre-pr-check.ps1` |
| Odkaz na skript v CONTRIBUTING / AGENTS | ✅ Aplikováno | `CONTRIBUTING.md` (Testy před odesláním PR), `AGENTS.md` (Testing Expectations) |
| MCP (context7, GitHub) | ⏳ Akce uživatele | Instalace v nástroji (`claude mcp add …`); konfigurace dle dokumentace |
| Skills (doc, gh-fix-ci, gh-address-comments) | ✅ Přeconfigurováno | Skilly zkontrolovány dle SKILL.md; v AGENTS.md doporučeny gh-fix-ci a gh-address-comments; doc pouze pro .docx (repo je bez .docx) |

---

## Codebase Profile

- **Type**: Java 17 + Angular 21 (TypeScript), Maven
- **Backend**: Spring, Jakarta EE 11, Solr 9.x, Saxon (XSLT 2.0)
- **Frontend**: Angular 21, Angular Material, Leaflet, ECharts
- **CI/CD**: GitHub Actions
- **Key paths**: `web/src/main/java/`, `web/src/main/ng/`, `solr/`, `.agents/`

---

## Kde mají být sdílená pravidla (ne .cursor)

| Účel | Umístění (verzované) | Poznámka |
|------|----------------------|----------|
| Agent governance, scope, „do not“ | `AGENTS.md` | Jedna pravda pro agenty |
| Větve, PR, commit, testování | `CONTRIBUTING.md` | Workflow a konvence |
| Rychlá orientace, build, gotchas | `CLAUDE.md` | Kontext pro Claude |
| Opakovatelné prompty, review | `.agents/prompts/` | např. `review_codebase.md` |
| Konfigurace úloh a cache | `.agents/config/` | např. `review_config.yaml` |
| Nalezené chyby, backlog | `.agents/reports/` | `bugs.md`, `refactoring_backlog.md` |

**Nepoužívat pro sdílená pravidla:** `.cursor/`, `.claude/`, `.codex/` — jsou v `.gitignore`; konfigurace tam není dostupná pro celý tým ani v CI.

---

## MCP Servers (top 1–2) — *instalace u uživatele*

### context7
**Proč**: Živá dokumentace pro Angular, Spring, Solr, Saxon — projekt používá mnoho knihoven bez lokálního kontextu.  
**Instalace**: `claude mcp add context7` (nebo dle dokumentace Claude Code).  
**Sdílení**: Konfiguraci MCP lze verzovat v `.mcp.json` v kořeni repozitáře (pokud je podporováno), ne v uživatelském `.cursor/`.

### GitHub (plugin-github-github)
**Proč**: PR, issues, CI — projekt používá GitHub Actions a workflow z `CONTRIBUTING.md`.  
**Poznámka**: Pokud používáte `gh` CLI, MCP může doplnit operace s PR a checks.

---

## Skills — *relevance rechecked; použití zdokumentováno v AGENTS.md*

Skills were reviewed against their actual SKILL.md definitions and this repo’s context.

### gh-fix-ci ✅ Relevant
**What it does:** Debug/fix failing **GitHub Actions** PR checks: `gh` auth → inspect checks and logs → summarize → plan → implement after explicit approval. External CI (e.g. Buildkite) is out of scope.  
**Why here:** Repo uses GitHub Actions (`.github/workflows/`). Use when a PR has red checks; ensure `gh auth status` first.

### gh-address-comments ✅ Relevant
**What it does:** Address review/issue comments on the **open PR for the current branch**: `gh` + skill’s `fetch_comments.py` → list threads → user picks which to address → agent applies fixes. Requires `gh` auth.  
**Why here:** Aligns with CONTRIBUTING.md PR workflow and agent branches; use when incorporating reviewer feedback.

### doc ⚠️ Narrow scope — not for general “documentation”
**What it does:** Reading/creating/editing **`.docx` (Word)** files only; python-docx, `render_docx.py`, layout fidelity. Not for Markdown or Javadoc.  
**Relevance:** This repo has **no .docx** in tree; docs are Markdown (README, CONTRIBUTING, .agents) and Javadoc. **Omit** from routine recommendations; invoke `doc` only if the task explicitly involves Word deliverables (e.g. external reports).

---

## Hooks — *nahrazeno skriptem a dokumentací*

Doporučení pro hooky (např. PostToolUse: format/lint) závisí na tom, kde Claude Code ukládá konfiguraci.  
Pro **sdílené** chování bylo v repozitáři provedeno:

- **Aplikováno:** V `CONTRIBUTING.md` a `AGENTS.md` jsou popsány požadované kroky před PR a odkaz na skript `scripts/pre-pr-check.sh` / `scripts/pre-pr-check.ps1`.
- Skript ze kořene repozitáře spustí `mvn compile`, `mvn test` a `npm run build` (Angular).

Žádné konkrétní hooky neukládejte do `.cursor/` s očekáváním, že je uvidí celý tým.

---

## Subagents — *zdokumentováno v AGENTS.md*

### code-reviewer
**Proč**: Velký backend (Java, Solr, XSLT) a frontend (Angular) — paralelní review zlepší kvalitu.  
**Kde popsat**: ✅ V `AGENTS.md` v sekci „Recommended subagents“ — kdy spouštět a že pravidla review zůstávají v AGENTS.md a CONTRIBUTING.md.

### security-reviewer (volitelně)
**Proč**: Spring, autentizace, CORS — `.agents/` již obsahuje `security_analysis.json`.  
**Kde**: ✅ Odkaz v `AGENTS.md` (Recommended subagents) a na `.agents/reports/bugs.md`.

---

## Shrnutí

- **Pravidla a konvence**: `AGENTS.md`, `CONTRIBUTING.md`, `CLAUDE.md`, `.agents/` — vše verzované. ✅ Provedeno v repozitáři.
- **MCP / Skills / Subagents**: Používejte dle dokumentace nástroje; *dokumentaci kdy a jak je používat* máte v `AGENTS.md` (Recommended Skills, Recommended subagents). ✅ Doplněno.
- **Hooky**: Sdílené chování je pokryto skriptem `scripts/pre-pr-check.sh` / `.ps1` a odkazem v `CONTRIBUTING.md` a `AGENTS.md`. ✅ Aplikováno.

**Chcete víc?** Můžete požádat o další doporučení pro konkrétní kategorii (např. další MCP servery nebo skills).  
**MCP instalace:** context7 a GitHub MCP instalujte lokálně dle dokumentace Claude Code; konfiguraci lze verzovat v `.mcp.json`, pokud nástroj podporuje.
