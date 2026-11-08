import { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { translateMessage } from "../api/conversationApi";

export default function MessageTranslation({ conversationId, messageId, content, language }: {
    conversationId: string; messageId: string; content: string; language: string;
}) {
    const { t } = useTranslation();
    const [translated, setTranslated] = useState<string | null>(null);
    const [showOriginal, setShowOriginal] = useState(true);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(false);
    const pending = useRef<AbortController | null>(null);
    useEffect(() => () => pending.current?.abort(), []);

    async function translate() {
        if (pending.current) return;
        if (translated !== null) { setShowOriginal(false); return; }
        const controller = new AbortController();
        pending.current = controller;
        setLoading(true);
        setError(false);
        try {
            const result = await translateMessage(conversationId, messageId, language, controller.signal);
            if (!result.translatedText?.trim() || result.targetLanguage.toLowerCase() !== language.toLowerCase()) throw new Error("Invalid translation");
            if (!controller.signal.aborted) {
                setTranslated(result.translatedText);
                setShowOriginal(false);
            }
        } catch {
            if (!controller.signal.aborted) setError(true);
        } finally {
            if (!controller.signal.aborted) { setLoading(false); pending.current = null; }
        }
    }

    return <>
        <p lang={showOriginal ? undefined : language} className="text-sm leading-relaxed whitespace-pre-wrap [overflow-wrap:anywhere]">{showOriginal ? content : translated}</p>
        <div className="mt-2 text-xs space-y-1">
            <button type="button" disabled={loading} className="underline font-bold rounded focus-visible:outline-2 focus-visible:outline-offset-2 disabled:opacity-60"
                onClick={() => showOriginal ? void translate() : setShowOriginal(true)}>
                {t(loading ? "chat.translation.loading" : showOriginal ? "chat.translation.translate" : "chat.translation.original")}
            </button>
            {error && <p role="alert">{t("chat.translation.error")}</p>}
        </div>
    </>;
}
