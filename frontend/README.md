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

Authenticated guests and owners can open **Messages / Wiadomości** from the profile
dropdown on desktop and mobile. `/conversations` lists the current participant's
available conversations, sorted by `lastMessageAt` with `createdAt` as a fallback.
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
inaccessible conversations. Both conversation routes require a guest or owner role.
Administrator-only accounts have no conversation navigation or reservation actions;
administrator privileges never replace the backend's participant authorization.
Accounts with guest/owner and administrator roles still need to be participants.

History and sending use the gateway REST API. **Refresh** retrieves newer messages
(including intervening pages); **Load older messages** follows the backend cursor.
There is no polling or live delivery. Desktop Enter sends and Shift+Enter inserts a
line break; touch devices use the Send button. Messages accept 1–4000 UTF-16 code
units after the same edge-whitespace normalization as the backend. Failed sends
retain the draft; after an ambiguous network failure, refresh before retrying to
check whether the server persisted the message.

For manual verification with a running gateway, open the same authorized conversation
as each participant, send multiline text, refresh the other session, and load older
history. Check blank/overlong validation, failed-send draft retention, request retries,
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
```

The unit script uses Node's built-in TypeScript support (Node 22.18+ or 24+).
The browser script builds the production frontend, serves it at
`http://127.0.0.1:4173`, and runs desktop (1440×1000) and mobile (390×844) Chromium
checks. API responses and JWTs are controlled fixtures; this verifies UI behavior,
not live backend integration. Coverage includes role visibility and route guards,
loading/empty/error states, retries, activity ordering, bounded context enrichment,
conversation reuse and duplicate-click prevention at all three reservation entry
points, access/lifecycle failures, Polish/English and navigation between contexts.
Test artifacts go to the system temporary directory. Optionally set
`PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH` to an existing Chromium executable and
`KWATERA_SCREENSHOT_DIR` to an existing directory for screenshots.

For real integration verification, use a running gateway with actual guest and owner
accounts for the same reservation: open the conversation from each reservation
view, verify both obtain the same conversation ID, then open it from each inbox.
An unrelated participant and an administrator-only account must not gain access.
Check cancelled/completed reservations within and beyond the configured retention
boundary; unavailable conversations should disappear from the inbox and direct
links should show a localized error. No live transport is implemented here (#237).
