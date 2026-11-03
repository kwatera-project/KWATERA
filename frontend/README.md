[← Back to the project README](../README.md)

# Frontend

The KWATERA frontend is a React and TypeScript application built with Vite. It provides guest booking, owner management, administration, billing, reporting, and meter-reading user flows. The production container builds the application with Bun and serves the static files through nginx.

## Main responsibilities

- Render guest, owner, and administrator interfaces.
- Handle authentication state and role-specific navigation.
- Send API requests through the API Gateway.
- Provide a static demo mode for the GitHub Pages deployment.

## Default port

After Docker Compose startup: [http://localhost:5173](http://localhost:5173)

API calls use `VITE_API_BASE_URL` when set and otherwise target `http://localhost:8090`.

## Local development

Bun is the supported local package manager:

```bash
bun install --frozen-lockfile
bun run dev
```

Run `bun run lint` and `bun run build` before submitting frontend changes.

## Reservation conversations

Authenticated users can open **Messages / Wiadomości** from the profile
dropdown on desktop and mobile. `/conversations` lists the current participant's
available conversations, sorted by `lastMessageAt` with `createdAt` as a fallback,
preserving backend sub-millisecond precision. Cross-tab token changes or logout
immediately invalidate the inbox and prevent earlier session requests from updating it.
Each item shows its reservation ID, status, last activity and available unit name.
Optional unit names are loaded only for the visible page of ten conversations, with
at most three concurrent lookups per page; lookup failures use a fallback label.
The existing list API has no pagination, so the conversation metadata itself is
fetched in one request. No message previews or unread counts are inferred.

**Open conversation / Otwórz rozmowę** in My Reservations, owner reservation management
and reservation details calls the existing idempotent reservation conversation API.
Repeated clicks are blocked while a request is pending. Blocked reservations have a
disabled action; the backend decides availability for other statuses, including
retention for cancelled and completed reservations. Access and lifecycle failures
are shown without conversation details and failed requests can be retried.

`/conversations/:conversationId` continues to support direct links. The conversation
screen links to both the inbox and reservation details, including an inbox exit for
inaccessible conversations. Both conversation routes require authentication and use
backend participant authorization, including for users whose only role is administrator.
Administrator roles (including mixed roles) do not enable actions for unrelated
reservations. Their reservation action is shown only when the available reservation
guest ID matches their user ID. An owner snapshotted in a conversation can still
open it through the inbox or a direct link without assuming current unit ownership.
Static demo mode hides messaging navigation and reservation actions because GitHub
Pages has no conversation backend; it does not simulate conversations.

Sending uses the gateway REST API; persisted messages arrive live through native
WebSocket/STOMP 1.2 at `/ws/conversations`. The URL follows `VITE_API_BASE_URL`,
converting HTTP/HTTPS to WS/WSS. JWTs are sent only in CONNECT headers, never URLs.
The active conversation and private error queue are the only subscriptions.
History loading starts after the unique message subscription receipt, buffers live
deliveries and merges by persisted ID. If transport is unavailable or silent, REST
history starts within two seconds so the chat remains usable. REST sending is
available during an outage; failed or uncertain POSTs are never automatically retried.

The localized status shows connection, reconnection and actionable failures.
STOMP owns exponential transport retries (1 second initially, capped at 30 seconds).
After subscription readiness, recovery traverses REST cursor pages to the last
completed REST snapshot or the end, retaining overlapping live messages. Authentication
and permanent subscription failures stop automatic retries; JWT expiry clears
protected conversation data and requires login. Session changes and navigation abort
conversation requests and dispose subscriptions, socket and timers. Static demo
mode does not create a live connection. This follows [the backend contract](../docs/messaging.md);
delivery is best effort and REST history remains the recovery source.

**Refresh** retrieves newer messages (including intervening pages);
**Load older messages** follows the backend cursor. Desktop Enter sends and Shift+Enter inserts a
line break; touch devices use the Send button. Messages accept 1–4000 UTF-16 code
units after the same edge-whitespace normalization as the backend. Failed sends
retain the draft; after an ambiguous network failure, refresh before retrying to
check whether the server persisted the message.

For manual verification with a running gateway, open the same authorized conversation
as each participant and send multiline text. Both sessions should receive the same
persisted message once without refreshing. Disconnect one session, send enough messages
to span multiple history pages, and reconnect; verify complete, ordered recovery and
manual refresh/older-history pagination. Use an explicitly allowed frontend origin:
the backend defaults to `http://localhost:5173` and `http://localhost:5174`; align
`KWATERA_MESSAGING_ALLOWED_ORIGINS` and HTTP CORS for other origins, without disabling
origin validation. Check blank/overlong validation, failed-send draft retention, request retries,
and unavailable/missing conversations in both Polish and English on desktop and mobile.
An unrelated account must see no reservation context or history; a signed-out visit
must redirect to login. Run the focused ordering/normalization regression checks with
`bun test tests/conversationMessages.test.mjs`; these use the runtime's built-in test
support without adding a test framework or dependencies.

Inbox/navigation regression checks are in `tests/conversationInbox.test.mjs` and
`tests/e2e/conversations.spec.ts`. Run:

```bash
bun run test:unit
bunx playwright install chromium
bun run test:conversations
bun run test:conversations:demo
```

The unit script uses Node's built-in TypeScript support (Node 22.18+ or 24+).
`conversationSession.test.mjs` and `conversationSocket.test.mjs` add deterministic
API/client mocks for receipt readiness, buffered initialization, REST/live overlap,
multi-page recovery, cursor preservation, transport fallback, failures, expiry and
session teardown. `tests/e2e/conversationLive.spec.ts` uses the actual STOMP library
against a Playwright-controlled WebSocket broker and mocked REST. It verifies
protocol headers/receipts, delivery, reconnection, history overlap, REST sending
during outages, access errors, account/conversation changes and scroll preservation.
These are frontend integration tests, not real backend delivery tests.

The browser script builds the production frontend, serves it at
`http://127.0.0.1:4173`, and runs desktop (1440×1000) and mobile (390×844) Chromium
checks. API responses and JWTs are controlled fixtures; this verifies UI behavior,
not live backend integration. Coverage includes role visibility and route guards,
loading/empty/error states, retries, activity ordering, bounded context enrichment,
conversation reuse and duplicate-click prevention at all three reservation entry
points, access/lifecycle failures, Polish/English and navigation between contexts.
The focused `review regressions` group covers cross-tab session invalidation,
administrator participants, and reservation card geometry at 320, 390, 768, 1024,
1280 and 1440 pixels with English/Polish labels and USD/EUR/PLN totals. It can be
run after building with `bunx playwright test --grep 'review regressions'`.
The demo script uses a separate Vite demo server at `http://127.0.0.1:4174`
and verifies messaging stays hidden on navigation and reservation surfaces.
Test artifacts go to the system temporary directory. Optionally set
`PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH` to an existing Chromium executable and
`KWATERA_SCREENSHOT_DIR` to an existing directory for screenshots.

For real integration verification, use a running gateway with actual guest and owner
accounts for the same reservation: open the conversation from each reservation
view, verify both obtain the same conversation ID, then open it from each inbox.
An unrelated user, including an unrelated administrator, must not gain access;
an administrator who is an actual participant must retain access.
Check cancelled/completed reservations within and beyond the configured retention
boundary; unavailable conversations should disappear from the inbox and direct
links should show a localized error. For an independent deployed-backend smoke test,
follow `scripts/messaging-smoke.ps1` and the instructions in `docs/messaging.md`.
