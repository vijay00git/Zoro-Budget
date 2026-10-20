# Colourful bento redesign

Approved 2026-10-20 from the mockup at https://claude.ai/artifact/2Xk8GXz13xWfrjjJhQSDCV.

## Decisions

- **Theme:** follows the phone's light/dark setting by default. A theme saved before the redesign is switched to System once; a theme chosen after that is kept.
- **Bento grids:** only on dashboard pages (Overview, the Savings summary, Planning). The Ledger stays a list.
- **Colour:** one accent per account, category and savings section, each with a soft tint, a strong readable shade (at least 4.5:1 contrast) and a solid shade for badges, in light and dark versions. Red is kept for warnings. Money in is green with a +; money out uses the normal text colour.
- **Logos:** no bundled bank logos. Account badges use initials on the account's colour.

## Done (2026-10-20)

**R1 foundations**
- `ui/theme/Accents.kt` works out the tint and readable shades from any stored colour (`accentOf`, `LocalIsDarkTheme`).
- About 60 category icons in 7 groups (`CategoryIconGroups`).
- Name-based icon guesses, so the 13 custom categories that still have the plain tag icon get a fitting one: Non-Veg, Vegetable, Ulundai, snacks, Rent EB water, Recharges, Wife Expenses, Rapido, Cosmetics, Given to, EMI. Person names such as "Vijay" keep the plain icon.
- `AppearancePicker` (colour and icon) in the Add category dialog and in the category edit dialog ("Change icon and colour").
- `AccountBadge` (initials, or a wallet or card icon) and `CategoryAvatar` (category icon with an account dot).
- Ledger rows show the category avatar. Money-out amounts are no longer red.
- The one-time theme switch to System.

**R2 Overview bento**
- `ui/components/BentoTiles.kt`, in this order:
  - indigo balance hero with Add and Transfer;
  - account tiles, two to a row (tapping one filters to that account), plus an Add account tile;
  - the low-balance card;
  - the bills-due card, only while a bill needs paying;
  - month selector;
  - a spending ring tile beside "All reviewed / SMS to review" and next-bill tiles;
  - a Recent tile.
- The older sections (budgets, health score, charts, heatmap) follow below.
- The selected tab is an indigo pill.

**R3 Savings bento**
- Indigo summary hero with a colour bar showing the mix by section.
- Empty sections collapse into a two-column "Start tracking" grid of tinted tiles.

**Checks**
- Rendered in light and dark through Robolectric/Roborazzi.
- 88 unit tests pass, and ktlint is clean.
- Not yet checked on a device: the Elo disconnected during this work.

**Later the same day, at the user's request**
- **Overview:**
  - Balances hidden behind an eye button, and account tiles opened from the balance card (both reset when the app restarts).
  - A compact balance card with Add and Transfer on the right.
  - A row of three tiles: low balance (with a pop-up for Add money), SMS review, and health score.
  - A Bills tile with a countdown to the next bill.
  - The Recent tile and the planned/spent summary removed.
- **Savings:** Gold and Silver weight and price are optional, with a new pieces field. Land size, price and value are optional. Gold, silver and land are left out of the total value and net worth.
- **R4 Planning bento** (`ui/components/PlanningTiles.kt`, `ReportsScreen.kt` rewritten):
  - a cash-flow hero tile;
  - a budget ring tile (tapping it sets the total budget) and a savings goal tile (tapping it edits the target);
  - Over budget / Near limit / No limit count tiles that filter the grid;
  - categories as two-column coloured tiles, problems first, plus a New category tile;
  - then bills and charts.
  - The section tabs are gone.

## Still to do

- **R4 polish:** fix the remaining dark-mode colours in the older Overview cards (the "Spent/Left" figures in Spending by category); Planning screen tiles; an account colour picker in the Add/Edit account dialog; a photo option for account badges; a new app icon.
- **Device check** on the Elo, then on the phone (backup first; the phone can now be updated with `adb install -r`).
- **Then:** savings step 4 (charts for each section) and step 5 (attachments).
