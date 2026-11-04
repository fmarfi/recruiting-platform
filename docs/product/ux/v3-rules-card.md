# v3 rules card (agents read this, not the full spec)
Full spec: visual-system-v3.md (orchestrator). Bar: mid-enterprise, sober, consistent.

Layout
- Beige canvas holds only: page header / record header band, list toolbar, white panels. No loose text on the canvas.
- Content lives in `Panel` (shell/components): title bar + divider, rows with 1 px dividers. No panel in a panel, no small cards, no tinted boxes.
- Label/value pairs: `DetailList` only. Never "Label: value" sentences.
- Record pages: `RecordLayout` + `RecordHeader` (band with name, badge, subtitle, fact strip, tabs) ; main = panels, aside = "Details" (DetailList) + "Next actions". Reference implementation: frontend/src/network/person/ (PersonPage, PersonHeader, PersonAside, OverviewTab).
- List pages: `PageHeader`, toolbar row on canvas, one table (DataGrid with `onRowClick`).

Look
- Inter 14 body; serif only for page title and record name. No uppercase anywhere. Labels 12-13 ink3, values 14 ink.
- Navy = actions. Brass = brand only. Status colour only inside badges (`StatusBadge`/`ToneBadge`, square, no dot).
- Radius 4 chips/badges, 6 inputs/buttons, 8 panels. Nothing oval except avatars.
- Icons: `@tabler/icons-react`, stroke 1.75, object icons from shell/components/objectIcons.ts. No "+", ">" glyphs.
- One primary button per page; extras in a More (IconDots) menu.
- Numbers on screens are days, money or people, never record counts.
- Tokens only: Mantine props, var(--space-N), var(--color-...), var(--tone-...). No hex, no raw px.

Cheap working (token budget)
- Read only the files named in your task; use grep and line ranges for files over 200 lines.
- i18n: do not read whole en.json/tr.json. Find the object with grep -n, then Edit to append keys before its closing brace.
- Test output: always `| tail -5`.
