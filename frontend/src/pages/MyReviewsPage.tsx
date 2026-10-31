import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getMyReviews } from "../api/reviewApi";
import type { ReviewResponseDto } from "../types/review";
import { useTranslation } from "react-i18next";
import { Star, MessageSquare, Edit3, Calendar, Info } from "lucide-react";

export default function MyReviewsPage() {
    const { t } = useTranslation();
    const [reviews, setReviews] = useState<ReviewResponseDto[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const loadReviews = useCallback(async () => {
        setLoading(true);
        setError("");

        try {
            const data = await getMyReviews();
            setReviews(data);
        } catch (err: unknown) {
            setError(err instanceof Error ? err.message : t('myReviews.loadError'));
        } finally {
            setLoading(false);
        }
    }, [t]);

    useEffect(() => {
        const timeoutId = window.setTimeout(() => {
            void loadReviews();
        }, 0);

        return () => {
            window.clearTimeout(timeoutId);
        };
    }, [loadReviews]);

    return (
        <div className="p-4 sm:p-8 max-w-7xl mx-auto min-h-screen text-[#1A1A1A] space-y-6">
            <div>
                <h1 className="text-3xl font-bold text-[#1A1A1A] tracking-tight">{t('myReviews.title')}</h1>
                <p className="text-sm text-[#7A7A7A] mt-1">{t('myReviews.subtitle')}</p>
            </div>

            {error && (
                <div className="flex items-center gap-3 p-4 bg-red-50 border-l-4 border-red-500 rounded-r-xl text-red-700 animate-fade-in shadow-sm">
                    <svg className="w-5 h-5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                        <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd"/>
                    </svg>
                    <span className="text-sm font-semibold">{error}</span>
                </div>
            )}

            {loading ? (
                <div className="p-8 text-center text-gray-500 font-medium">{t('myReviews.loading')}</div>
            ) : reviews.length === 0 ? (
                <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-5 sm:p-8 text-center space-y-4">
                    <MessageSquare size={36} className="mx-auto text-[#7A7A7A]" />
                    <p className="text-[#7A7A7A] font-medium">
                        {t('myReviews.noReviews')}
                    </p>
                </div>
            ) : (
                <div className="grid gap-6">
                    {reviews.map((review: any) => (
                        <div
                            key={review.id}
                            className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-6 hover:shadow-md transition-all duration-300"
                        >
                            <div className="grid grid-cols-1 lg:grid-cols-[240px_1fr_auto] gap-6 items-center">
                                <div className="space-y-2.5">
                                    <div className="flex items-center gap-3">
                                        <div className="p-2.5 bg-[#F7F7F7] border border-[#DACDCA] rounded-xl text-[#42211D]">
                                            <Star size={20} />
                                        </div>
                                        <div>
                                            <h2 className="text-lg font-black text-[#1A1A1A] tracking-tight">
                                                {t('myReviews.reviewName')}
                                            </h2>
                                            <div className="text-sm text-gray-500 mt-1" title={review.id}>
                                                ID: <span className="text-gray-900 font-semibold font-mono">#{review.id.slice(-8)}</span>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 lg:px-4 items-center">
                                    {review.startDate && review.endDate && (
                                        <div className="space-y-1">
                                            <span className="block text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">{t('adminReservations.stayDates')}</span>
                                            <div className="flex items-center gap-2 text-sm text-[#1A1A1A]">
                                                <Calendar size={16} className="text-[#42211D] shrink-0" />
                                                <span className="font-semibold text-gray-800 whitespace-nowrap">{review.startDate} &rarr; {review.endDate}</span>
                                            </div>
                                        </div>
                                    )}

                                    <div className="space-y-1">
                                        <span className="block text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">
                                             {t('myReviews.ratingName')}
                                        </span>
                                        <div className="flex items-center gap-2 text-sm text-[#42211D]">
                                            <Star size={16} className="text-amber-500 fill-amber-500 shrink-0" />
                                            <span className="text-lg font-black tracking-tight whitespace-nowrap">
                                                {review.rating} / 5
                                            </span>
                                        </div>
                                    </div>
                                </div>

                                <div className="flex flex-col sm:flex-row lg:flex-col items-start sm:items-center lg:items-end justify-between lg:justify-center gap-4 w-full lg:w-auto border-t lg:border-t-0 pt-4 lg:pt-0 border-[#DACDCA]">
                                    <div className="flex flex-wrap items-center gap-2.5 w-full sm:w-auto justify-start sm:justify-end">
                                        <Link
                                            to={`/reservations/${review.reservationId}`}
                                            className="px-4 py-2 text-xs font-bold bg-white border border-gray-300 text-gray-700 hover:bg-gray-50 rounded-lg transition-all shadow-sm active:scale-95 inline-flex items-center justify-center gap-1.5 shrink-0"
                                        >
                                            <Info size={14} />
                                            {t('reservationDetails.showReservation')}
                                        </Link>

                                        <Link
                                            to={`/reviews/${review.id}`}
                                            className="px-4 py-2 text-xs font-bold text-white bg-[#42211D] hover:bg-[#321815] rounded-lg transition-all shadow-sm active:scale-95 inline-flex items-center justify-center gap-1.5 shrink-0"
                                        >
                                            <Edit3 size={14} />
                                            <span>{t('myReviews.editReview')}</span>
                                        </Link>
                                    </div>
                                </div>
                            </div>

                            <div className="mt-6 pt-4 border-t border-gray-100 space-y-1">
                                <span className="block text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">
                                    {t('myReviews.commentLabel')}
                                </span>
                                <p className="text-sm font-medium text-[#1A1A1A] leading-relaxed">
                                    {review.comment || t('myReviews.noComment')}
                                </p>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}