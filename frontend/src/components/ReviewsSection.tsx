
import { useEffect, useId, useState } from "react";
import { Star } from "lucide-react";
import { useTranslation } from "react-i18next";
import { getReviewsForProperty, getReviewsForUnit } from "../api/reviewApi";
import type { ReviewPageDto } from "../types/review";
import { getLocaleCode } from "../utils/locale";

interface ReviewsSectionProps {
    kind: "property" | "unit";
    accommodationId: string;
}

export default function ReviewsSection(props: ReviewsSectionProps) {
    return (
        <ReviewsContent
            key={`${props.kind}:${props.accommodationId}`}
            {...props}
        />
    );
}

function ReviewsContent({ kind, accommodationId }: ReviewsSectionProps) {
    const { t, i18n } = useTranslation();
    const headingId = useId();

    const [page, setPage] = useState(0);
    const [attempt, setAttempt] = useState(0);
    const [result, setResult] = useState<ReviewPageDto | null>(null);
    const [loading, setLoading] = useState(true);
    const [failed, setFailed] = useState(false);

    const locale = getLocaleCode(i18n.language);

    useEffect(() => {
        let active = true;
        let correctingPage = false;

        const fetchReviews =
            kind === "property" ? getReviewsForProperty : getReviewsForUnit;

        fetchReviews(accommodationId, page, kind === "unit" ? 2 : 5)
            .then(data => {
                if (!active) return;

                const lastPage = Math.max(0, data.totalPages - 1);

                if (page > lastPage) {
                    correctingPage = true;
                    setPage(lastPage);
                    return;
                }

                setResult(data);
            })
            .catch(() => {
                if (active) setFailed(true);
            })
            .finally(() => {
                if (active && !correctingPage) {
                    setLoading(false);
                }
            });

        return () => {
            active = false;
        };
    }, [kind, accommodationId, page, attempt]);

    const changePage = (nextPage: number) => {
        setLoading(true);
        setFailed(false);
        setPage(nextPage);
    };

    const retry = () => {
        setLoading(true);
        setFailed(false);
        setAttempt(value => value + 1);
    };

    const buttonClasses =
        "rounded-lg border border-brand-accent px-4 py-2 text-sm font-semibold text-brand-primary hover:bg-brand-bg disabled:opacity-50 disabled:cursor-not-allowed focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-primary";

    return (
        <section
            aria-labelledby={headingId}
            className="min-w-0 rounded-xl border border-brand-accent bg-white p-4 sm:p-6"
        >
            <div className="flex flex-wrap items-center justify-between gap-3">
                <h2
                    id={headingId}
                    className="text-xl font-bold text-brand-main"
                >
                    {t(
                        kind === "property"
                            ? "reviews.propertyTitle"
                            : "reviews.unitTitle"
                    )}
                </h2>

                {result && !failed && (
                    <div className="flex flex-wrap items-center gap-2">
                        {result.averageRating != null ? (
                            <>
                                <Star
                                    size={22}
                                    className="shrink-0 fill-amber-400 text-amber-400"
                                    aria-hidden="true"
                                />

                                <span className="text-xl font-bold text-brand-main">
                                    {result.averageRating.toLocaleString(locale, {
                                        maximumFractionDigits: 2
                                    })}
                                </span>

                                <span className="text-sm text-brand-muted">
                                    / 5
                                </span>

                                <span
                                    className="text-sm text-brand-muted"
                                    aria-hidden="true"
                                >
                                    ·
                                </span>
                            </>
                        ) : (
                            <span className="text-sm text-brand-muted">
                                {t("reviews.noAverage")}
                            </span>
                        )}

                        <span className="text-sm text-brand-muted">
                            {t("reviews.count", {
                                count: result.reviewCount
                            })}
                        </span>
                    </div>
                )}
            </div>

            <div className="my-4 border-t border-brand-accent" />

            <div aria-busy={loading} className="space-y-4">
                {loading ? (
                    <p
                        role="status"
                        className="text-sm text-brand-muted"
                    >
                        {t("reviews.loading")}
                    </p>
                ) : failed ? (
                    <div role="alert" className="space-y-3">
                        <p className="text-sm text-red-700">
                            {t("reviews.error")}
                        </p>

                        <button
                            type="button"
                            className={buttonClasses}
                            onClick={retry}
                        >
                            {t("reviews.retry")}
                        </button>
                    </div>
                ) : result && (
                    <>
                        {result.reviewCount === 0 ? (
                            <p className="text-sm text-brand-muted">
                                {t(
                                    kind === "property"
                                        ? "reviews.emptyProperty"
                                        : "reviews.emptyUnit"
                                )}
                            </p>
                        ) : (
                            <>
                                <ul className="divide-y divide-brand-accent">
                                    {result.reviews.map(review => {
                                        const date = review.createdAt
                                            ? new Date(review.createdAt)
                                            : null;

                                        return (
                                            <li
                                                key={review.id}
                                                className="space-y-2 py-4 first:pt-0"
                                            >
                                                <div className="flex flex-wrap items-center justify-between gap-2">
                                                    <div
                                                        className="flex items-center gap-1"
                                                        role="img"
                                                        aria-label={t(
                                                            "reviews.rating",
                                                            {
                                                                rating: review.rating
                                                            }
                                                        )}
                                                    >
                                                        {[1, 2, 3, 4, 5].map(star => (
                                                            <Star
                                                                key={star}
                                                                size={18}
                                                                aria-hidden="true"
                                                                className={
                                                                    star <= review.rating
                                                                        ? "fill-amber-400 text-amber-400"
                                                                        : "text-gray-300"
                                                                }
                                                            />
                                                        ))}

                                                        <span
                                                            aria-hidden="true"
                                                            className="ml-2 text-sm font-semibold"
                                                        >
                                                            {review.rating}/5
                                                        </span>
                                                    </div>

                                                    {date &&
                                                        !Number.isNaN(
                                                            date.getTime()
                                                        ) && (
                                                            <time
                                                                dateTime={review.createdAt}
                                                                className="text-xs text-brand-muted"
                                                            >
                                                                {date.toLocaleDateString(
                                                                    locale
                                                                )}
                                                            </time>
                                                        )}
                                                </div>

                                                {review.comment?.trim() && (
                                                    <p className="whitespace-pre-wrap break-words text-sm leading-relaxed text-brand-main">
                                                        {review.comment}
                                                    </p>
                                                )}
                                            </li>
                                        );
                                    })}
                                </ul>

                                {result.reviews.length === 0 && (
                                    <p className="text-sm text-brand-muted">
                                        {t("reviews.emptyPage")}
                                    </p>
                                )}
                            </>
                        )}

                        {(result.totalPages > 1 || page > 0) && (
                            <nav
                                aria-label={t("reviews.pagination")}
                                className="flex flex-wrap items-center justify-between gap-3 border-t border-brand-accent pt-4"
                            >
                                <button
                                    type="button"
                                    className={buttonClasses}
                                    disabled={page === 0}
                                    onClick={() => changePage(page - 1)}
                                >
                                    {t("reviews.previous")}
                                </button>

                                <span
                                    className="text-sm text-brand-muted"
                                    role="status"
                                >
                                    {t("reviews.page", {
                                        page: page + 1,
                                        total: result.totalPages
                                    })}
                                </span>

                                <button
                                    type="button"
                                    className={buttonClasses}
                                    disabled={
                                        page + 1 >= result.totalPages
                                    }
                                    onClick={() => changePage(page + 1)}
                                >
                                    {t("reviews.next")}
                                </button>
                            </nav>
                        )}
                    </>
                )}
            </div>
        </section>
    );
}
