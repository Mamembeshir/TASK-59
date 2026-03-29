# InstituteOps (Step 2 Security + RBAC)

This repository contains the production-ready Maven Spring Boot 3.3+ (Java 21) skeleton for InstituteOps, including:

- Spring Boot app with Thymeleaf and static assets in one container
- MySQL 8.4 database service
- Flyway migration bootstrap with complete initial schema
- Docker volume persistence for database data and homework uploads (`/uploads`)
- Spring Security form login with bcrypt password hashes
- Full RBAC with 7 roles
- Internal API key/secret authentication for `/api/internal/**`
- AES-256 (GCM) encryption converters for sensitive contact/payment reference fields
- Operation and data-access logging

## One-command startup

```bash
docker compose up --build
```

If MySQL still shows initialization errors from a previous failed boot, reset the DB volume once and start again:

```bash
docker compose down -v
docker compose up --build
```

After startup:

- App: http://localhost:8080
- MySQL: localhost:3306

## Seeded login users

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

Example:

```bash
curl -H "X-API-KEY: local-sync-client" -H "X-API-SECRET: internal-secret" http://localhost:8080/api/internal/ping
```
