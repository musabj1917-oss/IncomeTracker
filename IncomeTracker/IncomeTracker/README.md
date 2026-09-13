# Income Tracker

Reads incoming Telebirr / CBE **SMS** (not app notifications — see below),
parses the amount received, and shows a running total. Warm cream & gold
theme, three pages (Home, History, Settings).

Made by Musab & Claude.

## Why SMS instead of notifications

The earlier version watched for Telebirr/CBE app *notifications*, which is
why "nothing showed up" — those apps mostly don't post a system notification
for a transaction. The actual confirmation is a plain **SMS**, sent from a
short code (Telebirr uses `127`). This version reads that SMS directly:

- `SmsReceiver` catches new SMS the moment they arrive.
- `SmsHistoryScanner` can also read the phone's **existing** SMS inbox, so
  transactions from before the app was installed show up too — tap
  "Scan SMS History" on the Home tab after granting SMS access.

## Building this with no computer — phone only

You don't need Android Studio. GitHub will compile the APK for you on
their servers; you just need to get this code into a GitHub repo from
your phone. Do this all in your phone's browser (Chrome/Safari), not
the GitHub app — the app doesn't support Codespaces.

**1. Create a free GitHub account** at github.com if you don't have one.

**2. Create a new empty repository**
   - Tap the "+" → "New repository".
   - Name it `IncomeTracker`, keep it Private or Public, don't add a
     README (this project already has one) → "Create repository".

**3. Open a Codespace** (a free cloud computer in your browser)
   - On the repo page, tap the green "Code" button → "Codespaces" tab
     → "Create codespace on main".
   - This opens a full VS Code editor running in the cloud, in your
     browser. Give it a minute to start.

**4. Upload this project's zip into the Codespace**
   - In the Codespace's file explorer (left sidebar), tap the "..."
     menu (or long-press the empty area) → "Upload...".
   - Choose the `IncomeTracker.zip` file that was sent to you in this
     chat (it should already be saved on your phone / downloads).

**5. Open the built-in terminal and run these commands** one at a time
   (menu → Terminal → New Terminal, or the `` ` `` icon):

   ```bash
   unzip -o IncomeTracker.zip -d /tmp/extracted
   cp -r /tmp/extracted/IncomeTracker/. .
   rm -rf /tmp/extracted IncomeTracker.zip
   git add .
   git commit -m "Update Income Tracker app"
   git push
   ```

**6. Let GitHub build it**
   - Go back to your repo's page → "Actions" tab. A workflow called
     "Build APK" should already be running (it starts automatically
     on push). Wait for the green checkmark (a couple of minutes).

**7. Download the APK**
   - Tap into the finished workflow run → scroll to "Artifacts" →
     tap `IncomeTracker-debug-apk` to download it as a zip.
   - Unzip it (your phone's Files app can do this) to get
     `app-debug.apk`.
   - Tap the `.apk` file to install it. You'll need to allow
     "Install unknown apps" for your browser/Files app the first time
     Android asks. If an older version is already installed, uninstall
     it first (the package name is unchanged, but a clean install avoids
     signature mismatches from the debug key).

## SMS access permission

Android treats reading SMS as a sensitive permission, so the app asks
for it directly (no manual Settings digging needed this time):

1. Open the app → Home tab → tap "Allow SMS Access" → allow both prompts.
2. The status line changes to "SMS access granted".
3. Optionally tap "Scan SMS History" to import past Telebirr/CBE messages
   already sitting in your inbox.

## Fixing sender IDs without a computer

Go to **Settings** tab:

- **SMS Senders** — edit which sender IDs count as Telebirr / CBE
  (comma-separated, e.g. `127`). Defaults are `127` for Telebirr and
  `CBE` for CBE — verify the CBE one is right for your SIM.
- **"Show recent SMS senders"** — lists every distinct sender address in
  your inbox so you can spot the real one and type it into the field
  above, no Logcat or Android Studio required.
- **Unmatched SMS** — if a message comes from a recognized sender but
  doesn't match the parsing pattern, its raw text shows up here so the
  regex in `SmsParsers.kt` can be adjusted to fit.

## CBE parsing

`SmsParsers.parseCbe()` has a best-effort pattern for a typical
"credited with ETB ... balance is ETB ..." message. If your CBE SMS is
worded differently, check Settings ▸ Unmatched SMS for the exact text
and the regex can be tuned to match.

## What counts as income

Only "received"/"credited" messages are parsed — outgoing payments are
ignored. Duplicate SMS (same transaction number, Telebirr only) are
automatically skipped so re-scanning history won't double count.

## Pages

- **Home** — total income, SMS access status, "Scan SMS History", 5 most
  recent transactions.
- **History** — full transaction list, filterable by All / Telebirr / CBE.
- **Settings** — sender IDs, sender detection helper, unmatched SMS
  viewer, clear-data, credits.
