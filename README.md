# RippleUp

A sustainability-actions Android app (Jetpack Compose), rebuilt 1:1 from the RippleUp MVP
design PDF (73 pages). Local-only backend (Room) with tamper-evident stats (HMAC-SHA256),
per-action cooldowns and daily caps.

## Build

```
./gradlew assembleDebug          # debug APK
./gradlew assembleRelease        # signed release APK (CI injects keystore via secrets)
```

CI: `.github/workflows/build-apk.yml` builds debug + signed release on every push to `main`
and uploads them as artifacts (`rippleup-debug-apk`, `rippleup-release-apk`).

Signing secrets (set once via `gh secret set`): `KEYSTORE_B64`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD`.

## Test account

Local auth only: log in with `admin` / `rudra` (one-tap fill button on the login form).

## Verify flow

- Self-report: FAB → "Self - Report" → 5-minute window → camera capture → Submit & Verify.
- Partner QR: FAB → "RippleUp QR" → live camera + MLKit barcode scan.
- Guards: per-action cooldowns (45/30/90/120 min), 12 actions/day, 300 pts/day.

## Desktop (Windows) build

`desktop/` is a native Windows port of the same UI (Compose Multiplatform 1.6.11):
all screens, the verify flow, badges, sheets and the demo data are identical.

Platform swaps vs. the Android build:
- Persistence: Room → atomic JSON store at `~/.rippleup/state.json` (same schema/HMAC tags)
- Photo capture: camera intent → file chooser (Swing `JFileChooser`)
- QR scan: live CameraX+MLKit → identical viewfinder; click simulates scanning a partner QR
- Auth/session storage: SharedPreferences → `java.util.prefs`

Build & run:
```
cd desktop
./gradlew run                 # dev run
./gradlew createDistributable # self-contained app: build/compose/binaries/main/app/RippleUpDesktop
```
`RippleUpDesktop.exe` runs without any Java installation (bundled runtime).
CI: `.github/workflows/build-desktop.yml` builds the zip on every push touching `desktop/`.

---

## PRODUCTION SETUP (Supabase backend) — 5 steps

The app now has a full cloud backend: user accounts, sync, QR verification with
location proof (who + where + map), admin review, badges, notifications, events.

1. **Create the database** — Supabase Dashboard → SQL Editor → paste
   `supabase/schema.sql, then supabase/migration_v2_security.sql` → Run. (Creates profiles, ripples, partner_locations,
   verifications, badges, notifications, event_registrations, RLS, and the
   approve/reject/badge pipelines.)
2. **Connect the app** — Supabase Dashboard → Settings → API → copy the Project URL →
   paste it into `app/src/main/java/com/yft/rippleup/data/remote/Config.kt`
   (`SUPABASE_URL`) and the same file under `desktop/`. The publishable key is already set.
3. **Auth settings** — Authentication → Providers → Email: keep enabled; turn OFF
   "Confirm email" while testing (re-enable on Pro).
4. **Google sign-in** — Google Cloud Console → create an OAuth **Web** client ID →
   paste it into Supabase Auth → Google provider config AND into `GOOGLE_WEB_CLIENT_ID`
   in both Config.kt files. Until then the Google button shows a setup hint and
   email login works.
5. **Make yourself admin** — log in once in the app, then SQL Editor:
   `update public.profiles set is_admin = true where email = 'your-email';`
   The Admin Review screen appears on your Profile.

### QR verification pipeline
- Print the codes in `../RippleUp-QR-Codes/` and stick them at the partner locations.
- Each QR encodes a signed location ID. Scanning requires the app's GPS fix; the app
  checks the distance to the location's geofence, then files a **verification receipt**:
  who scanned, which account, which place, exact GPS coordinates, distance, device, time.
- Admin Review approves/rejects → cloud pipeline awards points, badges and notifies the user.

### Free tier
Everything runs on the Supabase free plan (500 MB DB, 50k monthly users). When you
upgrade to Pro, just re-enable email confirmation and add backups — no code changes.
