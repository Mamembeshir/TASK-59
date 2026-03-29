# InstituteOps (Final Step 11/11)

InstituteOps is a modular Spring Boot 3.3 (Java 21) platform that now includes full student lifecycle operations, grades ledgering, inventory + procurement workflows, store/group-buy campaigns, and recommender features, delivered with Thymeleaf UI and API coverage.

## Services

- `app`: Spring Boot backend + Thymeleaf frontend (`http://localhost:8080`)
- `mysql`: MySQL 8.4 (`localhost:3306`)
- `flyway`: migration bootstrap job
- `uploads` volume: persistent file storage mounted at `/uploads`
- `db_data` volume: persistent MySQL data

## Start the full stack

```bash
docker compose up --build
```

`docker compose` now includes a built-in development encryption key so no `.env` setup is required for local startup. To override it explicitly, set `APP_ENCRYPTION_AES_KEY_BASE64` before running compose.

If you need a clean DB reset:

```bash
docker compose down -v
docker compose up --build
```

## Verification checklist

1. Open `http://localhost:8080/login` and sign in with one of the seeded users below.
2. Confirm dashboard loads and module navigation works:
   - Student lifecycle (`/student`)
   - Grades (`/instructor/grades`)
   - Inventory (`/inventory`)
   - Procurement (`/procurement`)
   - Store manager (`/store`) and student (`/store/student`)
   - Recommender admin (`/admin/recommender`)
3. Verify internal API auth:

```bash
docker compose exec mysql mysql -uroot -proot -Dinstituteops -e "UPDATE sync_config SET enabled=TRUE, lan_only=TRUE WHERE sync_name='LAN_OPTIONAL_SYNC';"
curl -H "X-API-KEY: local-sync-client" -H "X-API-SECRET: internal-secret" http://localhost:8080/api/internal/ping
```

Expected from LAN/local address: `pong`.

If `sync_config.enabled` is `FALSE` (default), `/api/internal/**` is blocked.

## Run tests locally

Run all backend and web-layer tests (required acceptance verification):

```bash
./mvnw clean test
```

This includes:

- backend unit tests (grade calculation, inventory transactions, procurement flow, group-buy rules, recommender, security filter behavior)
- web-layer contract tests via Spring MockMvc slices
- Thymeleaf route rendering tests for login/dashboard/module pages
- security integration tests with real filter chain (`SecurityIntegrationTest`)

Run only frontend flow tests:

```bash
./mvnw -Dtest=ThymeleafUiFlowWebMvcTest,ApiIntegrationWebMvcTest test
```

Run only security integration tests (real filter chain enabled):

```bash
./mvnw -Dtest=SecurityIntegrationTest test
```

## Seeded users

- `sysadmin` / `Admin@123`
- `registrar` / `Registrar@123`
- `instructor` / `Instructor@123`
- `inventory` / `Inventory@123`
- `approver` / `Approver@123`
- `store` / `Store@123`
- `student1` / `Student@123`

## Internal API client

- Key: `local-sync-client`
- Secret: `internal-secret`
