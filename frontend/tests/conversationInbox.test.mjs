import { test } from "node:test";
import assert from "node:assert/strict";
import { canOpenReservationConversation, conversationAccessError, conversationActivity, sortConversations } from "../src/utils/conversationInbox.ts";

test("admin reservation actions require known participation, including mixed roles", () => {
    for (const roles of [["ROLE_ADMIN"], ["ROLE_ADMIN", "ROLE_OWNER"], ["ROLE_ADMIN", "ROLE_GUEST"]]) {
        assert.equal(canOpenReservationConversation(roles, "admin", "other"), false);
        assert.equal(canOpenReservationConversation(roles, "admin"), false);
        assert.equal(canOpenReservationConversation(roles, "admin", "admin"), true);
    }
    assert.equal(canOpenReservationConversation(["ROLE_ADMIN"], undefined, "admin"), false);
    assert.equal(canOpenReservationConversation([], "user"), false);
    for (const roles of [["ROLE_GUEST"], ["ROLE_OWNER"]]) assert.equal(canOpenReservationConversation(roles, "user"), true);
});

test("inbox sorting preserves backend sub-millisecond precision before UUID ordering", () => {
    const earlier = { id: "a", lastMessageAt: "2026-10-30T10:00:00.123001Z", createdAt: "2026-09-01T00:00:00Z" };
    const later = { ...earlier, id: "z", lastMessageAt: "2026-10-30T10:00:00.123002Z" };
    assert.deepEqual(sortConversations([earlier, later]), [later, earlier]);
    const createdLater = { ...later, id: "zz", lastMessageAt: null, createdAt: "2026-10-30T10:00:00.123002001Z" };
    assert.deepEqual(sortConversations([earlier, later, createdLater]), [createdLater, later, earlier]);
});

test("fraction padding and timezone offsets keep equal activity instants tied", () => {
    const rows = [
        { id: "c", lastMessageAt: "2026-10-30T11:00:00.100000+01:00" },
        { id: "b", lastMessageAt: "2026-10-30T10:00:00.1Z" },
        { id: "a", lastMessageAt: "2026-10-30T10:00:00.100Z" },
    ];
    assert.deepEqual(sortConversations(rows).map(row => row.id), ["a", "b", "c"]);
});

test("inbox and entry failures distinguish session, access, lifecycle and retryable errors", () => {
    assert.equal(conversationAccessError(401), "unauthenticated");
    for (const status of [403, 404]) assert.equal(conversationAccessError(status), "inaccessible");
    for (const status of [409, 410]) assert.equal(conversationAccessError(status), "unavailable");
    for (const status of [null, 400, 500, 503]) assert.equal(conversationAccessError(status), null);
});

test("activity uses the last message, then creation, then a missing-date fallback", () => {
    assert.equal(conversationActivity({ lastMessageAt: "2026-10-30T10:00:00Z", createdAt: "2026-10-01T10:00:00Z" }), "2026-10-30T10:00:00Z");
    assert.equal(conversationActivity({ lastMessageAt: "invalid", createdAt: "2026-10-01T10:00:00Z" }), "2026-10-01T10:00:00Z");
    assert.equal(conversationActivity({ lastMessageAt: null, createdAt: null }), null);
});

test("sorting is newest first, deterministic for ties, preserves inputs and puts missing dates last", () => {
    const rows = [
        { id: "missing", lastMessageAt: null, createdAt: "invalid" },
        { id: "old", lastMessageAt: null, createdAt: "2026-10-01T10:00:00Z" },
        { id: "b", lastMessageAt: "2026-10-30T10:00:00Z", createdAt: "2026-09-01T10:00:00Z" },
        { id: "a", lastMessageAt: null, createdAt: "2026-10-30T10:00:00Z" },
    ];
    assert.deepEqual(sortConversations(rows).map(row => row.id), ["a", "b", "old", "missing"]);
    assert.equal(rows[0].id, "missing");
});
