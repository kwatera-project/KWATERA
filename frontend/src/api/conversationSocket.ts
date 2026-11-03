import { Client, ReconnectionTimeMode, Versions, type IFrame } from "@stomp/stompjs";
import type { MessageResponse } from "../types/conversation";

export type ConnectionStatus = "connecting" | "connected" | "disconnected" | "reconnecting"
    | "authenticationError" | "subscriptionError" | "transportError";

export function conversationSocketUrl(gateway: string, origin: string): string {
    const url = new URL(gateway || "/", origin);
    url.pathname = `${url.pathname.replace(/\/$/, "")}/ws/conversations`;
    url.protocol = url.protocol === "https:" ? "wss:" : "ws:";
    url.search = "";
    url.hash = "";
    return url.href;
}

interface SocketOptions {
    url: string;
    conversationId: string;
    token: string;
    expiresAt?: number;
    isCurrent: () => boolean;
    onStatus: (status: ConnectionStatus) => void;
    onReady: () => void;
    onMessage: (message: MessageResponse) => void;
    onError: (code: string) => void;
}

function errorCode(frame: IFrame): string {
    try {
        const value: unknown = JSON.parse(frame.body);
        if (value && typeof value === "object" && "code" in value && typeof value.code === "string") return value.code;
    } catch { /* A protocol ERROR may have no JSON body. */ }
    return frame.headers.message || "INVALID_DESTINATION";
}

// STOMP owns reconnection. This adapter only manages readiness, expiry and session guards.
export function connectConversationSocket(options: SocketOptions, client: Client = new Client()) {
    let stopped = false;
    let attempted = false;
    let generation = 0;
    let receiptTimer: ReturnType<typeof setTimeout> | undefined;
    let expiryTimer: ReturnType<typeof setTimeout> | undefined;
    const subscriptions: ReturnType<Client["subscribe"]>[] = [];
    const current = () => !stopped && options.isCurrent();
    const clearReceiptTimer = () => clearTimeout(receiptTimer);

    function stop() {
        if (stopped) return;
        stopped = true;
        generation++;
        clearReceiptTimer();
        clearTimeout(expiryTimer);
        if (client.connected) subscriptions.forEach(subscription => {
            try { subscription.unsubscribe(); }
            catch { /* A transport closing concurrently must not prevent the remaining cleanup. */ }
        });
        subscriptions.length = 0;
        // Force close also cancels library reconnect/heartbeat timers, even without DISCONNECT receipts.
        void client.deactivate({ force: true });
    }

    function fail(code: string) {
        if (!current()) return;
        if (code === "INVALID_MESSAGE") {
            options.onError(code);
            return;
        }
        options.onStatus(code === "UNAUTHENTICATED" ? "authenticationError" : "subscriptionError");
        stop();
        options.onError(code);
    }

    function checkExpiry() {
        clearTimeout(expiryTimer);
        if (!current()) return;
        if (options.expiresAt === undefined) return;
        const remaining = options.expiresAt - Date.now();
        if (remaining <= 0) fail("UNAUTHENTICATED");
        else expiryTimer = setTimeout(checkExpiry, Math.min(remaining, 2_147_483_647));
    }

    client.configure({
        brokerURL: options.url,
        stompVersions: new Versions(["1.2"]),
        connectHeaders: { Authorization: `Bearer ${options.token}`, host: new URL(options.url).hostname },
        reconnectDelay: 1000,
        maxReconnectDelay: 30_000,
        reconnectTimeMode: ReconnectionTimeMode.EXPONENTIAL,
        connectionTimeout: 8000,
        heartbeatIncoming: 10_000,
        heartbeatOutgoing: 10_000,
        discardWebsocketOnCommFailure: true,
        debug: () => {},
        logRawCommunication: false,
        beforeConnect: () => {
            if (!current()) { stop(); return; }
            checkExpiry();
            if (!current()) return;
            options.onStatus(attempted ? "reconnecting" : "connecting");
            attempted = true;
        },
        onConnect: () => {
            if (!current()) { stop(); return; }
            const installedGeneration = ++generation;
            const valid = () => current() && generation === installedGeneration;
            const receipt = `conversation-${crypto.randomUUID()}`;
            let ready = false;
            clearReceiptTimer();
            subscriptions.length = 0;
            client.watchForReceipt(receipt, () => {
                if (!valid() || ready) return;
                ready = true;
                clearReceiptTimer();
                options.onStatus("connected");
                options.onReady();
            });
            receiptTimer = setTimeout(() => {
                if (!valid()) return;
                options.onStatus("transportError");
                client.forceDisconnect();
            }, 8000);
            subscriptions.push(client.subscribe("/user/queue/messaging-errors", frame => {
                if (valid()) fail(errorCode(frame));
            }));
            subscriptions.push(client.subscribe(`/user/queue/conversations/${options.conversationId}/messages`, frame => {
                if (!valid()) return;
                try {
                    const message: MessageResponse = JSON.parse(frame.body);
                    if (message?.conversationId !== options.conversationId || typeof message.id !== "string"
                        || typeof message.senderUserId !== "string" || typeof message.content !== "string"
                        || typeof message.createdAt !== "string" || !Number.isFinite(Date.parse(message.createdAt))) return;
                    options.onMessage(message);
                } catch { /* Ignore malformed deliveries; authenticated REST history remains authoritative. */ }
            }, { receipt }));
        },
        onStompError: frame => {
            // Every STOMP ERROR is terminal; only private INVALID_MESSAGE errors are recoverable.
            const code = errorCode(frame);
            fail(code === "INVALID_MESSAGE" ? "INVALID_DESTINATION" : code);
        },
        onWebSocketError: () => { if (current()) options.onStatus("transportError"); },
        onWebSocketClose: () => {
            generation++;
            clearReceiptTimer();
            subscriptions.length = 0;
            if (current()) options.onStatus("disconnected");
        },
    });
    checkExpiry();
    if (current()) client.activate();
    return stop;
}
