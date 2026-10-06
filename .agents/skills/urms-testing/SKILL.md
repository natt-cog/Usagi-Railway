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
- Outages: P-26-0039 復電完了, P-26-0040 承認済 (SP01 52T2), P-26-0041 申請中 (SS01 52F1). Next number P-26-0042.
- Inspections: U5103 超過 (交番), U5101/U5102/U3101 注意, U3102/U5104 正常. U5102 is 注意 because of 走行km (585,400).
- Failures: F-26-0001 修理依頼中 (R-26-0001 修理中, U5103 休車), F-26-0002 調査中, F-26-0003 修理完了, F-26-0004 受付. Next F-26-0005 / R-26-0003.
- Initial repair status REQUESTED is displayed as 依頼中; initial failure OPEN is 受付.
- `UV25-0901` / `US25-0902` are spares; registering a failure on them fails with UR-3001.
- Registering a severity A failure on an in-service formation (運用中) sets it to 休車.

# Browser workflows
- Outage approve / き電停止 / 復電, repair requests and failure close use native confirm dialogs: submit then accept.
- Alarm acknowledgement has no confirmation dialog.
- Failure form: the serial number field normalizes full-width input and looks up the mount position via `/api/equipment/{serial}` on change.
- Assert normalized input and lookup result independently: successful blur normalization alone does not prove that the lookup request used the normalized serial.
- 警報一覧 auto-reloads every 60 s; finish interactions quickly or reopen the page.
- 電力日報 and 検査期限 show the same fixed-length file the C / COBOL batch produces; compare with `batch/*/expected/`.
- In a long running instance, record IDs may already exceed the seed's next values. Reuse the IDs actually created by your browser flow rather than restarting solely to recover expected numbering.

# Japanese input and recording
- Native typing tools may omit Japanese or full-width characters on Linux. If that happens, use OS clipboard paste into the real UI: `printf '%s' 'ＵＶ０８－０００１' | xclip -selection clipboard`, then focus the input and press Ctrl+V.
- Capture the pasted full-width value before pressing Tab, then capture normalized input and mount lookup result after Tab. Do not classify input-tool omissions as application mojibake.
- Check `command -v xclip` and `command -v wmctrl` before relying on these tools.
- Maximize Chrome before recording with `wmctrl -r :ACTIVE: -b add,maximized_vert,maximized_horz`.
- Chrome may warn that the public demo passwords were found in a breach. Dismiss that browser warning; do not change the app's fixture credentials.

# Role testing
- Test both hidden controls and server authorization with browser-origin POSTs carrying the user's own CSRF token.
- The logged-in page's hidden `input[name="_csrf"]` is available even when its feature action forms are absent (e.g. logout form). Do not copy tokens across users.
- MAKER: no `/power/**` (403), can view rolling stock and update repair progress only.
- For a convincing hidden-control test, open an OPEN/受付 fault as maker: repair request must be absent even though a maintainer would be allowed to request repair in that state.
- MAINTAINER: outage applications, failure registration/repair requests; cannot approve outages or acknowledge alarms.
- DISPATCHER: alarm ack, outage approve/reject/start/complete; cannot create outages or register failures.
- `/api/batch/**` is ADMIN only; `/api/telemetry` POST is RTU/ADMIN only (HTTP Basic).
- Returned repairs hide the maker update form. Use a browser-origin own-CSRF form POST to test rollback after return; then verify the visible UR-3003 banner and unchanged 返却済 / 修理完了 state.
