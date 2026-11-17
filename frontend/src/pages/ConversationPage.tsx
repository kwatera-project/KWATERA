import { useLayoutEffect, useRef, useState } from "react";
import { Link, useLocation, useNavigate, useOutletContext, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { ArrowLeft, Loader2, MessageSquare, PanelLeftOpen, Send } from "lucide-react";
import { useConversation } from "../hooks/useConversation";
import { decodeJwt } from "../utils/jwtUtils";
import { getLocaleCode } from "../utils/locale";
import { MESSAGE_LIMIT, normalizeMessage } from "../utils/conversationMessages";
import { useAuthToken } from "../hooks/useAuthToken";
import type { ConversationsOutletContext } from "./ConversationsPage";
import ConversationThumbnail from "../components/ConversationThumbnail";
import MessageTranslation from "../components/MessageTranslation";

const secondaryButton = "chat-secondary";

function readScrollPosition(history: HTMLDivElement) {
    const first = history.querySelector<HTMLElement>("[data-message-id]");
    return {
        first, offset: first?.offsetTop ?? 0, top: history.scrollTop,
        bottom: history.scrollHeight - history.scrollTop - history.clientHeight < 48,
    };
}

export default function ConversationPage() {
    const { conversationId = "" } = useParams();
    const token = useAuthToken();
    const userId = decodeJwt(token)?.userId;
    const [attempt, setAttempt] = useState(0);

    return <ConversationScreen key={`${conversationId}:${token}:${attempt}`} conversationId={conversationId}
        userId={userId} token={token} onRetry={() => setAttempt(value => value + 1)} />;
}

function ConversationScreen({ conversationId, userId, token, onRetry }: {
    conversationId: string;
    userId: string | undefined;
    token: string | null;
    onRetry: () => void;
}) {
    const { t, i18n } = useTranslation();
    const { reportAccess, inboxCollapsed, toggleInbox } = useOutletContext<ConversationsOutletContext>();
    const chat = useConversation(conversationId, userId, token);
    const backRef = useRef<HTMLAnchorElement>(null);
    const headingRef = useRef<HTMLHeadingElement>(null);
    const navigate = useNavigate();
    const fromInbox = useLocation().state?.fromInbox === true;
    const [draft, setDraft] = useState("");
    const [validation, setValidation] = useState<"blank" | null>(null);
    const [announcement, setAnnouncement] = useState("");
    const historyRef = useRef<HTMLDivElement>(null);
    const composerRef = useRef<HTMLTextAreaElement>(null);
    const scrollPosition = useRef<ReturnType<typeof readScrollPosition> | null>(null);
    const locale = getLocaleCode(i18n.language);
    const content = normalizeMessage(draft);
    const tooLong = content.length > MESSAGE_LIMIT;
    const busy = chat.operation !== null;

    useLayoutEffect(() => {
        if (chat.fatalError && chat.fatalError !== "load") reportAccess(conversationId, chat.fatalError);
    }, [conversationId, chat.fatalError, reportAccess]);

    useLayoutEffect(() => {
        const target = window.matchMedia("(min-width: 768px)").matches ? headingRef.current : backRef.current;
        target?.focus({ preventScroll: true });
    }, []);

    useLayoutEffect(() => {
        const history = historyRef.current;
        if (!history) return;
        const previous = scrollPosition.current;
        if (!previous || previous.bottom) {
            history.scrollTop = history.scrollHeight;
        } else if (previous.first && history.contains(previous.first)) {
            history.scrollTop = previous.top + previous.first.offsetTop - previous.offset;
        }
        scrollPosition.current = readScrollPosition(history);
    }, [chat.messages, chat.nextCursor]);

    async function loadHistory(action: "older" | "refresh") {
        const history = historyRef.current;
        if (history && action === "older") scrollPosition.current = { ...readScrollPosition(history), bottom: false };
        await chat.loadHistory(action);
    }

    async function submit() {
        if (busy) return;
        setAnnouncement("");
        if (!content.length) {
            setValidation("blank");
            composerRef.current?.focus();
            return;
        }
        if (tooLong) return;
        setValidation(null);
        const history = historyRef.current;
        if (history) scrollPosition.current = { ...readScrollPosition(history), bottom: true };
        if (await chat.send(content)) {
            setDraft("");
            setAnnouncement(t("chat.sent"));
            composerRef.current?.focus();
        }
    }

    const conversation = chat.conversation;
    const title = chat.reservation?.unitName || t("chat.title");
    const back = (
        <Link ref={backRef} to="/conversations" replace className={`${secondaryButton} md:hidden`}
            aria-label={t("inbox.back")} title={t("inbox.back")}
            onClick={event => {
                if (fromInbox && event.button === 0 && !event.ctrlKey && !event.metaKey && !event.shiftKey && !event.altKey) {
                    event.preventDefault();
                    navigate(-1);
                }
            }}>
            <ArrowLeft size={18} aria-hidden="true" /><span aria-hidden="true" className="md:hidden">{t("inbox.title")}</span>
        </Link>
    );
    const otherLabel = userId === conversation?.guestUserId ? t("chat.owner") : t("chat.guest");
    const validationMessage = tooLong ? t("chat.tooLong", { limit: MESSAGE_LIMIT }) : validation ? t("chat.blank") : null;

    const composerError = chat.operationError === "send" || chat.operationError === "invalidMessage" ? chat.operationError : null;
    const historyError = chat.operationError === "older" || chat.operationError === "refresh" ? chat.operationError : null;
    const connectionWarning = chat.connectionStatus !== "connected" && chat.connectionStatus !== "connecting";
    return (
        <section aria-labelledby="chat-title" className="min-h-0 flex-1 flex flex-col overflow-y-auto overscroll-contain">
            <header className={`grid shrink-0 grid-cols-[minmax(0,1fr)_auto] items-center gap-2 border-b border-brand-accent px-3 py-2 sm:px-4 md:gap-3 ${inboxCollapsed ? "md:grid-cols-[auto_minmax(0,1fr)_auto]" : ""}`}>
                <div className={`col-start-1 row-start-1 ${inboxCollapsed ? "" : "md:hidden"}`}>
                    {back}
                    {inboxCollapsed && <button type="button" className={`${secondaryButton} hidden w-11 px-0 md:inline-flex`}
                        aria-label={t("inbox.expand")} title={t("inbox.expand")}
                        data-inbox-toggle aria-controls="conversation-inbox" aria-expanded={false} onClick={toggleInbox}>
                        <PanelLeftOpen size={18} aria-hidden="true" />
                    </button>}
                </div>
                <div className={`col-span-2 col-start-1 row-start-2 flex min-w-0 items-center gap-3 md:col-span-1 md:row-start-1 ${inboxCollapsed ? "md:col-start-2" : ""}`}>
                    {!chat.fatalError && conversation && chat.reservation?.unitId &&
                        <ConversationThumbnail key={chat.reservation.unitId} unitId={chat.reservation.unitId} />}
                    <div className="min-w-0 flex-1">
                        <h2 id="chat-title" ref={headingRef} tabIndex={-1} title={title} className="line-clamp-2 w-fit max-w-full rounded-sm text-xl font-extrabold tracking-tight [overflow-wrap:anywhere] focus-visible:outline-2 focus-visible:outline-brand-primary md:text-2xl">{title}</h2>
                        {conversation && !chat.fatalError && <p className="text-sm text-brand-muted">
                            <span title={conversation.reservationId} className="font-mono font-bold">#RES-{conversation.reservationId.slice(-8)}</span>
                            {chat.reservation?.startDate && chat.reservation.endDate && <>
                                <span className="block sm:inline">
                                    <span className="hidden sm:inline">{" · "}</span>
                                    <time className="whitespace-nowrap" dateTime={chat.reservation.startDate}>{chat.reservation.startDate}</time>
                                    {" – "}<time className="whitespace-nowrap" dateTime={chat.reservation.endDate}>{chat.reservation.endDate}</time>
                                </span>
                            </>}
                        </p>}
                    </div>
                </div>
                {conversation && !chat.fatalError && <Link to={`/reservations/${conversation.reservationId}`} className={`${secondaryButton} col-start-2 row-start-1 ${inboxCollapsed ? "md:col-start-3" : ""}`}>
                    {t("chat.reservationDetails")}
                </Link>}
            </header>
            {chat.contextError && !chat.fatalError && <div className="chat-feedback-warning flex shrink-0 flex-wrap items-center justify-between gap-x-3 border-b border-brand-accent px-3 py-1 sm:px-4">
                <p role="status" className="text-sm">{t("chat.contextError")}</p>
                <button type="button" className="chat-inline-action" aria-disabled={chat.contextLoading}
                    onClick={() => void chat.retryContext()}>
                    {t(chat.contextLoading ? "chat.loadingReservationDetails" : "chat.retryReservationDetails")}
                </button>
            </div>}
            {chat.loading ? <p className="p-4 flex items-center gap-2 text-brand-muted" role="status">
                <Loader2 size={18} className="animate-spin motion-reduce:animate-none" aria-hidden="true" />{t("chat.loading")}
            </p> : chat.fatalError || !conversation ? <div className="space-y-2 p-4">
                <p role="alert" className="chat-feedback-error">{t(`chat.errors.${chat.fatalError ?? "load"}`)}</p>
                {chat.fatalError === "unauthenticated" ? <Link to="/login" className={secondaryButton}>{t("chat.login")}</Link>
                    : chat.fatalError === "load" && <button type="button" onClick={onRetry} className={secondaryButton}>{t("chat.retry")}</button>}
            </div> : <>
                {connectionWarning && <div className="chat-feedback-warning shrink-0 flex flex-wrap items-center justify-between gap-x-2 border-b border-brand-accent px-3 sm:px-4">
                    <p role="status" data-testid="connection-status" className="flex items-center gap-2 py-1.5 text-sm">
                        <span aria-hidden="true" className="h-2 w-2 shrink-0 rounded-full bg-amber-700" />
                        {t(`chat.connection.${chat.connectionStatus}`)}
                    </p>
                    {chat.connectionStatus === "subscriptionError" && <button type="button" onClick={chat.reconnect} className="chat-inline-action">{t("chat.retry")}</button>}
                </div>}
                {historyError && <div className="shrink-0 flex flex-wrap items-center justify-between gap-x-2 border-b border-brand-accent px-3">
                    <p role="alert" className="chat-feedback-error">{t(`chat.errors.${historyError}`)}</p>
                    <button type="button" className="chat-inline-action" disabled={busy}
                        onClick={() => void loadHistory(historyError)}>{t("chat.retry")}</button>
                </div>}
                <div ref={historyRef} role="region" aria-label={t("chat.history")} tabIndex={0}
                    className="relative min-h-16 flex-1 overflow-y-auto overscroll-contain [overflow-anchor:none] bg-brand-bg/60 p-3 sm:p-4 focus-visible:outline-2 focus-visible:outline-brand-primary focus-visible:-outline-offset-2"
                    onScroll={() => {
                        const history = historyRef.current;
                        if (history) {
                            const position = readScrollPosition(history);
                            scrollPosition.current = { ...position, bottom: chat.operation !== "older" && position.bottom };
                        }
                    }}>
                    {chat.nextCursor && <div className="flex justify-center mb-5">
                        <button type="button" onClick={() => void loadHistory("older")} disabled={busy} className={secondaryButton}>
                            {chat.operation === "older" && <Loader2 size={16} className="animate-spin motion-reduce:animate-none" aria-hidden="true" />}
                            {t(chat.operation === "older" ? "chat.loadingOlder" : "chat.loadOlder")}
                        </button>
                    </div>}
                    {chat.messages.length === 0 ? (
                        <div className="h-full flex flex-col justify-center items-center text-center text-brand-muted gap-2">
                            <MessageSquare size={28} className="text-brand-primary" aria-hidden="true" />
                            <p className="font-bold text-brand-main">{t("chat.empty")}</p><p className="text-sm">{t("chat.emptyHint")}</p>
                        </div>
                    ) : (
                        <ol className="space-y-4" aria-label={t("chat.history")}>
                            {chat.messages.map((message, index) => {
                                const own = message.senderUserId === userId;
                                const date = new Date(message.createdAt);
                                const previous = chat.messages[index - 1];
                                const startsDay = !previous || date.toDateString() !== new Date(previous.createdAt).toDateString();
                                return <li key={message.id}>
                                    {startsDay && <div className="mb-3 flex items-center gap-3 text-sm font-bold text-brand-muted before:h-px before:flex-1 before:bg-brand-accent after:h-px after:flex-1 after:bg-brand-accent">
                                        {date.toLocaleDateString(locale, { dateStyle: "long" })}
                                    </div>}
                                    <div className={`flex ${own ? "justify-end" : "justify-start"}`}>
                                        <article data-message-id={message.id} className={`min-w-0 max-w-[90%] sm:max-w-[75%] rounded-xl border px-4 py-3 shadow-sm ${own ? "bg-brand-bg border-brand-primary/40 text-brand-main" : "bg-card border-brand-accent text-brand-main"}`}>
                                            <p className="mb-1 text-sm font-bold text-brand-primary">{own ? t("chat.you") : otherLabel}</p>
                                            {own ? (
                                                <p className="text-base leading-relaxed whitespace-pre-wrap [overflow-wrap:anywhere]">{message.content}</p>
                                            ) : (
                                                <MessageTranslation key={i18n.resolvedLanguage ?? "en"} conversationId={conversationId} messageId={message.id} content={message.content} language={i18n.resolvedLanguage ?? "en"} />
                                            )}
                                            <time dateTime={message.createdAt} className="mt-1 block text-sm text-brand-muted">
                                                {date.toLocaleTimeString(locale, { timeStyle: "short" })}
                                            </time>
                                        </article>
                                    </div>
                                </li>;
                            })}
                        </ol>
                    )}
                </div>
                <form className="chat-composer shrink-0 bg-card p-3 sm:px-4 border-t border-brand-accent space-y-1.5" onSubmit={event => { event.preventDefault(); void submit(); }}>
                    <label htmlFor="chat-message" className="sr-only">{t("chat.message")}</label>
                    <textarea id="chat-message" ref={composerRef} rows={2} value={draft} readOnly={chat.operation === "send"} aria-busy={chat.operation === "send"}
                        placeholder={t("chat.placeholder")} aria-invalid={!!validationMessage || composerError === "invalidMessage"} aria-describedby={`chat-help chat-count${validationMessage ? " chat-validation" : ""}${composerError ? " chat-send-error" : ""}`}
                        className="block w-full min-w-0 resize-none bg-card border border-brand-accent rounded-lg p-3 text-base focus:outline-none focus:ring-2 focus:ring-brand-primary focus:border-brand-primary aria-invalid:border-red-800 disabled:opacity-60"
                        onChange={event => { setDraft(event.target.value); setValidation(null); setAnnouncement(""); }}
                        onKeyDown={event => {
                            if (event.key === "Enter" && !event.shiftKey && !event.nativeEvent.isComposing && window.matchMedia("(hover: hover) and (pointer: fine)").matches) {
                                event.preventDefault();
                                void submit();
                            }
                        }} />
                    {validationMessage && <p id="chat-validation" role="alert" className="chat-feedback-error">{validationMessage}</p>}
                    {composerError && <p id="chat-send-error" role="alert" className="chat-feedback-error">{t(`chat.errors.${composerError}`)}</p>}
                    <div className="flex items-center justify-between gap-2">
                        <div className="text-xs text-brand-muted space-y-1">
                            <p id="chat-help" className="sr-only md:not-sr-only">{t("chat.keyboardHint")}</p>
                            <p id="chat-count" className={tooLong ? "text-red-700 font-bold" : ""}>{t("chat.characterCount", { count: content.length, limit: MESSAGE_LIMIT })}</p>
                        </div>
                        <button type="submit" disabled={busy || tooLong} className="chat-secondary shrink-0 bg-brand-primary px-4 text-white hover:bg-brand-primary-hover">
                            {chat.operation === "send" ? <Loader2 size={16} className="animate-spin motion-reduce:animate-none" aria-hidden="true" /> : <Send size={16} aria-hidden="true" />}
                            {t(chat.operation === "send" ? "chat.sending" : composerError === "send" ? "chat.retrySend" : "chat.send")}
                        </button>
                    </div>
                    <p role="status" className="sr-only">{chat.operation === "refresh" ? t("chat.synchronizing") : announcement}</p>
                </form>
            </>}
        </section>
    );
}
