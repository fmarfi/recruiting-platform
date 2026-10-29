# Brand direction

Owner: brand and product design · Date: 2026-10-29 · Status: PROPOSAL for the user's decision · Companion files: `design-tokens-v2.json` (tokens), `logo.svg` (monogram and wordmark), `app-architecture.md` (information architecture and build plan).

Scope: the product brand of the platform the firm's partners and consultants work in every day, in Turkey and internationally. It is not the firm's own brand. The client-facing reports (shortlists, candidate profiles) carry the **firm's** logo, with the product mark only as a small "Prepared with" line, which the firm can switch off.

## 1. Recommended direction: "Quiet Authority"

**Idea.** A retained-search partner's desk: ink, good paper, one brass detail. The product should feel like a trusted adviser's private study, not a sales CRM. Calm, exact and discreet. Its confidence comes from order and restraint, not decoration.

**Positioning line.**
- EN: *"The mastermind of the search. Every mandate, candidate and relationship, under control."*
- TR: *"Aramanın beyni. Her görev, her aday, her ilişki kontrol altında."*

Short tagline for the login screen and the logo lock-up: **EN "Executive search, proven."** / **TR "Yönetici araması, sınanmış."**

**Personality words:** discreet, exact, composed, senior, bilingual, trustworthy. **Not:** playful, loud, techy, salesy, cute.

**Why this direction.** The critiques say the app reads as "a re-skinned sales tool" (generic vocabulary, counts instead of fees, a Kanban as the centrepiece). A premium brand alone will not fix that. The brand has to set rules for the product as well: search-firm vocabulary, money and time as the headline numbers, and the compliance spine (consent, off-limits) shown with pride rather than hidden in Settings. Quiet Authority keeps the existing navy identity, which is cheap to migrate, and adds warmth (ivory paper) and a single brass accent that signals seniority.

### 1.1 Product name: three candidates

No trademark searches were possible in this session, so every name below is **unchecked**. Before any public use, run searches at TÜRKPATENT (Turkey), EUIPO and WIPO Global Brand Database in Nice classes 9 (software), 35 (recruitment and business services) and 42 (SaaS), plus a domain and app-store check. No real client's name is used or referenced.

| Name | Meaning and fit | Trademark risk (unverified estimate) |
| --- | --- | --- |
| **Mihenk** (recommended) | Turkish *mihenk taşı* = touchstone, the stone that tests the purity of gold by the streak it leaves. That is exactly what executive search does: it tests people. Short, easy to say in EN ("mee-HENK"), rare in software. | **Medium-low.** It is a real Turkish word, so others may use it in unrelated classes (jewellery, consulting, training). A stylised mark in classes 9/35/42 is likely registrable. Confirm before you commit. |
| **Pusula** | Turkish for compass: guidance and direction. Instantly understood in Turkey. | **High.** A very common Turkish brand word (media, finance, apps). It will probably clash and needs a qualifier ("Pusula Search"). |
| **Northline** | An English coined compound: a clear line to the right person. Neutral internationally. | **Medium.** Compounds with "North" are common in finance and logistics, and so are "Northline" companies. Needs a full search. |

**Decision: Mihenk.** It carries a story a partner can tell a client in one sentence, it is Turkish without being parochial, and the touchstone streak gives a natural logo.

### 1.2 Logo concept

- **Monogram:** a dark midnight square with softened corners (the touchstone). Inside it, a geometric **M** drawn with an even stroke and sharp joins. Under the M, a single short **brass streak** at a slight angle: the mark that gold leaves on a touchstone. It is the only brass on the mark.
- **Wordmark:** "Mihenk" set in Source Serif 4 SemiBold, tracked +0.5 %, in midnight. The optional tagline is set in Inter SemiBold, uppercase, tracked +18 %, in ink-3.
- **Lock-ups:** horizontal (monogram + wordmark) for the header and login; monogram alone for the favicon, the app icon and the collapsed navigation; wordmark alone for the report footers.
- **Clear space:** the height of the brass streak × 3 on all sides. **Minimum size:** monogram 16 px (favicon; at that size the streak becomes 2 px), horizontal lock-up 120 px wide.
- **On dark (header):** a white M on the midnight square with a 1 px `#B9C3D6` outline; the wordmark in white.
- File: `docs/product/brand/logo.svg` (a vector draft; the wordmark uses live text, so outline it in a vector editor before it ships).

## 2. Colour

Principle: **neutrals carry the interface, midnight carries action, brass is jewellery.** Brass appears at most once per screen in a decorative role (logo streak, active-nav marker, the primary KPI rule, the "fee-weighted" headline). As text, use only the dark `brass` value. Colour never carries meaning alone: every status has a dot and a label, and every alert has an icon and words.

### 2.1 Palette

| Role | Token | Hex | Use |
| --- | --- | --- | --- |
| Primary (Midnight) | `accent` | `#14233D` | Primary buttons, links, selected states, header chrome |
| Primary hover | `accentHover` | `#0B1628` | Hover and pressed state of primary |
| Primary soft | `accentSoft` | `#E8ECF3` | Selected row, light button, active tab background |
| Header chrome | `chrome` | `#14233D` | Global header and left rail background |
| Chrome active | `chromeActive` | `#24385E` | Active nav item on chrome |
| Chrome text | `chromeInk` / `chromeInk2` | `#FFFFFF` / `#B9C3D6` | Text on chrome |
| Accent (Brass, text-safe) | `brass` | `#8A5F1A` | Brass text: "fee-weighted" numbers, premium labels |
| Accent (Brass, decorative) | `brassDeco` | `#C49A4E` | Streak, active-nav marker, KPI rule. Never as text on light backgrounds |
| Brass soft | `brassSoft` | `#F5EEDF` | Highlight band behind the headline KPI |
| Background (Ivory) | `bg` | `#F7F5F0` | App canvas |
| Surface | `surface` | `#FFFFFF` | Cards, tables, panels |
| Surface 2 | `surface2` | `#F2EFE8` | Table header, hover row, inset areas |
| Surface 3 | `surface3` | `#E8E4DA` | Pressed, skeleton base |
| Border | `border` | `#DDD8CC` | Dividers and card edges (decorative, see note) |
| Border strong | `borderStrong` | `#857F72` | Input borders, checkboxes, any border that identifies a control |
| Ink | `ink` | `#16181D` | Primary text |
| Ink 2 | `ink2` | `#3A3F48` | Secondary text, labels |
| Ink 3 | `ink3` | `#5B616C` | Captions, placeholders, metadata |
| Focus | `focus` | `#1F5FD1` | Focus ring on light |
| Focus on chrome | `focusOnChrome` | `#7FA6F0` | Focus ring on the midnight header |
| Danger | `danger` / `dangerSoft` | `#B42318` / `#FCEBE8` | Destructive actions, blocking errors |
| Success | `success` | `#1E6B45` | Success text and icons |
| Warning | `warning` | `#8A5A00` | Warning text and icons |
| Info | `info` | `#1D4F91` | Informational text and icons |

**Status tones** (the keys of `Tone.java`; the frontends keep mapping statuses to tones at runtime from `/reference-data`):

| Tone | bg | fg (text) | dot | Typical statuses |
| --- | --- | --- | --- | --- |
| NEUTRAL | `#EEEBE4` | `#3A3F48` | `#7D7869` | Research, Not sent, Draft |
| INFO | `#E5EDF9` | `#1D4F91` | `#3B78C0` | Contacted, Sent, Lead |
| PROGRESS | `#ECEAF7` | `#40368A` | `#6A5CC8` | Longlist, Shortlist, Client interview |
| GOOD | `#DFF0E5` | `#17573A` | `#2F8F5A` | Placed, Consent given, Won, Active |
| WARN | `#FAEFD6` | `#744A00` | `#A8740C` | On hold, Retention ending, Consent pending |
| BAD | `#FBE3DF` | `#9A2C25` | `#CF4A40` | Rejected, Consent refused/withdrawn, Off-limits, Lost |
| MUTED | `#F1EFEA` | `#555B66` | `#8C877B` | Closed, Withdrew, Anonymised |

### 2.2 Contrast table (computed, WCAG 2.x relative luminance)

The ratios were computed with the WCAG formula (sRGB linearisation, (L1 + 0.05) / (L2 + 0.05)). Thresholds: normal text 4.5:1, large text (24 px, or 18.66 px bold) 3:1, UI components and graphics 3:1 (SC 1.4.11).

| Foreground | Background | Ratio | Passes for |
| --- | --- | --- | --- |
| ink `#16181D` | surface `#FFFFFF` | 17.76 | AAA text |
| ink `#16181D` | bg `#F7F5F0` | 16.30 | AAA text |
| ink2 `#3A3F48` | surface `#FFFFFF` | 10.58 | AAA text |
| ink2 `#3A3F48` | bg `#F7F5F0` | 9.71 | AAA text |
| ink2 `#3A3F48` | surface3 `#E8E4DA` | 8.34 | AAA text |
| ink3 `#5B616C` | surface `#FFFFFF` | 6.23 | AA text |
| ink3 `#5B616C` | bg `#F7F5F0` | 5.72 | AA text |
| ink3 `#5B616C` | surface2 `#F2EFE8` | 5.42 | AA text |
| ink3 `#5B616C` | surface3 `#E8E4DA` | 4.91 | AA text |
| accent `#14233D` | surface `#FFFFFF` | 15.70 | AAA text (links) |
| accent `#14233D` | bg `#F7F5F0` | 14.41 | AAA text |
| accent `#14233D` | accentSoft `#E8ECF3` | 13.25 | AAA text (selected tab, light button) |
| accent `#14233D` | brassSoft `#F5EEDF` | 13.59 | AAA text (headline KPI band) |
| surface `#FFFFFF` | accent `#14233D` | 15.70 | AAA text (primary button) |
| chromeInk `#FFFFFF` | chromeActive `#24385E` | 11.65 | AAA text (active nav) |
| chromeInk2 `#B9C3D6` | chrome `#14233D` | 8.85 | AAA text (inactive nav) |
| brassDeco `#C49A4E` | chrome `#14233D` | 6.04 | AA text: brass is allowed as text **only on midnight** |
| brassDeco `#C49A4E` | chromeActive `#24385E` | 4.48 | graphics only (active marker); not for text |
| focusOnChrome `#7FA6F0` | chrome `#14233D` | 6.44 | focus ring ≥ 3:1 |
| brass `#8A5F1A` | surface `#FFFFFF` | 5.63 | AA text |
| brass `#8A5F1A` | bg `#F7F5F0` | 5.16 | AA text |
| brass `#8A5F1A` | surface2 `#F2EFE8` | 4.90 | AA text |
| brass `#8A5F1A` | brassSoft `#F5EEDF` | 4.87 | AA text |
| surface `#FFFFFF` | brass `#8A5F1A` | 5.63 | AA text (rare brass button, e.g. "Send shortlist" in reports) |
| danger `#B42318` | surface `#FFFFFF` | 6.57 | AA text |
| danger `#B42318` | dangerSoft `#FCEBE8` | 5.69 | AA text (error banner) |
| surface `#FFFFFF` | danger `#B42318` | 6.57 | AA text (destructive button) |
| success `#1E6B45` | surface `#FFFFFF` | 6.47 | AA text |
| warning `#8A5A00` | surface `#FFFFFF` | 5.93 | AA text |
| info `#1D4F91` | surface `#FFFFFF` | 8.14 | AAA text |
| focus `#1F5FD1` | surface `#FFFFFF` | 5.81 | focus ring ≥ 3:1 |
| focus `#1F5FD1` | bg `#F7F5F0` | 5.33 | focus ring ≥ 3:1 |
| focus `#1F5FD1` | accentSoft `#E8ECF3` | 4.90 | focus ring on selected row |
| focus `#1F5FD1` | surface2 `#F2EFE8` | 5.06 | focus ring on hover row |
| borderStrong `#857F72` | surface `#FFFFFF` | 3.98 | UI component boundary (inputs, checkboxes) |
| borderStrong `#857F72` | bg `#F7F5F0` | 3.65 | UI component boundary |
| border `#DDD8CC` | surface `#FFFFFF` | 1.42 | **Decorative only**: card edges and dividers never identify a control |
| brassDeco `#C49A4E` | surface `#FFFFFF` | 2.60 | **Decorative only**: never text, never the only cue |

Status tones (chip text on its own tint, and on white for the plain-text use):

| Tone | fg on bg | fg on white | dot on bg | dot on white |
| --- | --- | --- | --- | --- |
| NEUTRAL | 8.89 | 10.58 | 3.70 | 4.41 |
| INFO | 6.91 | 8.14 | 3.84 | 4.53 |
| PROGRESS | 8.37 | 9.94 | 4.44 | 5.28 |
| GOOD | 7.21 | 8.53 | 3.41 | 4.04 |
| WARN | 6.75 | 7.71 | 3.56 | 4.06 |
| BAD | 6.21 | 7.61 | 3.64 | 4.46 |
| MUTED | 5.94 | 6.83 | 3.12 | 3.58 |

Every chip's text passes AA (≥ 4.5), and every dot passes 3:1 as a graphic. The WARN and MUTED dots were darkened from the v1.1 values (`#C98A12` gave 2.58 and `#A3ACB9`-style greys gave 2.32) to pass.

### 2.3 Dark mode

Not in Phase 1 (v1.1 removed it, and the users work in bright offices and print to PDF). The `dark` keys mirror `light` so the token shape stays stable. Revisit after the pilot.

## 3. Typography (free Google Fonts, latin-ext subset for Ğ Ş İ ı Ç Ö Ü)

| Role | Family | Use |
| --- | --- | --- |
| Display serif | **Source Serif 4** (wght 600, opsz auto) | Page titles (h1), the record name in the record header, the headline KPI number, report and PDF headings, the wordmark. Never in tables, forms or buttons. |
| UI sans | **Inter** (400, 500, 600; tabular numbers `tnum`) | Everything else. Already loaded today. |
| Mono | **IBM Plex Mono** (400) | IDs, audit hashes, API keys only |

Scale (rem at a 16 px root):

| Token | Size / line | Weight | Family | Use |
| --- | --- | --- | --- | --- |
| display | 2.25rem (36) / 1.15 | 600 | serif | Command centre greeting, login |
| h1 | 1.75rem (28) / 1.2; phone 1.5rem | 600 | serif | Page and record title |
| h2 | 1.25rem (20) / 1.3 | 600 | sans | Card and section titles |
| h3 | 1.0625rem (17) / 1.35 | 600 | sans | Sub-sections, dialog titles |
| body | 1rem (16) / 1.5 | 400 | sans | Text, inputs |
| table | 0.9375rem (15) / 1.4 | 400 | sans | Grid cells (compact density 0.875rem) |
| small | 0.875rem (14) / 1.45 | 500 | sans | Labels, chips (md) |
| caption | 0.8125rem (13) / 1.45 | 400 | sans | Metadata, timestamps; never below 13 px |
| eyebrow | 0.75rem (12) / 1.3, uppercase, +0.06em | 600 | sans | Card eyebrows only; 12 px is allowed here because it is short and uppercase |
| kpi | 2rem (32) / 1.1, `tnum` | 600 | serif | KPI values |

Rules: maximum 70 characters per line in prose; numbers right-aligned with tabular figures; currency as `₺ 1.250.000` in TR and `TRY 1,250,000` in EN, following the locale; no italics in the UI except for quoted client words.

## 4. Iconography

- **Set:** Tabler Icons (MIT, already common with Mantine), **outline only**, 1.75 px stroke, 20 px in UI (16 px in dense tables, 24 px in empty states). No filled icons except the status dot and the checked checkbox.
- **One icon per object type**, used everywhere (nav, record header tile, search results, side panel): Client `building`, Contact `id-badge-2`, Search/assignment `target-arrow`, Candidate `user-star`, Person (generic) `user`, Opportunity `briefcase`, Proposal `file-certificate`, Report `file-description`, Activity `messages`, Task `checkbox`, Consent `shield-check`, Off-limits `hand-stop`, Exposure `eye`, Fee/revenue `coins`, Insights `chart-dots-3`, Admin `settings`.
- Icons are labelled: decorative icons get `aria-hidden`, icon-only buttons get `aria-label` and a tooltip, and status icons always sit next to text.
- No third-party logos or brand icons in the product.

## 5. Imagery and illustration

- **No stock photos** of handshakes, office towers or smiling teams. **No 3D blobs or mascots.**
- **People:** initials avatars only (midnight text on `accentSoft`, or a stable tone per person). Real photos only when uploaded by the firm with consent, and **never** in blind reports.
- **Illustration** for empty states and onboarding: fine single-weight line drawings (1.5 px, ink-3) of desk objects: a touchstone, a fountain pen, a folder, a compass, a magnifier. A brass accent is allowed on one element. Maximum 120 px tall. Delivered as inline SVG that inherits `currentColor`.
- **Charts:** neutrals for context and midnight for the measured series. Brass is reserved for the one fee-weighted figure. Values are printed on the chart (no tooltip-only data), with a table alternative one click away.
- **Reports and PDFs:** ivory paper `#F7F5F0` is used only on screen previews; print on white with the firm's logo, Source Serif headings, Inter body, a thin brass rule under the title, and a "Confidential" footer with page numbers.

## 6. Motion

- **Purpose only:** to show where something came from or went (a side panel slides in from the right, a toast rises, a row that moves stage fades and settles). No decorative loops, no parallax, no bouncing.
- **Durations:** `fast` 120 ms (hover, press, chip toggles), `base` 200 ms (panels, menus, dialogs), `slow` 320 ms (page-level transitions, at most once per navigation). **Easing:** `cubic-bezier(0.2, 0, 0, 1)` for entering and `cubic-bezier(0.4, 0, 1, 1)` for exiting.
- **Distance:** maximum 16 px of translation for menus and toasts; side panels slide their own width.
- **Reduced motion** (`prefers-reduced-motion: reduce`): all translations become opacity-only fades of 0 to 120 ms, skeleton shimmer becomes static, and drag ghosts never animate. This is already enforced globally in `tokens.ts`; keep it.
- **Never animate** numbers counting up, or anything that delays input. Optimistic updates appear immediately, with motion only as confirmation.

## 7. Tone of voice

**Voice:** a senior colleague: brief, specific, calm, never cute. Write in the second person ("you"), in the active voice, with the verb first on buttons. Say what happened, why it matters and what to do next. Use the vocabulary of executive search, never of sales CRM.

**Glossary (binding for EN/TR copy, from the consultant critique):**

| Concept | EN | TR | Do not use |
| --- | --- | --- | --- |
| The firm's customer | Client | Müşteri | Account (in the UI), Customer |
| A retained engagement | Search (record: Assignment) | Arama / Görev | Job order, İş emri (in the UI) |
| A won mandate | Mandate | Görev | Deal |
| Early list | Longlist | Uzun liste | Sourced, Bulundu |
| Presented list | Shortlist | Kısa liste | Presented, Müşteriye sunuldu |
| Research stage | Research | Araştırma | Sourcing |
| People database | Candidates and contacts | Adaylar ve kişiler | Network, Ağ |
| Commercial offer | Proposal (in Business development only) | Teklif | Proposal on a client tab |
| Do-not-approach | Off-limits | Yasaklı şirket | Blacklist |
| Shown to client | Exposure | Sunum geçmişi | Submission |
| KVKK explicit consent | Consent | Açık rıza | GDPR opt-in |
| Information notice | Privacy notice | Aydınlatma metni | |

### 7.1 Microcopy: 10 examples in English

| # | Kind | Copy |
| --- | --- | --- |
| 1 | Empty state (Searches) | **No live searches yet.** Win a mandate in Business development, or start a search from a client. · Button: **Start a search** |
| 2 | Empty state (Longlist) | **The longlist is empty.** Add people from Candidates, or paste names to research. · Button: **Add candidates** |
| 3 | Empty state (Today) | **Nothing due today.** Your next deadline is Thursday: shortlist for the CFO search. |
| 4 | Error (consent gate) | **Can't share this candidate yet.** We have no explicit consent on record. · Button: **Resend consent request** |
| 5 | Error (off-limits) | **Off-limits: Example Holding.** This company is protected until 30 Jun 2027. A partner can override with a reason. · Button: **Request override** |
| 6 | Error (network) | **We couldn't reach the server.** Nothing you typed is lost. We'll retry in 10 seconds. · Button: **Retry now** |
| 7 | Error (version conflict) | **Someone else updated this record while you were editing.** Review their changes, then save yours. · Button: **Compare and save** |
| 8 | Success (with undo) | **Call logged with Ayşe Demir.** Follow-up set for Monday. · Button: **Undo** |
| 9 | Success (bulk) | **3 candidates added to the longlist.** 1 skipped: already on this search. · Button: **Undo** |
| 10 | Buttons | **Send shortlist** · **Log a call** · **Request consent** · **Add to search** · **Mark as placed** |

### 7.2 Microcopy: 10 examples in Turkish

| # | Tür | Metin |
| --- | --- | --- |
| 1 | Boş durum (Aramalar) | **Henüz aktif arama yok.** İş geliştirmede bir görev kazanın ya da bir müşteriden arama başlatın. · Buton: **Arama başlat** |
| 2 | Boş durum (Uzun liste) | **Uzun liste boş.** Adaylardan kişi ekleyin ya da araştırılacak isimleri yapıştırın. · Buton: **Aday ekle** |
| 3 | Boş durum (Bugün) | **Bugün için bekleyen iş yok.** Sıradaki teslim perşembe: CFO araması için kısa liste. |
| 4 | Hata (rıza) | **Bu aday henüz paylaşılamaz.** Kayıtlı bir açık rıza yok. · Buton: **Rıza talebini yeniden gönder** |
| 5 | Hata (yasaklı şirket) | **Yasaklı şirket: Örnek Holding.** Bu şirket 30 Haziran 2027'ye kadar korunuyor. Bir ortak, gerekçe yazarak istisna tanıyabilir. · Buton: **İstisna iste** |
| 6 | Hata (bağlantı) | **Sunucuya ulaşamadık.** Yazdıklarınız kaybolmadı. 10 saniye içinde yeniden deneyeceğiz. · Buton: **Şimdi dene** |
| 7 | Hata (sürüm çakışması) | **Siz düzenlerken bu kaydı başka biri güncelledi.** Onun değişikliklerine bakın, sonra kendinizinkini kaydedin. · Buton: **Karşılaştır ve kaydet** |
| 8 | Başarı (geri al) | **Ayşe Demir ile görüşme kaydedildi.** Takip pazartesiye ayarlandı. · Buton: **Geri al** |
| 9 | Başarı (toplu) | **3 aday uzun listeye eklendi.** 1 aday atlandı: zaten bu aramada. · Buton: **Geri al** |
| 10 | Butonlar | **Kısa listeyi gönder** · **Görüşme kaydet** · **Rıza iste** · **Aramaya ekle** · **Yerleşti olarak işaretle** |

Turkish rules: formal *siz* throughout; correct the dotted and dotless i in upper case (`İ`, `I`) with `toLocaleUpperCase('tr')`; dates as `30 Haz 2027`; never show the English fallback in a Turkish UI (the CI key test already guards this).

## 8. Dos and don'ts

| Do | Don't |
| --- | --- |
| Lead every home and list screen with money and time (fee-weighted pipeline, days to shortlist) | Headline with counts ("Active clients 9") |
| Use the glossary: Client, Search, Longlist, Shortlist, Candidates | Mix "Account" and "Client", or say "Network", "Sourced", "Job order" in the UI |
| Keep brass to one decorative use per screen, and as text only in the dark `brass` value or on midnight | Use brass for buttons, borders, large fills or body text |
| Show consent and off-limits as clear, first-class banners with an action | Signal a refused consent with a colour chip alone |
| Pair every colour with a label and an icon | Rely on red/green alone |
| Use Source Serif only for titles, names and the headline number | Set tables, forms or buttons in serif |
| Write the verb first: "Send shortlist" | Write "OK", "Submit", "Yes" |
| Offer Undo after reversible actions | Ask "Are you sure?" for reversible actions |
| Use skeletons that match the final layout | Use full-page spinners |
| Use initials avatars, and no photos in blind reports | Use stock photography or mascots |
| Seed believable synthetic data (Turkish and international names, real-looking titles) | Show junk demo records ("asdas") or English-only fake names in a Turkish pitch |

## 9. Two short alternatives

**B. "Bosphorus Modern".** Deep teal `#0E4F5C` with a warm coral accent `#C2553A` on cool white. Sans-only (Manrope for headings, Inter for UI). It is more contemporary and international, and it suits a firm that sells speed and tech-savvy. Risks: it reads younger and more "startup", it moves away from the existing navy (more migration), and coral sits close to the danger red, so it needs careful semantics. Name: **Northline**.

**C. "Partner's Desk".** Warm graphite `#2B2A28` with an oxblood accent `#7A2632` on parchment `#FAF7F2`. Serif-heavy (Fraunces for headings, Inter for UI). It is the most "old-firm" and luxurious, and works well for boutique retained search with very senior partners. Risks: oxblood conflicts with the BAD/danger tone, and the dense serif hurts the readability of data-heavy screens. It is the weakest choice for consultants who live in grids all day. Name: **Mihenk** also fits.

**Why A wins:** it is closest to the existing tokens (lowest migration cost), its semantics are cleanest (the brass accent cannot be confused with any status), it is readable all day in grids, and it is premium through restraint.
