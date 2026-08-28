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
