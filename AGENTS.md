# AGENTS.md

Read `PRD.md` first (scope, roles, schema). This file lists the technical conventions in the codebase so every agent builds on them instead of reinventing them.

## Stack

- Java 21, Jakarta EE 10: **JSP 3.1 + JSTL 3 + Servlets 6**, plain JDBC + HikariCP, PostgreSQL 16+. No frameworks (Quiz 2 requires JSP).
- Maven. Local server: `mvn jetty:run` (Jetty 12 ee10). Production: Tomcat 10.1 via `Dockerfile` (Render).
- Config from environment variables, falling back to `.env` in the working directory (`Env.get`).

## Commands

```bash
mvn jetty:run          # http://localhost:8080, creates schema + seeds an empty database
mvn verify             # compile + unit tests (CI fails otherwise)
bash scripts/smoke.sh  # against a running app: every page, every role, key business rules
```

## Conventions you must reuse

| Need | Use |
|------|-----|
| New module | A servlet extending `BaseServlet`, mapped to `/things/*`; override `index/create/store/show/edit/update/destroy` |
| SQL | `Db.list/one/value/count/update/insert` with `?` parameters only. Never concatenate user input into SQL |
| Transactions | `Db.tx(() -> { ... })` (see `SaleServlet`, `TreeServlet`) |
| Validation | `Form` (`text`, `date`, `decimal`, `id`, `option`, `email`, `password`, `unique`), then `invalid(...)` to re-render with errors |
| Filters + pagination (15) | `new Query().search(...).filter(value, "col = ?").page(req, select, from, orderBy)` + `<ui:pagination result="${page}" noun="..."/>` |
| Fixed options | `Options.*` enums; JSP reads `${treeStatuses}` etc.; label any stored value with `${h:label(v)}` |
| Authorization | `requireAdmin(req)`, `requireWriter(req)` (not viewer), `requireOwnerOrAdmin(req, row)`; in JSP `${isAdmin}`, `${canWrite}` |
| Output | Escape every user value: `${h:e(x)}`. Dates `${h:date(d)}` → `21 Oct 2026`, `${h:number}`, `${h:qty}`, `${h:money}` → `IDR 1,250,000` |
| POST forms | Always `<ui:csrf/>`; deletes use `<ui:delete action="/things/1/delete" what="thing"/>` |
| Page shell | `<ui:layout title="...">`, `<ui:header title=... description=...>actions</ui:header>`, `<ui:empty icon=... title=...>` |
| Form fields | `<ui:input>`, `<ui:select>`, `<ui:textarea>` (show errors automatically) |
| Flash | `flash(req, "The tree was created successfully.")` / `flashError(...)`, then `redirect(...)` |
| Author columns | Set `user_id` / `created_by` from `userId(req)`, never from input |
| `sales.total_amount` | `SaleService.total(...)`; quantity rule `SaleService.check(...)` inside `Db.tx` |
| Photos | Stored in `trees.photo` (bytea), served by `/trees/{id}/photo`; type checked by magic bytes |
| App name | `App.NAME` / `${appName}`, never a literal |

## Rules

- All UI text is correct English (Title Case headings, sentence case buttons, full sentences with a period for messages). Code and commits are English.
- Lists: paginate 15, join what you display in one query (no N+1).
- Schema changes go in `schema.sql` as idempotent statements (`create ... if not exists`, `alter table ... add column if not exists`), plus a PRD section 6 update.
- The team works directly on `main`. Conventional Commits. Never push yourself; the human pushes.
- Do not report a task as done while `mvn verify` or the smoke test fails.
- Production disk is not persistent: never write application data to files.
