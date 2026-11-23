# FloPro Plumbing — Job Cards

A simple phone app for **FloPro Plumbing** (Kwambonambi): job cards with photos, a saved customer list, and quotes and invoices in FloPro's own invoice layout.

## What it does
- **Job cards:** customer, site, problem reported, work done, photos, parts & labour
- **Customers:** saved automatically the first time a name is typed on a job card; pick, edit, call or WhatsApp them; see their jobs and what they owe
- **Quotes & invoices:** made from a job card in one tap, in the FloPro layout (logo, terms, bank details). Save as PDF and send. Quotes turn into invoices; invoices get marked paid
- **Job card printout** with photos and signature lines
- **Backup / restore** to a file
- **Works offline** and can be added to the home screen

All data (jobs, customers, photos, invoices) is stored **on the phone** (IndexedDB). Updates to the app never touch it.

## Auto-update
Every push to `main` runs `.github/workflows/deploy.yml`, which:
1. Builds `index.html` from `src/app.html` and stamps it with a new build id
2. Writes the same id to `version.json`
3. Deploys to GitHub Pages

When the app is opened (or comes back to the foreground, or every 30 minutes), it checks `version.json`.
If the build id is newer it updates itself: straight away on a list screen, or with an **Update now** banner if Barry is busy editing something.
The service worker (`src/sw.js`) loads network-first, so the newest files arrive at once when online and the cached copy is used when offline.

## Making changes
1. Edit `src/app.html` (the whole app: HTML, CSS and JavaScript) or `src/sw.js`
2. Test locally: `python3 scripts/build.py && python3 -m http.server` then open http://localhost:8000
3. Commit and push to `main` — every phone picks up the change the next time the app is opened

Keep saved data compatible: new fields should have defaults, and old records must still load (see `migrate()` in `src/app.html`).

## Setup (once)
Repo **Settings → Pages → Build and deployment → Source: GitHub Actions**. The app's link is then shown there (https://freddieschallies.github.io/flopro/).
Open it on the phone and choose **Add to Home screen**.

## Android app (APK)
`android-app/` is a small Capacitor shell that opens https://freddieschallies.github.io/flopro/ full screen, so the APK picks up every app update automatically (and works offline after the first open).
It adds native **print / save PDF** (Android print screen) and **share backup file**, plus camera access for job photos.

Every push that touches `android-app/` runs `.github/workflows/android.yml`, which builds the APK and attaches it to a GitHub release.
**Download link (always the newest):** https://github.com/freddieschallies/flopro/releases/latest/download/FloPro-Jobs.apk

### Signing
Release APKs are signed with the FloPro key from three repo secrets: `FLOPRO_KEYSTORE_BASE64`, `FLOPRO_KEYSTORE_PASSWORD`, `FLOPRO_KEY_ALIAS`.
Keep a safe copy of the keystore - Android only lets a new APK install over the old one if it is signed with the same key.
Without the secrets the workflow builds a debug-signed test APK.
