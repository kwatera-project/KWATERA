import { test } from "node:test";
import assert from "node:assert/strict";
import { MESSAGE_LIMIT, mergeMessages, normalizeMessage } from "../src/utils/conversationMessages.ts";

function message(id, createdAt, content = id) {
    return { id, createdAt, content, conversationId: "conversation", senderUserId: "guest" };
}

test("history, refresh and persisted sends merge without duplicates or input mutation", () => {
    const first = message("a", "2026-10-28T10:00:00Z");
    const second = message("b", "2026-10-28T10:01:00Z");
    const current = [second];
    const incoming = [second, first];
    assert.deepEqual(mergeMessages(current, incoming), [first, second]);
    assert.deepEqual(current, [second]);
    assert.deepEqual(incoming, [second, first]);
    assert.deepEqual(mergeMessages(mergeMessages(current, incoming), [second]), [first, second]);
});

test("ordering preserves microseconds before using the backend UUID tie break", () => {
    const earlier = message("ffffffff", "2026-10-28T10:00:00.123001Z");
    const later = message("00000000", "2026-10-28T10:00:00.123002Z");
    const tied = message("00000001", "2026-10-28T10:00:00.123002Z");
    assert.deepEqual(mergeMessages([], [tied, later, earlier]), [earlier, later, tied]);
});

test("variable ISO fractional precision sorts as instants", () => {
    const whole = message("z", "2026-10-28T10:00:00Z");
    const fraction = message("a", "2026-10-28T10:00:00.000001Z");
    const next = message("b", "2026-10-28T10:00:01Z");
    assert.deepEqual(mergeMessages([], [next, fraction, whole]), [whole, fraction, next]);
});

test("normalization matches Java strip while preserving internal text", () => {
    assert.equal(normalizeMessage("\t \nHello  there\n  second line\t \n"), "Hello  there\n  second line");
    assert.equal(normalizeMessage("\u001c\u1680\u2000\u3000"), "");
    assert.equal(normalizeMessage("\u00a0hello\u2007\u202f"), "\u00a0hello\u2007\u202f");
    assert.equal(normalizeMessage("\ufeffhello\ufeff"), "\ufeffhello\ufeff");
});

test("content limits count UTF-16 units like Java", () => {
    assert.equal(normalizeMessage(" \n" + "x".repeat(MESSAGE_LIMIT) + "\t").length, MESSAGE_LIMIT);
    assert.ok(normalizeMessage("x".repeat(MESSAGE_LIMIT + 1)).length > MESSAGE_LIMIT);
    assert.equal("😀".repeat(2000).length, MESSAGE_LIMIT);
    assert.equal(normalizeMessage(" \t\n"), "");
});
