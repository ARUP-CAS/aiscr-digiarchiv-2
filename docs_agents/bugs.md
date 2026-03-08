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
