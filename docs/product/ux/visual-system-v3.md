# Visual system v3: "Quiet Authority", enforced

Status: binding for all frontend work from 2026-11-03 (decided by the orchestrator with the user's mandate).
Goal: the app must read as one deliberate product for an executive-search firm: calm, dense, precise. Reference feel: Linear, Attio, Affinity. Not a template, not a dashboard kit.

Every rule below is a check in review. "Why" is given once so builders can decide edge cases the same way.

Sources this enforces (read the named sections only if a rule is unclear): `docs/product/brand/brand-direction.md` sections 4 (icons, one per object type), 6 (motion), 8 (dos and don'ts); `docs/product/research/critique-consultant-review.md` "What a partner would laugh at" and "Top 15 criticisms" (money and time over counts, consent as banners, board must show all stages). Where this file and brand-direction.md differ, this file wins.

## 0. The bar: mid-enterprise
The product is sold to mid-size firms (tens to a few hundred users, years of data). A screen passes only if it would survive an IT/procurement demo next to Salesforce, Bullhorn or Invenias. Concretely:
- **Scale:** every list is built for 10,000+ rows: server-side paging and sort, total count in the title, sticky header, saved views, select-all-matching for bulk actions. No screen assumes a short list.
- **Many users:** every record shows owner and "Last updated by <name>, <time>"; activity is attributed; history is one click away.
- **Permissions are visible (post-pilot; role work is postponed, do not build it now):** an action the user may not take is disabled with a tooltip saying why, never silently missing or failing.
- **Trust:** destructive actions name the record ("Archive Lale Yapi Holding?"); reversible actions offer Undo instead of a confirm; errors say what happened and what to do, with no stack traces or raw codes.
- **Keyboard and speed:** every primary flow can be finished with the keyboard; Ctrl+K reaches every record and action; Ctrl+Enter saves forms; Esc closes.
- **Accessibility:** WCAG 2.1 AA (contrast, focus, labels, 44 px touch targets on phone).
- **Tone:** sober and exact. No emoji, no exclamation marks, no cute empty-state copy, no marketing words in the UI.
- **Locale:** TR and EN, Turkish date/number formats in TR, no truncated Turkish labels.

## 1. Principles
1. **Content over chrome.** Borders, boxes, colour and badges are spent only where they carry meaning. Default is plain text on the surface.
2. **One way to do each thing.** One page header, one table, one toolbar, one badge style, one empty state. Screens differ in content, never in pattern.
3. **Hierarchy by weight and colour of text, not by boxes.** Ink for primary, ink2 for normal, ink3 for meta.
4. **Dense but breathable.** CRM users scan lists all day: 14 px body, 40 px rows, 24 px between sections.

## 2. Typography (Inter for UI; Source Serif 4 only where named)
| Role | Size / weight | Colour | Font |
| --- | --- | --- | --- |
| Page title (h1) | 24 / 600, line 1.25 | ink | Serif |
| Record name (client, person, search on its own page) | 24 / 600 | ink | Serif |
| Section title (h2) | 16 / 600 | ink | Inter |
| Body, table cells, inputs | 14 / 400 | ink2 (names: ink 500) | Inter |
| Secondary line in a cell, meta | 13 / 400 | ink3 | Inter |
| Field label, table header, small caption | 12 / 500 | ink3 | Inter |
| KPI number | 28 / 600 tabular | ink | Inter |

- Serif nowhere else: not in cards, columns, dialogs, tabs or nav.
- No uppercase text anywhere. No letter-spacing tricks. Sentence case for every label, button and title.
- Numbers, dates and counts use `font-variant-numeric: tabular-nums`.

## 3. Colour
- **Navy (`accent`) is the only action colour:** primary buttons, links, selected states, focus.
- **Brass is brand only:** logo mark and the active-nav indicator. Never a button, badge or text colour.
- Status colour lives only inside status badges and the overdue/stale text cue. Never colour whole cards, rows, column borders or headings.
- Canvas `bg` behind pages; `surface` (white) for panels and tables; `surface2` only for table headers and hover.

## 4. Surfaces and spacing
- Spacing scale only: 4, 8, 12, 16, 24, 32, 40. Page gutter 32 (desktop) / 16 (phone). 24 between sections, 12 inside a section, 8 between label and value.
- **Panel** = white, 1 px `border`, radius 8, no shadow. Shadow only for floating things (menu, popover, dialog, dragged card).
- **No panel inside a panel.** Inside a panel, separate groups with a 1 px divider or 24 px space.
- No coloured top borders, left stripes, gradients or decorative backgrounds.
- Radius: 4 for chips/badges/segmented controls, 6 for inputs/buttons, 8 for panels, 12 for dialogs. **Nothing pill-shaped or oval** except avatars. Filter options are square-cornered chips or a select, never rounded pills.
- **Few large panels, never loose text, never small cards.** (User feedback, both ways: "everything is in a card" was wrong, and "text spat on a blank page that can't be told apart" was also wrong.) Rules:
  - The beige canvas holds only: the page title/header band, the list toolbar, and panels. **Any other content (facts, lists, timelines, text) lives inside a white panel.**
  - A panel is a *region*, not a decoration: it has a **panel header** (h2 16/600 ink + optional count and right-aligned actions, 12 px vertical padding, 1 px divider below) and a body. Use the shared `Panel` component.
  - Inside a panel: rows separated by 1 px dividers, groups by a divider or 24 px. No panel inside a panel, no tinted boxes inside a panel.
  - Typical counts: list page = 1 panel (the table). Record page = 2 to 4 panels (see section 5). Home = KPI strip + 2 or 3 panels.
  - Not allowed: a card around a filter bar, each fact in its own box, a row of many small cards, or loose paragraphs on the canvas.
- **Label/value pairs** always use the shared `DetailList`: a two-column grid, label 13/400 ink3 in a fixed 140 px column, value 14/500 ink; rows 8 px apart; empty value shows "–" in ink3. Never inline "Label: value" sentences.

## 5. Page anatomy (every routed page)
```
[Breadcrumb 13 ink3]            (record pages only)
[Title serif 24]  [badge]                 [secondary] [secondary] [Primary]
[One-line description 14 ink3]            (list pages only, optional)
-- 24 --
[Toolbar row]                              (list pages)
-- 12 --
[Content]
```
- Use the shared `PageHeader` for every page. Max one primary button per page, max two secondary; more goes into a "More" (`...`) menu.
- **Record pages** (client, candidate, search) use the shared `RecordLayout`:
```
[Header band: white surface, full width, 1 px bottom border]
  breadcrumb / avatar or object icon 40 px / serif name + status badge / subtitle (title, company) / actions right
  fact strip: 4-6 key facts, label 12 ink3 above value 14/500 ink, separated by 1 px vertical dividers
  tabs (underline) sit at the bottom edge of the band
[Optional full-width banner: consent / off-limits problem]
[Body: two columns on >= 1200 px]
  main (2/3): the active tab's panels (e.g. "Activity", "Career", "Pipeline")
  aside (1/3): "Details" panel (DetailList of all fields + "Last updated by"), "Next actions" panel
  phone: one column, aside below main
```
- Section titles inside the body are panel headers, never free-floating h2 on the canvas.

## 6. Components
- **Icons:** `@tabler/icons-react` only, outline, stroke 1.75, 16 px in buttons/menus/tables, 18 px in navigation. No hand-drawn SVG, no text glyphs ("+", ">", "x") as icons, no emoji. One icon per object type everywhere, exported from `shell/components/objectIcons.ts`: Client `IconBuilding`, Contact `IconIdBadge2`, Search `IconTargetArrow`, Candidate `IconUserStar`, Person `IconUser`, Activity `IconMessages`, Task `IconCheckbox`, Consent `IconShieldCheck`, Off-limits `IconHandStop`, Exposure `IconEye`, Fee `IconCoins`, Settings `IconSettings`, Home `IconHome`.
- **Consent and off-limits problems:** never a colour chip alone. On cards and rows: tone badge with icon and words ("Consent refused"). On record pages: a full-width danger/warning banner under the header with the action ("Send consent request").
- **Board:** all stages visible at 1280 px without horizontal scroll; empty stages narrow.
- **Home KPIs:** lead with time and progress for a search firm (open searches, shortlists due, average days open, placements this year), not raw record counts.
- **Buttons:** height 32 (sm) in toolbars and headers, 36 in forms/dialogs. Primary = filled navy. Secondary = `default` (white, border). Tertiary = `subtle`. Leading icon only for create (plus) and export/import. Labels are verbs: "New client", "Log activity", not "+ New".
- **Badges (status):** one style everywhere via `StatusBadge`/`ToneBadge`: tone soft background, tone text, 12 / 500, height 20, radius 4, padding 0 6, **no dot**. One status badge per row or card.
- **Tables (`shell/grid`):** header 12 / 500 ink3 on `surface2`, no vertical borders, 1 px row dividers, row height 40 (compact 32), whole row clickable with pointer cursor and `surface2` hover, first column ink 500, numbers right-aligned tabular, no chevrons or underlines in cells, row actions appear on hover at the right end.
- **Toolbar (list pages):** one row, 32 px controls: search (grows) - scope toggle (All/Mine) - "Filters" button with count - spacer - view controls (density, columns, export) as icon buttons with tooltips. Active filters show under it as removable chips. Advanced filters live in a popover or collapsible panel, never always open.
- **Tabs:** underline style, 14 / 500, count in ink3 after the label.
- **Timeline:** grouped by day ("Today", "Yesterday", "12 Oct 2026") with 12 / 500 ink3 group labels; each entry: 16 px type icon in ink3, one line summary in ink2, author and time 13 ink3.
- **KPI tiles:** label 12 ink3 above number 28; plain panel; optional delta line 13; no accent borders.
- **Empty state:** one sentence 14 ink3 + optional one button, centred in the area, no illustration.
- **Loading:** skeleton shaped like the content; never a lone spinner on a page.
- **Header bar:** logo - global search (grows) - "New" (default/secondary style, plus icon) - avatar menu. The TR/EN switch moves into the avatar menu.

## 6b. Create and edit forms (New menu, quick add, edit drawers)
User verdict on the current ones: "like a text editor". Forms must feel like a fast tool, not a document.
- **Ask for the minimum first.** A create form shows only what is needed to create the record (person: name, title, company, email; client: name, sector, city). Everything else sits under a "More details" disclosure, closed by default, or is added later on the record page.
- **Context before content.** Pick *who/what* first, then *what happened*, then *when*. Quick add order: type (icon toggle row) - client/candidate (one combined search picker with object icons) - note - when.
- **Layout:** modal width 560 px (`size="lg"`), never scrolls for the minimum fields. Two-column grid (`SimpleGrid cols={{ base: 1, sm: 2 }}`) for short fields; long fields (note, address) span both. Inputs in a row align on their top edge: no description text under one label and not the other.
- **Labels, not paragraphs.** Field label only (12/500 ink3 per spec). No description line under labels; put a short hint in the placeholder ("e.g. Finance Director") or a tooltip icon. One line of required-field legend is removed: mark required with the asterisk only.
- **Section labels** inside a form: 13/600 ink2 sentence case ("Contact details"), 24 px above, no uppercase, no divider boxes.
- **Smart defaults:** owner = me, date = now, type = the most common one. Date/time: quick picks as chips ("Now", "Today", "Yesterday"; follow-up: "Tomorrow", "Next week", "Pick date") before the date input.
- **Footer:** sticky, right-aligned: secondary "Cancel", primary verb with object ("Create candidate", "Save changes", "Log call"). Keyboard hint (Ctrl+Enter) as 12 ink3 text on the left. Primary is disabled only with a reason shown next to it.
- **After create:** toast with "Open" link; never leave the user on an empty state.
- **Edit drawers:** same rules; width 640 px; sections as above; sticky footer.

## 7. Motion and focus
- 120 ms ease-out for hover/colour; 180 ms for panels. Respect `prefers-reduced-motion`.
- Visible focus ring (`focus` colour, 2 px) on every interactive element.

## 8. Review checklist (orchestrator gate, per screen)
- [ ] Uses PageHeader; one primary action; no uppercase; serif only in title/record name
- [ ] No panel in panel; no coloured borders/stripes; brass not used for actions
- [ ] Tabler icons only; no "+" or ">" glyphs
- [ ] Table/toolbar/badge/empty/loading follow section 6
- [ ] 1440 px and 375 px: no overflow, no horizontal page scroll
- [ ] Looks like the same product as the clients list
- [ ] Section 0 enterprise bar: scale, owner/last-updated, disabled-with-reason, named destructive actions, keyboard path, AA
- [ ] Gate tools: `design-skills:accessibility-audit` and `design-skills:design-critique` on each screen before sign-off
