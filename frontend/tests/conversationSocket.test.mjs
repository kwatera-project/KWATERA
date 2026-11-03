import { test } from "node:test";
import assert from "node:assert/strict";
import { ReconnectionTimeMode } from "@stomp/stompjs";
import { connectConversationSocket, conversationSocketUrl } from "../src/api/conversationSocket.ts";

function harness(t, options = {}) {
    let config, activated = 0, deactivated = 0, unsubscribed = 0, forced = 0, ready = 0;
    const subscriptions = [], receipts = new Map(), statuses = [], errors = [], messages = [];
    const client = {
        connected: true,
        configure: value => { config = value; }, activate: () => { activated++; },
        deactivate: async () => { deactivated++; }, forceDisconnect: () => { forced++; },
        watchForReceipt: (id, callback) => receipts.set(id, callback),
        subscribe: (destination, callback, headers) => {
            subscriptions.push({ destination, callback, headers });
            return { unsubscribe: () => { unsubscribed++; } };
        },
    };
    const stop = connectConversationSocket({
        url: "wss://gateway.example/ws/conversations", conversationId: "chat", token: "secret",
        isCurrent: () => true, onStatus: value => statuses.push(value), onReady: () => { ready++; },
        onError: value => errors.push(value), onMessage: value => messages.push(value), ...options,
    }, client);
    t.after(stop);
    return { stop, get config() { return config; }, receipts, subscriptions, statuses, errors, messages,
        get ready() { return ready; }, get activated() { return activated; }, get deactivated() { return deactivated; },
        get unsubscribed() { return unsubscribed; }, get forced() { return forced; } };
}

test("gateway URLs convert HTTP/HTTPS, preserve prefixes and contain no query or fragment", () => {
    assert.equal(conversationSocketUrl("https://example.com/gateway/", "http://localhost"), "wss://example.com/gateway/ws/conversations");
    assert.equal(conversationSocketUrl("http://localhost:8090", "http://localhost"), "ws://localhost:8090/ws/conversations");
    assert.equal(conversationSocketUrl("", "https://example.com"), "wss://example.com/ws/conversations");
    assert.equal(conversationSocketUrl("https://example.com/gateway?legacy=1#section", "http://localhost"), "wss://example.com/gateway/ws/conversations");
});

test("JWT is only in CONNECT; STOMP 1.2 and one bounded exponential reconnect manager are configured", t => {
    const h = harness(t);
    assert.equal(h.activated, 1);
    assert.deepEqual(h.config.connectHeaders, { Authorization: "Bearer secret", host: "gateway.example" });
    assert.equal(h.config.stompVersions.supportedVersions(), "1.2");
    assert.equal(h.config.brokerURL.includes("secret"), false);
    assert.equal(h.config.reconnectTimeMode, ReconnectionTimeMode.EXPONENTIAL);
    assert.equal(h.config.reconnectDelay, 1000); assert.equal(h.config.maxReconnectDelay, 30000);
    h.config.beforeConnect(); h.config.onWebSocketError(); h.config.onWebSocketClose(); h.config.beforeConnect();
    assert.deepEqual(h.statuses, ["connecting", "transportError", "disconnected", "reconnecting"]);
});

test("only exact active/private destinations are subscribed and only a matching unique receipt enables readiness", t => {
    const h = harness(t); h.config.onConnect();
    assert.deepEqual(h.subscriptions.map(s => s.destination), ["/user/queue/messaging-errors", "/user/queue/conversations/chat/messages"]);
    const receipt = h.subscriptions[1].headers.receipt;
    assert.equal(h.receipts.has("send-receipt"), false);
    assert.equal(h.ready, 0);
    h.receipts.get(receipt)(); h.receipts.get(receipt)();
    assert.equal(h.ready, 1); assert.equal(h.statuses.at(-1), "connected");
    h.config.onWebSocketClose(); h.config.onConnect();
    assert.notEqual(h.subscriptions[3].headers.receipt, receipt);
    h.receipts.get(receipt)(); assert.equal(h.ready, 1);
    h.receipts.get(h.subscriptions[3].headers.receipt)(); assert.equal(h.ready, 2);
});

test("valid persisted DTOs are delivered; malformed and other-conversation messages are ignored", t => {
    const h = harness(t); h.config.onConnect();
    const deliver = body => h.subscriptions[1].callback({ body });
    const message = { id: "m", conversationId: "chat", senderUserId: "guest", content: "Hello", createdAt: "2026-10-28T12:00:00.123456Z" };
    deliver(JSON.stringify(message)); deliver(JSON.stringify({ ...message, conversationId: "other" }));
    deliver("invalid"); deliver("null"); deliver(JSON.stringify({ conversationId: "chat" }));
    assert.deepEqual(h.messages, [message]);
    h.stop(); deliver(JSON.stringify(message)); assert.equal(h.messages.length, 1);
    assert.equal(h.unsubscribed, 2); assert.equal(h.deactivated, 1);
});

for (const code of ["UNAUTHENTICATED", "CONVERSATION_NOT_FOUND", "CONVERSATION_UNAVAILABLE", "INVALID_DESTINATION"]) {
    test(`STOMP ERROR ${code} stops retries and ignores late close/receipt callbacks`, t => {
        const h = harness(t); h.config.onConnect();
        h.config.onStompError({ body: JSON.stringify({ code }), headers: {} });
        const status = h.statuses.at(-1);
        h.config.onWebSocketClose(); h.config.beforeConnect();
        h.receipts.values().next().value();
        assert.equal(h.ready, 0); assert.equal(h.deactivated, 1);
        assert.deepEqual(h.errors, [code]);
        assert.equal(status, code === "UNAUTHENTICATED" ? "authenticationError" : "subscriptionError");
        assert.equal(h.statuses.at(-1), status);
    });
}

test("private INVALID_MESSAGE is recoverable; private authentication failure ends the session", t => {
    const h = harness(t); h.config.onConnect();
    const error = code => h.subscriptions[0].callback({ body: JSON.stringify({ code }), headers: {} });
    error("INVALID_MESSAGE"); assert.equal(h.deactivated, 0);
    error("UNAUTHENTICATED"); assert.equal(h.deactivated, 1);
    assert.deepEqual(h.errors, ["INVALID_MESSAGE", "UNAUTHENTICATED"]);
});

test("an invalidated or expired session cannot activate a socket", t => {
    const h = harness(t, { isCurrent: () => false });
    assert.equal(h.activated, 0);
    assert.deepEqual(h.errors, []); // The conversation session owns expiration and its UI state.
});

test("receipt timeout forces transport recovery and teardown cancels all pending timers", t => {
    t.mock.timers.enable({ apis: ["setTimeout"] });
    const h = harness(t); h.config.onConnect();
    t.mock.timers.tick(8000); assert.equal(h.forced, 1);
    h.config.onWebSocketClose(); h.config.onConnect();
    h.stop(); t.mock.timers.tick(60000);
    assert.equal(h.forced, 1); assert.equal(h.deactivated, 1);
});

test("a changed authentication session cannot reconnect with the old JWT", t => {
    let current = true;
    const h = harness(t, { isCurrent: () => current });
    h.config.onConnect(); current = false; h.config.beforeConnect();
    assert.equal(h.deactivated, 1); assert.equal(h.ready, 0);
    assert.deepEqual(h.errors, []);
});
