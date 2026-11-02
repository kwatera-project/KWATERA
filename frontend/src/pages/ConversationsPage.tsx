import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { ArrowRight, Loader2, MessageSquare } from "lucide-react";
import { ConversationApiError, getConversations } from "../api/conversationApi";
import { getReservationDetails } from "../api/reservationApi";
import type { ConversationResponse } from "../types/conversation";
import { decodeJwt } from "../utils/jwtUtils";
import { conversationAccessError, conversationActivity, INBOX_PAGE_SIZE, sortConversations } from "../utils/conversationInbox";
import { getLocaleCode } from "../utils/locale";
import { useAuthToken } from "../hooks/useAuthToken";

const button = "inline-flex items-center justify-center gap-2 px-4 py-2 text-sm font-bold text-brand-primary bg-brand-bg border border-brand-accent rounded-lg shadow-sm hover:bg-brand-accent/30 transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-primary disabled:opacity-50 disabled:cursor-not-allowed";

export default function ConversationsPage() {
    const token = useAuthToken();
    const [attempt, setAttempt] = useState(0);
    return <Inbox key={`${token}:${attempt}`} token={token} userId={decodeJwt(token)?.userId} onRetry={() => setAttempt(value => value + 1)} />;
}

function Inbox({ token, userId, onRetry }: { token: string | null; userId: string | undefined; onRetry: () => void }) {
    const { t } = useTranslation();
    const [conversations, setConversations] = useState<ConversationResponse[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [page, setPage] = useState(0);

    useEffect(() => {
        let active = true;
        const isCurrentSession = () => active && localStorage.getItem("token") === token;
        async function load() {
            try {
                if (!userId) throw new ConversationApiError("Unauthorized", 401);
                const items = await getConversations();
                if (isCurrentSession()) setConversations(sortConversations(items.filter(item => item.guestUserId === userId || item.ownerUserId === userId)));
            } catch (cause) {
                if (isCurrentSession()) {
                    setConversations([]);
                    setError(cause instanceof ConversationApiError ? conversationAccessError(cause.status) ?? "load" : "load");
                }
            } finally {
                if (isCurrentSession()) setLoading(false);
            }
        }
        void load();
        return () => { active = false; };
    }, [token, userId]);

    return (
        <div className="max-w-7xl mx-auto p-4 sm:p-8 min-h-screen text-brand-main space-y-6">
            <header className="border-b border-brand-accent pb-6 space-y-2">
                <h1 className="text-3xl font-black tracking-tight">{t("inbox.title")}</h1>
                <p className="text-sm text-brand-muted">{t("inbox.subtitle")}</p>
            </header>
            {loading ? <p role="status" className="flex items-center gap-2 text-brand-muted"><Loader2 size={18} aria-hidden="true" className="animate-spin motion-reduce:animate-none" />{t("inbox.loading")}</p>
                : error ? <div className="bg-white border border-brand-accent rounded-xl p-6 space-y-4">
                    <p role="alert" className="text-sm text-red-700">{t(`inbox.errors.${error}`)}</p>
                    {error === "unauthenticated" ? <Link to="/login" className={button}>{t("chat.login")}</Link>
                        : <button type="button" className={button} onClick={onRetry}>{t("chat.retry")}</button>}
                </div>
                : conversations.length === 0 ? <div className="bg-white border border-brand-accent rounded-xl p-6 sm:p-10 text-center space-y-3">
                    <MessageSquare size={28} className="mx-auto text-brand-primary" aria-hidden="true" />
                    <h2 className="text-lg font-bold">{t("inbox.empty")}</h2>
                    <p className="text-sm text-brand-muted">{t("inbox.emptyHint")}</p>
                </div> : <>
                    <InboxItems key={page} token={token} conversations={conversations.slice(page * INBOX_PAGE_SIZE, (page + 1) * INBOX_PAGE_SIZE)} />
                    {conversations.length > INBOX_PAGE_SIZE && <nav aria-label={t("inbox.pagination")} className="flex flex-wrap items-center justify-between gap-3">
                        <button type="button" className={button} disabled={page === 0} onClick={() => setPage(value => value - 1)}>{t("inbox.previous")}</button>
                        <p role="status" className="text-sm text-brand-muted">{t("inbox.page", { page: page + 1, total: Math.ceil(conversations.length / INBOX_PAGE_SIZE) })}</p>
                        <button type="button" className={button} disabled={(page + 1) * INBOX_PAGE_SIZE >= conversations.length} onClick={() => setPage(value => value + 1)}>{t("inbox.next")}</button>
                    </nav>}
                </>}
        </div>
    );
}

function InboxItems({ token, conversations }: { token: string | null; conversations: ConversationResponse[] }) {
    const { t, i18n } = useTranslation();
    const [unitNames, setUnitNames] = useState<Record<string, string>>({});

    useEffect(() => {
        let active = true;
        const isCurrentSession = () => active && localStorage.getItem("token") === token;
        async function enrich() {
            // Only the visible page, at most three requests in flight. A failed
            // optional context lookup must never hide an available conversation.
            const ids = [...new Set(conversations.map(item => item.reservationId))];
            for (let offset = 0; offset < ids.length && isCurrentSession(); offset += 3) {
                const results = await Promise.all(ids.slice(offset, offset + 3).map(async id => {
                    try {
                        const details = await getReservationDetails(id);
                        return [id, typeof details.unitName === "string" ? details.unitName : ""] as const;
                    } catch {
                        return [id, ""] as const;
                    }
                }));
                if (isCurrentSession()) setUnitNames(previous => ({ ...previous, ...Object.fromEntries(results) }));
            }
        }
        void enrich();
        return () => { active = false; };
    }, [token, conversations]);

    return <ul aria-label={t("inbox.title")} className="space-y-4">
        {conversations.map(conversation => {
            const activity = conversationActivity(conversation);
            return <li key={conversation.id} className="bg-white border border-brand-accent rounded-xl shadow-sm p-5 sm:p-6 flex flex-col sm:flex-row sm:items-center justify-between gap-5">
                <div className="min-w-0 space-y-2">
                    <h2 className="text-lg font-bold break-words">{unitNames[conversation.reservationId] || t("reservationDetails.nameUnavailable")}</h2>
                    <p className="text-sm text-brand-muted break-all">{t("myReservations.reservationId")} <span title={conversation.reservationId} className="font-mono font-bold text-brand-main">#RES-{conversation.reservationId.slice(-8)}</span></p>
                    <p className="text-xs font-bold text-brand-primary">{t("common.status")}: {t(`statuses.${conversation.reservationStatus}`)}</p>
                    <p className="text-sm text-brand-muted">{t("inbox.lastActivity")}: {activity ? <time dateTime={activity}>{new Date(activity).toLocaleString(getLocaleCode(i18n.language), { dateStyle: "medium", timeStyle: "short" })}</time> : t("inbox.dateUnavailable")}</p>
                </div>
                <Link to={`/conversations/${conversation.id}`} className={`${button} shrink-0`}>
                    {t("inbox.open")}<ArrowRight size={16} aria-hidden="true" />
                </Link>
            </li>;
        })}
    </ul>;
}
