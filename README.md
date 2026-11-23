# FloPro Plumbing — Job Cards

A simple phone app for **FloPro Plumbing** (Kwambonambi): job cards with photos, a saved customer list, and quotes and invoices in FloPro's own invoice layout.

## What it does
- **Job cards:** customer, site, problem reported, work done, photos, parts & labour
- **Customers:** saved automatically the first time a name is typed on a job card; pick, edit, call or WhatsApp them; see their jobs and what they owe
- **Quotes & invoices:** made from a job card in one tap, in the FloPro layout (logo, terms, bank details). Save as PDF and send. Quotes turn into invoices; invoices get marked paid
- **Job card printout** with photos and signature lines
- **Backup / restore** to a file
- **Android app (APK)** that works offline and updates itself

All data (jobs, customers, photos, invoices) is stored **on the phone** (IndexedDB). Updates to the app never touch it.

## How it ships: APK with auto-update
The whole app is packed inside the Android APK (`android-app/`, Capacitor). It works fully offline.

Every push to `main` runs `.github/workflows/android.yml`, which builds `index.html` from `src/app.html`, packs it into the APK, signs it with the FloPro key and publishes it as the latest GitHub release.

When the app opens (or comes back to the front, at most every 10 minutes), it checks the latest release on GitHub. If there is a newer APK it shows **Update now**: one tap downloads it and opens Android's installer. Jobs, customers, stock and invoices are kept, because the update is signed with the same key.

Download link (always the newest): https://github.com/freddieschallies/flopro/releases/latest/download/FloPro-Jobs.apk

### Signing key
`android-app/signing/flopro-release.jks` is the FloPro release key, locked with a strong password. The password lives only in the repo secret `FLOPRO_KEYSTORE_PASSWORD` (and with Fred). Without it the build stops.
Never change or lose this key: Android only installs an update over the app if it is signed with the same key.

## Making changes
1. Edit `src/app.html` (the whole app: HTML, CSS and JavaScript) or `src/sw.js`
2. Test in a browser: `python3 scripts/build.py && python3 -m http.server` then open http://localhost:8000
3. Commit and push to `main` — a new APK is built and every phone is offered the update next time the app opens

Keep saved data compatible: new fields should have defaults, and old records must still load (see `migrate()` in `src/app.html`).
