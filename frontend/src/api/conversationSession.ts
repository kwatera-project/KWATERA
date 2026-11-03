import type { ConversationResponse, MessageHistoryParams, MessageHistoryResponse, MessageResponse, SendMessageRequest } from "../types/conversation";
import type { ReservationDetails } from "../types/reservation";
import type { ConnectionStatus } from "./conversationSocket";
import { mergeMessages } from "../utils/conversationMessages.ts";

type AccessError = "unauthenticated" | "inaccessible" | "unavailable";
type Operation = "refresh" | "older" | "send";

export interface ConversationState {
    conversation: ConversationResponse | null;
    reservation: ReservationDetails | null;
    contextLoading: boolean;
    messages: MessageResponse[];
    nextCursor: string | null;
    loading: boolean;
    fatalError: AccessError | "load" | null;
    operation: Operation | null;
    operationError: Operation | "invalidMessage" | null;
    connectionStatus: ConnectionStatus;
}

export const emptyConversation: ConversationState = {
    conversation: null, reservation: null, contextLoading: false, messages: [], nextCursor: null,
    loading: true, fatalError: null, operation: null, operationError: null, connectionStatus: "connecting",
};

interface SessionOptions {
    conversationId: string;
    userId: string | undefined;
    isCurrent: () => boolean;
    onChange: (state: ConversationState) => void;
    getConversation: (id: string, signal: AbortSignal) => Promise<ConversationResponse>;
    getReservation: (id: string) => Promise<ReservationDetails>;
    getHistory: (id: string, params: MessageHistoryParams, signal: AbortSignal) => Promise<MessageHistoryResponse>;
    sendMessage: (id: string, data: SendMessageRequest, signal: AbortSignal) => Promise<MessageResponse>;
    connect: (callbacks: {
        onStatus: (status: ConnectionStatus) => void;
        onReady: () => void;
        onMessage: (message: MessageResponse) => void;
        onError: (code: string) => void;
    }) => () => void;
}

function accessError(error: unknown): AccessError | null {
    const status = error && typeof error === "object" && "status" in error ? error.status : null;
    if (status === 401) return "unauthenticated";
    if (status === 403 || status === 404) return "inaccessible";
    if (status === 409 || status === 410) return "unavailable";
    return null;
}

// One synchronization scope per conversation and JWT. Only the socket manages retries.
export function createConversationSession(options: SessionOptions) {
    let state = { ...emptyConversation };
    let active = true;
    let stopSocket = () => {};
    let fallbackTimer: ReturnType<typeof setTimeout> | undefined;
    const requests = new AbortController();
    const current = () => active && options.isCurrent();
    const history = (params: MessageHistoryParams = {}) => options.getHistory(options.conversationId, { limit: 50, ...params }, requests.signal);
    let anchor: string | null = null;
    let initialized = false;
    let syncPromise: Promise<void> | null = null;
    let syncAgain = false;
    let buffered: MessageResponse[] = [];
    let busy = false;

    function update(patch: Partial<ConversationState>) {
        if (!current()) return;
        state = { ...state, ...patch };
        options.onChange(state);
    }

    function stop() {
        active = false;
        clearTimeout(fallbackTimer);
        stopSocket();
        requests.abort();
        buffered = [];
    }

    function deny(error: AccessError) {
        update({ conversation: null, reservation: null, messages: [], nextCursor: null,
            fatalError: error, loading: false, operation: null,
            connectionStatus: error === "unauthenticated" ? "authenticationError" : "subscriptionError" });
        stop();
    }

    function failure(error: unknown, action: Operation) {
        const fatal = accessError(error);
        if (fatal) deny(fatal);
        else update({ operationError: action === "send" && (error as { status?: number })?.status === 400 ? "invalidMessage" : action });
    }

    function merge(incoming: MessageResponse[]) {
        return mergeMessages(state.messages, incoming.filter(message => message.conversationId === options.conversationId));
    }

    function synchronize(): Promise<void> {
        if (!current()) return Promise.resolve();
        if (syncPromise) { syncAgain = true; return syncPromise; }
        syncPromise = (async () => {
            do {
                syncAgain = false;
                try {
                    let page = await history();
                    if (!current()) return;
                    const newestId = page.items[0]?.id ?? null;
                    const incoming = [...page.items];
                    const cursors = new Set<string>();
                    // Live echoes and POST results cannot prove that preceding messages were received.
                    // The recovery anchor is always the last completed REST snapshot.
                    while (initialized && page.hasMore && page.nextCursor && !page.items.some(message => message.id === anchor)) {
                        if (cursors.has(page.nextCursor)) throw new Error("Repeated history cursor");
                        cursors.add(page.nextCursor);
                        page = await history({ cursor: page.nextCursor });
                        if (!current()) return;
                        incoming.push(...page.items);
                    }
                    if (!current()) return;
                    const nextCursor = !initialized ? (page.hasMore ? page.nextCursor : null)
                        : !page.hasMore ? null : state.nextCursor;
                    anchor = newestId;
                    initialized = true;
                    update({ messages: merge([...incoming, ...buffered]), nextCursor, loading: false, fatalError: null, operationError: null });
                    buffered = [];
                } catch (error) {
                    if (!current()) return;
                    if (!initialized) {
                        const fatal = accessError(error);
                        if (fatal) { deny(fatal); return; }
                        update({ fatalError: "load", loading: false });
                    } else failure(error, "refresh");
                    update({ messages: merge(buffered) });
                    buffered = [];
                    syncAgain = false;
                }
            } while (syncAgain && current());
        })().finally(() => { syncPromise = null; });
        return syncPromise;
    }

    async function start() {
        try {
            if (!options.userId) { deny("unauthenticated"); return; }
            const conversation = await options.getConversation(options.conversationId, requests.signal);
            if (!current()) return;
            if (conversation.id !== options.conversationId
                || (options.userId !== conversation.guestUserId && options.userId !== conversation.ownerUserId)) {
                deny("inaccessible"); return;
            }
            update({ conversation });
            void options.getReservation(conversation.reservationId).then(reservation => {
                update({ reservation });
            }).catch(() => { /* Optional context must not prevent messaging. */ });
            stopSocket = options.connect({
                onStatus: connectionStatus => {
                    update({ connectionStatus });
                    if (connectionStatus === "transportError" || connectionStatus === "disconnected") {
                        clearTimeout(fallbackTimer);
                        if (!initialized) void synchronize();
                    }
                },
                onReady: () => {
                    clearTimeout(fallbackTimer);
                    void synchronize();
                },
                onMessage: message => {
                    if (!current() || message.conversationId !== options.conversationId) return;
                    if (syncPromise || !initialized) buffered.push(message);
                    else update({ messages: merge([message]) });
                },
                onError: code => {
                    if (code === "UNAUTHENTICATED") deny("unauthenticated");
                    else if (code === "CONVERSATION_NOT_FOUND") deny("inaccessible");
                    else if (code === "CONVERSATION_UNAVAILABLE") deny("unavailable");
                    else if (code === "INVALID_MESSAGE") update({ operationError: "invalidMessage" });
                    else {
                        update({ connectionStatus: "subscriptionError" });
                        clearTimeout(fallbackTimer);
                        if (!initialized) void synchronize();
                    }
                },
            });
            // An already expired JWT can reject synchronously while creating the socket.
            if (!current()) { stopSocket(); return; }
            fallbackTimer = setTimeout(() => { if (!initialized) void synchronize(); }, 2000);
        } catch (error) {
            if (!current()) return;
            const fatal = accessError(error);
            if (fatal) deny(fatal);
            else update({ fatalError: "load", loading: false });
        }
    }

    async function loadHistory(action: "refresh" | "older") {
        if (!current() || busy || !state.conversation || (action === "older" && !state.nextCursor)) return;
        busy = true;
        update({ operation: action, operationError: null });
        try {
            if (action === "refresh") await synchronize();
            else {
                const cursor = state.nextCursor;
                const page = await history({ cursor });
                if (!current()) return;
                // Recovery can already have reached the end of older history.
                update({ messages: merge(page.items), nextCursor: state.nextCursor === cursor ? (page.hasMore ? page.nextCursor : null) : state.nextCursor });
            }
        } catch (error) { if (current()) failure(error, action); }
        finally { busy = false; update({ operation: null }); }
    }

    async function send(content: string): Promise<boolean> {
        if (!current() || busy || !state.conversation) return false;
        busy = true;
        update({ operation: "send", operationError: null });
        try {
            const message = await options.sendMessage(options.conversationId, { content }, requests.signal);
            if (!current() || message.conversationId !== options.conversationId) return false;
            update({ messages: merge([message]) });
            return true;
        } catch (error) { if (current()) failure(error, "send"); return false; }
        finally { busy = false; update({ operation: null }); }
    }

    async function retryContext() {
        if (!current() || !state.conversation || state.contextLoading) return;
        update({ contextLoading: true });
        try { update({ reservation: await options.getReservation(state.conversation.reservationId) }); }
        catch { update({ reservation: null }); }
        finally { update({ contextLoading: false }); }
    }

    return { start, stop, loadHistory, send, retryContext };
}
