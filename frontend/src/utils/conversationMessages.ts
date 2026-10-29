import type { MessageResponse } from "../types/conversation";

export const MESSAGE_LIMIT = 4000;

function isMessageWhitespace(code: number): boolean {
    return (code >= 0x0009 && code <= 0x000D)
        || (code >= 0x001C && code <= 0x0020)
        || code === 0x1680
        || (code >= 0x2000 && code <= 0x2006)
        || (code >= 0x2008 && code <= 0x200A)
        || code === 0x2028 || code === 0x2029
        || code === 0x205F || code === 0x3000;
}

export function normalizeMessage(content: string): string {
    let start = 0;
    let end = content.length;
    while (start < end && isMessageWhitespace(content.charCodeAt(start))) start++;
    while (end > start && isMessageWhitespace(content.charCodeAt(end - 1))) end--;
    return content.slice(start, end);
}

function timestampKey(value: string): string {
    return value.replace(/(?:\.(\d+))?Z$/, (_, fraction: string = "") => `.${fraction.padEnd(9, "0")}Z`);
}

export function mergeMessages(current: MessageResponse[], incoming: MessageResponse[]): MessageResponse[] {
    const byId = new Map(current.map(message => [message.id, message]));
    incoming.forEach(message => byId.set(message.id, message));
    return [...byId.values()].sort((left, right) => {
        const leftTime = timestampKey(left.createdAt);
        const rightTime = timestampKey(right.createdAt);
        if (leftTime !== rightTime) return leftTime < rightTime ? -1 : 1;
        return left.id < right.id ? -1 : left.id > right.id ? 1 : 0;
    });
}
