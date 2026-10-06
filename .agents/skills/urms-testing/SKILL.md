---
name: testing-usagi-railway
description: Run and browser-test the Usagi Railway URMS legacy Java 8 JSP app (power management + rolling-stock maintenance), its C/COBOL batches, seeded state, confirmations and role boundaries.
---

# Environment
- Run from the repository with `JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 mvn -q -B spring-boot:run`.
- Before restarting, inspect `lsof -iTCP:8080 -sTCP:LISTEN`; terminate only the app's stale listener. Do not `pkill -f` with a pattern that also matches your own shell command.
- Wait for `Started UsagiRailwayApplication` and use `http://localhost:8080/urms/login`.
- H2 is in-memory: every restart resets the seed (operation date 2026/10/05, 5 substations, 6 formations, 4 failures, 2 repair orders). `demo/reset.sh` also resets git state and batch work dirs.
- The app pins JVM / Joda default time zone to Asia/Tokyo in `UsagiRailwayApplication`. If dates are off by one day, that pin was lost.
- Batches: `batch/c/run.sh` (gcc) and `batch/cobol/run.sh` (cobc). RC=4 is expected for the fixtures (missing telemetry / inspection warnings); RC>=8 is a failure.
- Ensure a Japanese font is installed before launching the browser (`fc-list :lang=ja`).

# Devin Secrets Needed
None. Demo logins (in-memory, public fixtures): `shirei/shirei123` (DISPATCHER), `kenshu/kenshu123` (MAINTAINER), `maker/maker123` (MAKER), `rtu/rtu123` (RTU, API only), `admin/admin123`.

# Seed facts useful for assertions
- Unacknowledged alarms: #3 SS04 52F2 trip (重故障), #5 SS02 overcurrent.
- SS04 52F2 is TRIPPED; an outage on it can be approved but `き電停止` fails with UR-2003.
- Outages: P-26-0039 完了, P-26-0040 承認済 (SP01 52T2), P-26-0041 申請中 (SS01 52F1). Next number P-26-0042.
- Inspections: U5103 超過 (交番), U5101/U5102/U3101 注意, U3102/U5104 正常. U5102 is 注意 because of 走行km (585,400).
- Failures: F-26-0001 修理依頼中 (R-26-0001 修理中, U5103 休車), F-26-0002 調査中, F-26-0003 修理済, F-26-0004 未処置. Next F-26-0005 / R-26-0003.
- `UV25-0901` / `US25-0902` are spares; registering a failure on them fails with UR-3001.
- Registering a severity A failure on an in-service formation sets it to 休車.

# Browser workflows
- Outage approve / き電停止 / 復電, repair requests and failure close use native confirm dialogs: submit then accept.
- Failure form: the serial number field normalizes full-width input and looks up the mount position via `/api/equipment/{serial}` on change.
- 警報一覧 auto-reloads every 60 s; finish interactions quickly or reopen the page.
- 電力日報 and 検査期限 show the same fixed-length file the C / COBOL batch produces; compare with `batch/*/expected/`.

# Role testing
- Test both hidden controls and server authorization with browser-origin POSTs carrying the user's own CSRF token.
- MAKER: no `/power/**` (403), can view rolling stock and update repair progress only.
- MAINTAINER: outage applications, failure registration/repair requests; cannot approve outages or acknowledge alarms.
- DISPATCHER: alarm ack, outage approve/reject/start/complete; cannot create outages or register failures.
- `/api/batch/**` is ADMIN only; `/api/telemetry` POST is RTU/ADMIN only (HTTP Basic).
