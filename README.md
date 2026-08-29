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
