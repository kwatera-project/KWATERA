import { test } from "node:test";
import assert from "node:assert/strict";
import { canUseConversations, conversationAccessError, conversationActivity, sortConversations } from "../src/utils/conversationInbox.ts";

test("only guest and owner roles enable conversation navigation, including mixed roles", () => {
    for (const roles of [[], ["ROLE_ADMIN"], ["ROLE_USER"]]) assert.equal(canUseConversations(roles), false);
    for (const roles of [["ROLE_GUEST"], ["ROLE_OWNER"], ["ROLE_ADMIN", "ROLE_OWNER"], ["ROLE_ADMIN", "ROLE_GUEST"]]) {
        assert.equal(canUseConversations(roles), true);
    }
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
