# InstituteOps

Docker-first setup for the full InstituteOps stack (app + MySQL + automated tests).

## Start app + database + automated tests

```bash
docker compose up --build
```

What this starts:

- `mysql`: MySQL 8.4
- `app`: Spring Boot application at `http://localhost:8080`
- `tests`: `run_test.sh` (executes `./mvnw -B verify -Pintegration-tests`)

`tests` service logs stream directly in terminal during `docker compose up` (Surefire/Failsafe output is visible).

## Reset database and rerun

```bash
docker compose down -v
docker compose up --build
```

## Test log viewing

If you want to re-follow only test output:

```bash
docker compose logs -f tests
```

## URLs

- App login page: `http://localhost:8080/login`
- API example: `http://localhost:8080/api/internal/ping`

## Login credentials (local QA/demo only)

> **These credentials are for local development and QA testing only.**
> They must NOT be used in any production or publicly accessible deployment.
> For production, all passwords and secrets must be rotated and provided via environment variables.

- `sysadmin` / `Admin@123`
- `registrar` / `Registrar@123`
- `instructor` / `Instructor@123`
- `inventory` / `Inventory@123`
- `approver` / `Approver@123`
- `store` / `Store@123`
- `student1` / `Student@123`

## Internal API client (local QA/demo only)

> **For production:** override `X-API-KEY` and `X-API-SECRET` with securely generated values via environment variables.

- Key: `local-sync-client`
- Secret: `internal-secret`

## Production deployment notes

When deploying outside of local QA/demo:

1. **Database credentials:** Override `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD` with strong, unique values.
2. **Encryption key:** Override `APP_ENCRYPTION_AES_KEY_BASE64` with a securely generated 256-bit AES key (base64 encoded).
3. **User passwords:** All seeded user passwords must be changed on first login or re-seeded with strong passwords.
4. **Internal API secret:** Override the internal API client secret via database seed or environment configuration.
5. **MySQL root password:** Override `MYSQL_ROOT_PASSWORD` with a strong value.
