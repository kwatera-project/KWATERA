import { test } from "node:test";
import assert from "node:assert/strict";
import { createConversationSession } from "../src/api/conversationSession.ts";

const flush = () => new Promise(resolve => setImmediate(resolve));
const deferred = () => { let resolve, reject; const promise = new Promise((yes, no) => { resolve = yes; reject = no; }); return { promise, resolve, reject }; };
const conversation = { id: "chat", guestUserId: "guest", ownerUserId: "owner", reservationId: "reservation" };
const message = (id, extra = {}) => ({ id, conversationId: "chat", senderUserId: "guest", content: id, createdAt: `2026-10-28T10:00:${id.padStart(2, "0")}Z`, ...extra });
const page = (items, nextCursor = null) => ({ items, hasMore: nextCursor !== null, nextCursor });

function harness(t, overrides = {}) {
    let state, callbacks, stopped = 0, current = true;
    const calls = [];
    const options = {
        conversationId: "chat", userId: "guest", isCurrent: () => current,
        onChange: value => { state = value; },
        getConversation: async () => conversation,
        getReservation: async () => ({ unitName: "Stay" }),
        getHistory: async (id, params, signal) => { calls.push({ id, params, signal }); return page([message("01")]); },
        sendMessage: async (_id, data) => message("02", data),
        connect: value => { callbacks = value; return () => { stopped++; }; },
        ...overrides,
    };
    const session = createConversationSession(options);
    t.after(session.stop);
    return { session, calls, get state() { return state; }, get socket() { return callbacks; },
        get stopped() { return stopped; }, invalidate: () => { current = false; } };
}

test("history waits for readiness, buffers initial live deliveries and deduplicates POST echoes", async t => {
    const history = deferred();
    const h = harness(t, { getHistory: () => history.promise });
    await h.session.start();
    assert.equal(h.state.loading, true);
    h.socket.onReady();
    h.socket.onMessage(message("03"));
    h.socket.onMessage(message("01"));
    h.socket.onMessage(message("08", { conversationId: "other" }));
    assert.deepEqual(h.state.messages, []);
    history.resolve(page([message("01")]));
    await flush();
    assert.deepEqual(h.state.messages.map(m => m.id), ["01", "03"]);
    h.socket.onMessage(message("02"));
    assert.equal(await h.session.send("sent through REST"), true);
    h.socket.onMessage(message("02"));
    assert.deepEqual(h.state.messages.map(m => m.id), ["01", "02", "03"]);
});

test("recovery traverses every cursor to the REST anchor while retaining overlapping live messages", async t => {
    const requests = [], recovery = deferred();
    let round = 0;
    const h = harness(t, { getHistory: async (_id, params) => {
        requests.push(params);
        if (round++ === 0) return page([message("01")], "older");
        if (!params.cursor) return recovery.promise;
        if (params.cursor === "opaque +/==") return page([message("03"), message("02")], "last");
        return page([message("01"), message("00")]);
    } });
    await h.session.start(); h.socket.onReady(); await flush();
    h.socket.onStatus("disconnected");
    h.socket.onMessage(message("04")); // A live ID must not replace the completed REST anchor.
    h.socket.onReady();
    h.socket.onMessage(message("06"));
    h.socket.onMessage(message("03"));
    recovery.resolve(page([message("05"), message("04")], "opaque +/=="));
    await flush();
    assert.deepEqual(requests, [{ limit: 50 }, { limit: 50 }, { limit: 50, cursor: "opaque +/==" }, { limit: 50, cursor: "last" }]);
    assert.deepEqual(h.state.messages.map(m => m.id), ["00", "01", "02", "03", "04", "05", "06"]);
    assert.equal(h.state.nextCursor, null);
});

test("recovery stops at a known anchor and preserves the older-history cursor", async t => {
    let round = 0;
    const h = harness(t, { getHistory: async (_id, params) => {
        if (params.cursor === "older") return page([message("00")]);
        return round++ === 0 ? page([message("01")], "older") : page([message("02"), message("01")], "overlap");
    } });
    await h.session.start(); h.socket.onReady(); await flush();
    h.socket.onReady(); await flush();
    assert.equal(h.state.nextCursor, "older");
    await h.session.loadHistory("older");
    assert.deepEqual(h.state.messages.map(m => m.id), ["00", "01", "02"]);
    assert.equal(h.state.nextCursor, null);
});

test("unavailable WebSocket permits REST history and sending, without retrying an uncertain POST", async t => {
    let sends = 0;
    const h = harness(t, { sendMessage: async () => { sends++; throw new Error("network"); } });
    await h.session.start();
    h.socket.onStatus("transportError"); await flush();
    assert.equal(h.state.loading, false);
    assert.deepEqual(h.state.messages.map(m => m.id), ["01"]);
    assert.equal(await h.session.send("hello"), false);
    await flush();
    assert.equal(sends, 1);
    assert.equal(h.state.operationError, "send");
});

test("a silent socket cannot leave history loading indefinitely", async t => {
    t.mock.timers.enable({ apis: ["setTimeout"] });
    const h = harness(t);
    await h.session.start();
    assert.equal(h.calls.length, 0);
    t.mock.timers.tick(2000); await flush();
    assert.equal(h.state.loading, false);
    assert.equal(h.calls.length, 1);
});

test("readiness during fallback loading triggers another snapshot after that request", async t => {
    const fallback = deferred(); let calls = 0;
    const h = harness(t, { getHistory: async () => ++calls === 1 ? fallback.promise : page([message("02"), message("01")]) });
    await h.session.start(); h.socket.onStatus("transportError");
    h.socket.onReady(); h.socket.onMessage(message("03"));
    fallback.resolve(page([message("01")])); await flush();
    assert.equal(calls, 2);
    assert.deepEqual(h.state.messages.map(m => m.id), ["01", "02", "03"]);
});

for (const [code, fatalError] of [["UNAUTHENTICATED", "unauthenticated"], ["CONVERSATION_NOT_FOUND", "inaccessible"], ["CONVERSATION_UNAVAILABLE", "unavailable"]]) {
    test(`${code} clears messages and metadata, aborts requests and stops the session`, async t => {
        const h = harness(t);
        await h.session.start(); h.socket.onReady(); await flush();
        h.socket.onError(code);
        assert.equal(h.state.fatalError, fatalError);
        assert.equal(h.state.conversation, null); assert.equal(h.state.reservation, null);
        assert.deepEqual(h.state.messages, []);
        assert.equal(h.calls[0].signal.aborted, true);
        assert.equal(h.stopped, 1);
        h.socket.onMessage(message("09"));
        assert.deepEqual(h.state.messages, []);
    });
}

test("private invalid-message errors keep access; rejected subscriptions keep REST usable", async t => {
    const h = harness(t);
    await h.session.start(); h.socket.onError("INVALID_DESTINATION"); await flush();
    assert.equal(h.state.connectionStatus, "subscriptionError");
    assert.equal(h.state.loading, false);
    h.socket.onError("INVALID_MESSAGE");
    assert.equal(h.state.operationError, "invalidMessage");
    assert.equal(h.state.conversation.id, "chat");
});

test("account switches and teardown discard pending history, context, sends and callbacks", async t => {
    const history = deferred(), context = deferred(), send = deferred();
    const h = harness(t, { getHistory: () => history.promise, getReservation: () => context.promise, sendMessage: () => send.promise });
    await h.session.start(); h.socket.onReady();
    const pendingSend = h.session.send("hello");
    const previous = h.state;
    h.invalidate(); h.session.stop();
    history.resolve(page([message("01")])); context.resolve({ unitName: "private" }); send.resolve(message("02"));
    assert.equal(await pendingSend, false); await flush();
    h.socket.onMessage(message("03")); h.socket.onError("INVALID_MESSAGE"); h.socket.onReady();
    assert.equal(h.state, previous);
});

test("switching conversations/Strict Mode cleanup ignores an earlier metadata request", async t => {
    const details = deferred(); let connects = 0;
    const h = harness(t, { getConversation: () => details.promise, connect: () => { connects++; return () => {}; } });
    const pending = h.session.start(); h.session.stop();
    details.resolve(conversation); await pending;
    assert.equal(connects, 0);
    assert.equal(h.state, undefined);
    const next = harness(t); await next.session.start(); next.socket.onReady(); await flush();
    assert.equal(next.state.loading, false);
});

test("roles cannot bypass participation and REST access failures discard loaded history", async t => {
    const other = harness(t, { userId: "admin" }); await other.session.start();
    assert.equal(other.state.fatalError, "inaccessible"); assert.equal(other.socket, undefined);
    let fail = false;
    const h = harness(t, { getHistory: async () => { if (fail) throw { status: 403 }; return page([message("01")]); } });
    await h.session.start(); h.socket.onReady(); await flush();
    fail = true; await h.session.loadHistory("refresh");
    assert.equal(h.state.fatalError, "inaccessible"); assert.deepEqual(h.state.messages, []);
});

test("recovery detects repeated cursors and keeps already loaded messages", async t => {
    let round = 0;
    const h = harness(t, { getHistory: async () => round++ === 0 ? page([message("01")]) : page([message("03")], "repeat") });
    await h.session.start(); h.socket.onReady(); await flush();
    h.socket.onReady(); await flush();
    assert.equal(h.state.operationError, "refresh");
    assert.deepEqual(h.state.messages.map(m => m.id), ["01"]);
});
