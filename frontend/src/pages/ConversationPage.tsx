import { useLayoutEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { ArrowLeft, Calendar, Home, Loader2, MessageSquare, RefreshCw, Send } from "lucide-react";
import { useConversation } from "../hooks/useConversation";
import { decodeJwt } from "../utils/jwtUtils";
import { getLocaleCode } from "../utils/locale";
import { MESSAGE_LIMIT, normalizeMessage } from "../utils/conversationMessages";

const secondaryButton = "inline-flex items-center justify-center gap-2 px-4 py-2 text-sm font-bold text-brand-primary bg-brand-bg border border-brand-accent rounded-lg shadow-sm hover:bg-brand-accent/30 transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-primary disabled:opacity-50 disabled:cursor-not-allowed";

export default function ConversationPage() {
    const { conversationId = "" } = useParams();
    const token = localStorage.getItem("token");
    const userId = decodeJwt(token)?.userId;
    const [attempt, setAttempt] = useState(0);

    return <ConversationScreen key={`${conversationId}:${token}:${attempt}`} conversationId={conversationId}
        userId={userId} onRetry={() => setAttempt(value => value + 1)} />;
}

function ConversationScreen({ conversationId, userId, onRetry }: {
    conversationId: string;
    userId: string | undefined;
    onRetry: () => void;
}) {
    const { t, i18n } = useTranslation();
    const chat = useConversation(conversationId, userId);
    const [draft, setDraft] = useState("");
    const [validation, setValidation] = useState<"blank" | null>(null);
    const [announcement, setAnnouncement] = useState("");
    const historyRef = useRef<HTMLDivElement>(null);
    const composerRef = useRef<HTMLTextAreaElement>(null);
    const scrollPosition = useRef<{ height: number; top: number; bottom: boolean; prepend: boolean } | null>(null);
    const initializedScroll = useRef(false);
    const locale = getLocaleCode(i18n.language);
    const content = normalizeMessage(draft);
    const tooLong = content.length > MESSAGE_LIMIT;
    const busy = chat.operation !== null;

    useLayoutEffect(() => {
        const history = historyRef.current;
        if (!history) return;
        const previous = scrollPosition.current;
        if (!initializedScroll.current || previous?.bottom) {
            history.scrollTop = history.scrollHeight;
        } else if (previous) {
            history.scrollTop = previous.top + (previous.prepend ? history.scrollHeight - previous.height : 0);
        }
        initializedScroll.current = true;
        scrollPosition.current = null;
    }, [chat.messages]);

    async function loadHistory(action: "older" | "refresh") {
        const history = historyRef.current;
        if (history) scrollPosition.current = {
            height: history.scrollHeight,
            top: history.scrollTop,
            bottom: action === "refresh" && history.scrollHeight - history.scrollTop - history.clientHeight < 48,
            prepend: action === "older",
        };
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
        if (history) scrollPosition.current = { height: history.scrollHeight, top: history.scrollTop, bottom: true, prepend: false };
        if (await chat.send(content)) {
            setDraft("");
            setAnnouncement(t("chat.sent"));
            composerRef.current?.focus();
        }
    }

    if (chat.loading) return (
        <div className="p-8 flex items-center justify-center gap-2 text-brand-muted" role="status">
            <Loader2 size={18} className="animate-spin motion-reduce:animate-none" aria-hidden="true" />{t("chat.loading")}
        </div>
    );

    if (chat.fatalError || !chat.conversation) return (
        <div className="max-w-7xl mx-auto p-4 md:p-8 text-brand-main">
            <div className="bg-white border border-brand-accent rounded-xl shadow-sm p-5 md:p-6 space-y-4">
                <h1 className="text-3xl font-black tracking-tight">{t("chat.title")}</h1>
                <p role="alert" className="text-sm text-red-700">{t(`chat.errors.${chat.fatalError ?? "load"}`)}</p>
                {chat.fatalError === "unauthenticated" ? (
                    <Link to="/login" className={secondaryButton}>{t("chat.login")}</Link>
                ) : <button type="button" onClick={onRetry} className={secondaryButton}>{t("chat.retry")}</button>}
            </div>
        </div>
    );

    const conversation = chat.conversation;
    const otherLabel = userId === conversation.guestUserId ? t("chat.owner") : t("chat.guest");
    const validationMessage = tooLong ? t("chat.tooLong", { limit: MESSAGE_LIMIT }) : validation ? t("chat.blank") : null;

    return (
        <div className="max-w-7xl mx-auto p-4 md:p-8 text-brand-main space-y-6 min-w-0">
            <Link to={`/reservations/${conversation.reservationId}`} className={`${secondaryButton} w-fit`}>
                <ArrowLeft size={16} aria-hidden="true" />{t("chat.backToReservation")}
            </Link>
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-brand-accent pb-6">
                <div className="min-w-0 space-y-1.5">
                    <h1 className="text-3xl font-black tracking-tight">{t("chat.title")}</h1>
                    <p className="text-sm text-brand-muted">{t("chat.subtitle")}</p>
                </div>
                <button type="button" onClick={() => void loadHistory("refresh")} disabled={busy} className={`${secondaryButton} shrink-0`}>
                    <RefreshCw size={16} aria-hidden="true" className={chat.operation === "refresh" ? "animate-spin motion-reduce:animate-none" : ""} />
                    {t(chat.operation === "refresh" ? "chat.refreshing" : "chat.refresh")}
                </button>
            </div>

            <section aria-labelledby="chat-reservation" className="bg-white border border-brand-accent rounded-xl shadow-sm p-5 md:p-6 space-y-4">
                <h2 id="chat-reservation" className="flex items-center gap-2 text-lg font-bold tracking-tight">
                    <Home size={18} className="text-brand-primary" aria-hidden="true" />{t("reservationDetails.stayInfo")}
                </h2>
                <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                    <div className="min-w-0">
                        {chat.reservation && <p className="text-xl font-black break-words">{chat.reservation.unitName || t("reservationDetails.nameUnavailable")}</p>}
                        <p className="text-sm text-brand-muted break-words">{t("myReservations.reservationId")} <span title={conversation.reservationId} className="font-mono font-bold text-brand-main">#RES-{conversation.reservationId.slice(-8)}</span></p>
                    </div>
                    {chat.reservation ? (
                        <div className="flex flex-wrap gap-x-6 gap-y-3 text-sm">
                            <div><span className="flex items-center gap-1.5 text-brand-muted text-xs font-bold uppercase tracking-wider"><Calendar size={14} aria-hidden="true" />{t("manualReservation.checkIn")}</span><p className="mt-1 font-bold">{chat.reservation.startDate}</p></div>
                            <div><span className="flex items-center gap-1.5 text-brand-muted text-xs font-bold uppercase tracking-wider"><Calendar size={14} aria-hidden="true" />{t("manualReservation.checkOut")}</span><p className="mt-1 font-bold">{chat.reservation.endDate}</p></div>
                        </div>
                    ) : (
                        <div className="text-sm text-brand-muted space-y-2">
                            <p role="status">{t("chat.contextError")}</p>
                            <button type="button" className={secondaryButton} disabled={chat.contextLoading} onClick={() => void chat.retryContext()}>{t(chat.contextLoading ? "chat.loading" : "chat.retry")}</button>
                        </div>
                    )}
                </div>
            </section>

            <section aria-labelledby="chat-history" className="bg-white border border-brand-accent rounded-xl shadow-sm overflow-hidden min-w-0">
                <h2 id="chat-history" className="flex items-center gap-2 text-lg font-bold tracking-tight p-4 sm:px-6 border-b border-brand-accent">
                    <MessageSquare size={18} className="text-brand-primary" aria-hidden="true" />{t("chat.history")}
                </h2>
                {chat.operationError && <div className="p-4 border-b border-brand-accent bg-red-50 text-red-700 text-sm space-y-2" role="alert">
                    <p>{t(`chat.errors.${chat.operationError}`)}</p>
                    {(chat.operationError === "older" || chat.operationError === "refresh") && <button type="button" className={secondaryButton} disabled={busy}
                        onClick={() => void loadHistory(chat.operationError === "older" ? "older" : "refresh")}>{t("chat.retry")}</button>}
                </div>}
                <div ref={historyRef} role="region" aria-label={t("chat.history")} tabIndex={0}
                    className="h-[min(50dvh,32rem)] min-h-48 overflow-y-auto overscroll-contain [overflow-anchor:none] bg-brand-bg/50 p-4 sm:p-6 focus-visible:outline-2 focus-visible:outline-brand-primary focus-visible:-outline-offset-2"
                    onScroll={() => {
                        const history = historyRef.current;
                        if (history && scrollPosition.current) scrollPosition.current = {
                            ...scrollPosition.current,
                            height: history.scrollHeight, top: history.scrollTop,
                            bottom: chat.operation !== "older" && history.scrollHeight - history.scrollTop - history.clientHeight < 48,
                        };
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
                            {chat.messages.map(message => {
                                const own = message.senderUserId === userId;
                                return <li key={message.id} className={`flex ${own ? "justify-end" : "justify-start"}`}>
                                    <article className={`min-w-0 max-w-[90%] sm:max-w-[75%] rounded-xl border px-4 py-3 shadow-sm ${own ? "bg-brand-primary border-brand-primary text-white" : "bg-white border-brand-accent text-brand-main"}`}>
                                        <p className={`text-xs font-bold mb-1.5 ${own ? "text-white/80" : "text-brand-primary"}`}>{own ? t("chat.you") : otherLabel}</p>
                                        <p className="text-sm leading-relaxed whitespace-pre-wrap [overflow-wrap:anywhere]">{message.content}</p>
                                        <time dateTime={message.createdAt} className={`block text-xs mt-2 ${own ? "text-white/80" : "text-brand-muted"}`}>
                                            {new Date(message.createdAt).toLocaleString(locale, { dateStyle: "medium", timeStyle: "short" })}
                                        </time>
                                    </article>
                                </li>;
                            })}
                        </ol>
                    )}
                </div>
                <form className="p-4 sm:p-6 border-t border-brand-accent space-y-3" onSubmit={event => { event.preventDefault(); void submit(); }}>
                    <label htmlFor="chat-message" className="block text-sm font-bold">{t("chat.message")}</label>
                    <textarea id="chat-message" ref={composerRef} rows={3} value={draft} readOnly={chat.operation === "send"} aria-busy={chat.operation === "send"}
                        placeholder={t("chat.placeholder")} aria-invalid={!!validationMessage} aria-describedby={`chat-help chat-count${validationMessage ? " chat-validation" : ""}`}
                        className="block w-full min-w-0 resize-y max-h-64 bg-white border border-brand-accent rounded-lg p-3 text-base sm:text-sm focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary disabled:opacity-60"
                        onChange={event => { setDraft(event.target.value); setValidation(null); setAnnouncement(""); }}
                        onKeyDown={event => {
                            if (event.key === "Enter" && !event.shiftKey && !event.nativeEvent.isComposing && window.matchMedia("(hover: hover) and (pointer: fine)").matches) {
                                event.preventDefault();
                                void submit();
                            }
                        }} />
                    {validationMessage && <p id="chat-validation" role="alert" className="text-sm text-red-700">{validationMessage}</p>}
                    <div className="flex flex-wrap items-center justify-between gap-3">
                        <div className="text-xs text-brand-muted space-y-1">
                            <p id="chat-help">{t("chat.keyboardHint")}</p>
                            <p id="chat-count" className={tooLong ? "text-red-700 font-bold" : ""}>{t("chat.characterCount", { count: content.length, limit: MESSAGE_LIMIT })}</p>
                        </div>
                        <button type="submit" disabled={busy || tooLong} className="inline-flex items-center justify-center gap-2 px-5 py-2.5 bg-brand-primary text-white hover:bg-brand-primary-hover text-sm font-bold rounded-lg border border-brand-accent shadow-sm transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-primary disabled:opacity-50 disabled:cursor-not-allowed">
                            {chat.operation === "send" ? <Loader2 size={16} className="animate-spin motion-reduce:animate-none" aria-hidden="true" /> : <Send size={16} aria-hidden="true" />}
                            {t(chat.operation === "send" ? "chat.sending" : "chat.send")}
                        </button>
                    </div>
                    <p role="status" className="text-sm text-brand-primary min-h-5">{chat.operation === "refresh" ? t("chat.refreshing") : announcement}</p>
                </form>
            </section>
        </div>
    );
}
