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

Authenticated guests and owners can open `/conversations/:conversationId` directly
using an existing conversation ID. The screen links back to the reservation details;
inbox navigation and reservation entry buttons are tracked separately in #238.
Only the reservation's conversation participants have access, including for accounts
with administrator roles.

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
