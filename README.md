# Harvesta

**Tree & orchard management for small farms.** Record planting blocks, tree types, every individual tree, care activities, harvests, expenses, and sales, then follow productivity and profit on a dashboard and in reports.

Built for EF234301 Web Programming (D), **Quiz 2**: a CRUD website in **JSP (Jakarta Server Pages)**. The Midterm version (Laravel) was migrated to JSP + Servlets; it remains in the Git history. The product spec is in [`PRD.md`](PRD.md).

[![tests](https://github.com/HilmyFausta/harvesta/actions/workflows/tests.yml/badge.svg)](https://github.com/HilmyFausta/harvesta/actions/workflows/tests.yml)

## Features

| # | Feature | CRUD | What it does |
|---|---------|------|--------------|
| F-01 | **Dashboard** | R | Tree, block, and monthly harvest stats; upcoming and overdue care; recent harvests; 12-month harvest chart; sales vs. expenses for admins. |
| F-02 | **Categories** | CRUD | Master data (for example *Fruit*). Cannot be deleted while tree types use it. |
| F-03 | **Tree Types** | CRUD | Master data per category (for example *Durian*), with an optional scientific name. |
| F-04 | **Blocks** | CRUD | Planting areas; the detail page lists their trees and harvest totals. |
| F-05 | **Trees** | CRUD | Auto code (`T-0001`), type, block, planting date, status, height, notes, one photo. Soft delete. |
| F-06 | **Care Logs** | CRUD | Activities with an optional next due date. Staff edit only their own. |
| F-07 | **Harvests** | CRUD | Quantity in kg or pcs and a grade. Dead trees accept no new care logs or harvests. |
| F-08 | **Expenses** | CRUD | Farm costs by category, per block or general. |
| F-09 | **Sales** | CRUD | Tied to a harvest; total computed on the server; a sale can never exceed the unsold quantity. |
| F-10 | **Reports** | R + export | Harvest, financial, and care summaries for any period, with charts and streamed CSV export. |
| F-11 | **User Management** | CRUD | Users and roles. The last admin cannot be demoted or deleted. |
| F-12 | **Profile** | RUD | Change name, email, and password; delete own account. |
| — | **Auth** | | Register (new users are Staff), login, logout. |

Every list has search and filters, 15-per-page pagination, delete confirmation, flash messages, and an empty state.

## Roles

| Role | Access |
|------|--------|
| **Admin** | Everything: all CRUD, master data, finance, reports, users. |
| **Staff** | Views all data; creates and edits trees, care logs, and harvests (edits only their own care logs/harvests). No deleting, finance, users, or master data changes. |
| **Viewer** | Read-only. |

## Tech stack

| Layer | Choice |
|-------|--------|
| View | **JSP 3.1** + JSTL 3 + custom tag files (`WEB-INF/tags`) and EL functions (`WEB-INF/harvesta.tld`) |
| Controller | Jakarta Servlets 6 (one servlet per module, `com.harvesta.web`) |
| Model / data | Plain JDBC (`Db.java`) with HikariCP, PostgreSQL 16+ |
| Security | BCrypt passwords, session login with fixation protection, CSRF tokens, role checks per action, output escaping, parameterized SQL |
| Frontend | Hand-written CSS (`assets/app.css`), Chart.js 4, a little vanilla JS |
| Build / run | Java 21, Maven, Jetty 12 (local), Tomcat 10.1 in Docker (production) |

## Running locally

Requirements: **JDK 21**, **Maven 3.9+**, **PostgreSQL 16+**.

```bash
cp .env.example .env              # fill DB_PASSWORD; DB_DATABASE must be an EMPTY database
createdb -U postgres harvesta_jsp
mvn jetty:run                     # http://localhost:8080
```

On first start the app creates every table (`src/main/resources/schema.sql`) and seeds accounts, master data, and twelve months of demo data. To start over, drop and re-create the database.

### Demo accounts

| Email | Role | Password (local) |
|-------|------|------------------|
| `admin@example.com` | Admin | `password` |
| `staff@example.com` | Staff | `password` |
| `viewer@example.com` | Viewer | `password` |

In production the passwords come from `SEED_*_PASSWORD` and the app refuses to seed without them.

## Checks

```bash
mvn verify                        # compile + unit tests
mvn jetty:run & bash scripts/smoke.sh   # every page and role against a running app
```

GitHub Actions runs both on every push (`.github/workflows/tests.yml`).

## Deployment

Free hosting: Render (Docker web service) + a free PostgreSQL (Neon or Supabase). Step by step: [`docs/DEPLOY.md`](docs/DEPLOY.md).

## Project structure

```
src/main/java/com/harvesta/
  App.java            startup: connect, create schema, seed
  Db.java             JDBC helpers (bound parameters, transactions)
  Env.java            env vars + local .env
  Options.java        fixed option sets (statuses, activities, grades, ...)
  Seeder.java         accounts, master data, deterministic demo data
  SaleService.java    "sale may not exceed unsold harvest" rule
  web/                servlets (controllers), AuthFilter, Form (validation), Query (filters/pagination), Fn (EL functions)
src/main/resources/   schema.sql, seed-photos/
src/main/webapp/
  WEB-INF/views/      JSP pages per module
  WEB-INF/tags/       layout, form fields, pagination, empty state, delete button
  assets/             app.css, app.js, images
scripts/smoke.sh      end-to-end smoke test
```
