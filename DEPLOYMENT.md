# Telefam deployment

## Railway backend

The repository root contains a Dockerfile that builds only the Ktor backend and runs it on Railway's assigned `$PORT` (set `SERVER_PORT` to `${{RAILWAY_TCP_PROXY_PORT}}` only if using a TCP proxy; for the normal Railway public domain, set `SERVER_PORT=8080`). `railway.toml` configures Dockerfile builds and `/health` checks.

Create a Railway project with:

1. A backend service deployed from this repository.
2. A Railway PostgreSQL service.
3. A public domain for the backend service.

Run `backend/sql/telefam_postgres_schema.sql` once against the Railway PostgreSQL database, then configure the backend service variables. Never commit real values.

Required backend variables:

- `SERVER_HOST=0.0.0.0`
- `SERVER_PORT=8080`
- `POSTGRES_JDBC_URL=jdbc:postgresql://...`
- `POSTGRES_USER=...`
- `POSTGRES_PASSWORD=...`
- `POSTGRES_MAX_POOL_SIZE=5` (increase after load testing)
- `JWT_ISSUER`, `JWT_AUDIENCE`, `JWT_SECRET_KEY`
- `SESSION_AES_GCM_MASTER_KEY`
- `SERVER_FINGERPRINT_PEPPER`
- `SANDBOX_WORK_DIR=/var/telefam/sandbox/quarantine`
- `SECURE_STORAGE_DIR=/var/telefam/storage/verified_avatars`
- `RESEND_API_KEY`, `RESEND_FROM_EMAIL`
- `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`
- `KLIPY_API_KEY`, `KLIPY_BASE_URL`

Railway's PostgreSQL `DATABASE_URL` is not used automatically by the current application; copy its connection details into the three `POSTGRES_*` variables above. A persistent object-storage/CDN arrangement should be added before relying on avatar storage in production because Railway's local filesystem is ephemeral.

## Android APK

Use the debug APK for local installation:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

For a release APK, provide a keystore through `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD`, and set `TELEFAM_API_BASE_URL` to the deployed Railway HTTPS URL before building.

The app also needs `google-services.json` for Firebase push notifications and Google sign-in functionality. It is intentionally not committed.
