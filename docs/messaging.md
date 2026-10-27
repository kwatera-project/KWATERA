# Conversation messaging integration

The reservation service supports native WebSocket with STOMP 1.2 at
`ws://localhost:8090/ws/conversations` through the reactive API gateway.
No SockJS or HTTP Authorization handshake header is required. Use TLS (`wss`) outside local development.

## Authentication and destinations

Obtain a JWT through the existing `POST /api/auth/login` API. Open the WebSocket
and send CONNECT with `accept-version:1.2`, `host:localhost`, and
`Authorization:Bearer <JWT>`. The service uses its existing JwtService signature
and expiration verification; it adds no separate issuer/audience policy. A verified
`userId` UUID is both Authentication.details and Principal.name; display names and
client-supplied identity headers never select the sender or recipients. A second
CONNECT cannot replace an identity. Tokens in URLs are unsupported and must never be used.

The handshake is publicly accessible under the existing HTTP policy; it does not
establish STOMP authentication. Existing REST security and its CSRF configuration
are unchanged. No Spring messaging CSRF mechanism is enabled: this endpoint uses
JWT-only CONNECT plus explicit WebSocket origin validation. HTTP CORS is a separate
filter and is not a substitute for this origin check.

`KWATERA_MESSAGING_ALLOWED_ORIGINS` is a comma-separated exact allowlist, defaulting
to `http://localhost:5173,http://localhost:5174`, matching the existing frontend
origins. Wildcards and empty entries fail startup. Compose forwards this variable.
If deploying different browser origins, align the existing downstream/gateway HTTP
CORS allowlists too. Nonbrowser clients without Origin remain subject to JWT and
destination authorization.

| Action | Destination |
|---|---|
| SEND | `/app/conversations/{conversationId}/messages` |
| SUBSCRIBE messages | `/user/queue/conversations/{conversationId}/messages` |
| SUBSCRIBE private errors | `/user/queue/messaging-errors` |

SEND uses `content-type:application/json` with only `{"content":"Hello, what time can we check in?"}`.
Unknown fields are rejected; sender, conversation, recipients, IDs and timestamps
cannot be chosen in JSON. The destination supplies the conversation UUID.
Content normalization and 1–4000 Java-character validation run in MessageService,
exactly as REST. Internal whitespace is preserved.

Both participants receive the persisted MessageResponse, for example:

```json
{"id":"11111111-1111-1111-1111-111111111111","conversationId":"22222222-2222-2222-2222-222222222222","senderUserId":"33333333-3333-3333-3333-333333333333","content":"Hello, what time can we check in?","createdAt":"2026-10-27T12:00:00.123456Z"}
```

Only exact destinations with canonical lowercase UUIDs are accepted. Broker queues,
topics, explicit user identities, wildcard subscriptions and unknown destinations
are denied. Subscription access calls ConversationService.get, including participant
checks before lifecycle checks. SEND calls MessageService.send. Roles grant no
implicit conversation access. PENDING/CONFIRMED and retained COMPLETED/CANCELLED
conversations follow the existing lifecycle policy; BLOCKED and expired conversations
cannot be revived. Authorization does not automatically terminate an existing
subscription on a later lifecycle transition; new subscriptions and sends recheck policy.

## Errors and expiration

Recoverable controller errors go only to the originating session's error queue:
`{"code":"INVALID_MESSAGE","message":"Message content is invalid"}`.
Codes are `INVALID_MESSAGE`, `INVALID_DESTINATION`, `UNAUTHENTICATED`,
`CONVERSATION_NOT_FOUND` (also inaccessible), and `CONVERSATION_UNAVAILABLE`.
CONNECT failures and inbound protocol/destination/subscription security failures
produce a sanitized STOMP ERROR and close the connection. They do not go to other
participants. Clients should handle both ERROR/close and private error messages.

An established session's JWT expiry is checked on every SEND/SUBSCRIBE and every
outbound private MESSAGE. After expiry, delivery is suppressed even for an existing
subscription, and further SEND/SUBSCRIBE closes the connection with
UNAUTHENTICATED. An idle expired socket may stay open but cannot receive private
messages. Obtain a valid JWT and reconnect; there is no STOMP refresh protocol.
Do not log bearer tokens or enable verbose frame/header logging in deployments.

Inbound STOMP frames are bounded to 32 KiB (enough for 4000 characters including
JSON Unicode escapes), outbound buffering to 128 KiB and send time to 10 seconds.
The first frame must arrive within 10 seconds. Oversize/transport failures close
the socket and may occur before an application error can be returned.

## Initial load and reconnect recovery

1. Authenticate, connect and subscribe to messages and errors.
2. Set a unique `receipt` header on SUBSCRIBE and wait for matching RECEIPT.
   The service acknowledges only after the embedded broker installs the subscription.
   SEND receipts are not persistence acknowledgements; use the delivered persisted DTO.
3. Buffer live messages while fetching authenticated REST history:
   `GET /api/v1/conversations/{conversationId}/messages?limit=50`.
4. Merge history and buffered messages by immutable message `id`, deduplicating overlap.
5. When more pages are needed, pass `nextCursor` unchanged as URL-encoded `cursor`
   with the same limit until `hasMore` is false. Limits are 1–100, default 50;
   results are newest first by `(createdAt DESC, id DESC)`. There is no `after` parameter.
6. After disconnect, reconnect and repeat. Traverse older pages until reaching known
   IDs (or the final page), retaining newly buffered live messages during recovery.

Subscribing before history avoids the obvious history-then-subscribe gap. Deduplicate
the intentional overlap. Use persisted IDs/timestamps rather than network arrival
order to reconcile results.

## Delivery, runtime and limitations

MessageService publishes one immutable event for REST and STOMP writes in the same
transaction as message persistence and lastMessageAt. A transactional event listener
runs only AFTER_COMMIT, sends the persisted DTO to the authoritative guest and owner
snapshot, and deduplicates identical recipients. Rollback emits no live message.
Delivery exceptions are logged without content/tokens and do not turn a committed
write into a REST failure. REST history is the recovery mechanism.

The embedded simple broker is a single-instance, best-effort MVP. It has no durable
queues, offline delivery, replay, exactly-once guarantee, outbox or cross-process crash
guarantees. Multiple sessions of one user each receive the message. Run one reservation
service instance; Eureka load balancing does not make this broker multi-instance safe.

Gateway configuration uses Spring Cloud 2025.1.1 / Gateway 5.0.1's
`spring.cloud.gateway.server.webflux.routes`, URI `lb:ws://reservation-service`, exact
path `/ws/conversations`, with no rewrite. Existing REST routes stay unchanged.
The local Config Server files contain no gateway route overrides; if adding remote
route overrides, preserve this route. Compose/Eureka retain `reservation-service`.

## Development smoke test and automated verification

With Compose running, an existing available conversation, and participant tokens:

```powershell
$env:GUEST_TOKEN = '<guest JWT>'
$env:OWNER_TOKEN = '<owner JWT>'
$env:CONVERSATION_ID = '<conversation UUID>'
./scripts/messaging-smoke.ps1
```

The script creates one message through the gateway, asserts both sessions receive the
same persisted ID and verifies it in REST history. It sets a local allowed Origin.
No credentials are stored. It is a development tool outside the product frontend.

```powershell
cd services/reservation-service
./mvnw.cmd '-Dtest=MessagingSessionsTest,StompMessagingIntegrationTest,Conversation*Test,Message*Test' test
cd ../api-gateway
./mvnw.cmd test
```

StompMessagingIntegrationTest uses real WebSocket/STOMP client sessions against the
reservation service with the repository's H2 convention, plus REST history and
transaction rollback checks. MessagingSessionsTest tests destination isolation and
expiration with a controlled clock. ConversationWebSocketGatewayTest boots the actual
gateway route and discovery/load-balancer chain against a local WebSocket echo server;
it verifies upgrade/path/frame forwarding, not a full Compose/PostgreSQL exchange.
The smoke command above covers that deployed combination.

## Acceptance evidence

| Requirement | Automated evidence |
|---|---|
| Guest and owner each send; both receive the stored result | `StompMessagingIntegrationTest.participantsExchangePersistedMessagesAndRecoverThroughRest` checks both recipients, stored sender/content/conversation/ID/timestamp and one row per send |
| Unauthenticated or unrelated clients cannot access conversations | Real-client bad-token, pre-CONNECT frame, origin, subscription and SEND rejection tests; `MessagingSessionsTest` checks exact paths, identity replacement and established-session expiration |
| REST and STOMP share durable persistence and validation | Shared `MessageService.send`; real-client REST publication, invalid content, maximum escaped content and rollback tests; existing `MessageIntegrationTest` lifecycle/pagination coverage |
| Reconnecting clients recover through REST | The real-client reconnect test verifies history includes the message written while disconnected; cursor traversal and deduplication are documented above |
| Gateway/runtime and integration material | `ConversationWebSocketGatewayTest` exercises the production load-balanced route and native upgrade/frame exchange; `scripts/messaging-smoke.ps1` exercises both participants and REST against an available Compose runtime |
