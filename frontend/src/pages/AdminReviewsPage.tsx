import { useEffect, useState, useCallback } from 'react';
import { Link } from "react-router-dom";
import { deleteReview } from "../api/reviewApi";
import {getAdminReviews} from "../api/adminApi";
import { Star, Info, ChevronDown, Trash2 } from 'lucide-react';
import { useTranslation } from "react-i18next";
import { format } from "date-fns";
import SharedDatePicker from "../components/SharedDatePicker.tsx";
import type {ReviewResponseDto} from "../types/review.ts";

const parseDateString = (str: string): Date | null => {
    if (!str) return null;
    const parts = str.split("-");
    if (parts.length !== 3) return null;
    const year = parseInt(parts[0], 10);
    const month = parseInt(parts[1], 10) - 1;
    const day = parseInt(parts[2], 10);
    return new Date(year, month, day);
};

export default function AdminReviewsPage() {
    const { t } = useTranslation();
    const [reviews, setReviews] = useState<ReviewResponseDto[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [dateType, setDateType] = useState<string>("createdAt");
    const [startDate, setStartDate] = useState<string>("");
    const [endDate, setEndDate] = useState<string>("");
    const [message, setMessage] = useState<{ text: string, type: 'success' | 'error' } | null>(null);

    const fetchReviews = useCallback(() => {
        setLoading(true);
        getAdminReviews(startDate || undefined, endDate || undefined, dateType || undefined)
            .then((data) => {
                setReviews(data);
                setLoading(false);
            })
            .catch((err) => {
                console.error(err);
                setMessage({
                    text: err instanceof Error ? err.message : t('adminReviews.loadError'),
                    type: 'error'
                });
                setLoading(false);
            });
    }, [startDate, endDate, dateType, t]);

    useEffect(() => {
        fetchReviews();
    }, [fetchReviews]);

    const handleDeleteReview = async (id: string) => {
        if (!window.confirm(t('adminReviews.confirmDelete'))) {
            return;
        }

        try {
            await deleteReview(id);
            setMessage({ text: t('adminReviews.deletedSuccess'), type: 'success' });
            setReviews(prev => prev.filter(r => r.id !== id));
        } catch (err: unknown) {
            console.error(err);
            setMessage({
                text: err instanceof Error ? err.message : t('common.networkError'),
                type: 'error'
            });
        }
    };

    return (
        <div className="p-4 sm:p-8 max-w-7xl mx-auto min-h-screen text-[#1A1A1A] space-y-6">
            <div className="border-b border-[#DACDCA] pb-6 mb-6 space-y-2">
                <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-4">
                    <h1 className="text-3xl font-bold text-[#1A1A1A] tracking-tight">
                        {t('adminReviews.title')}
                    </h1>
                </div>
                <p className="text-sm text-[#7A7A7A]">
                    {t('adminReviews.subtitle')}
                </p>
            </div>

            {message && (
                <div className={`flex items-center justify-between gap-3 p-4 border-l-4 rounded-r-xl animate-fade-in shadow-sm mb-6 ${
                    message.type === 'success' ? 'bg-emerald-50 border-emerald-500 text-emerald-700' : 'bg-red-50 border-red-500 text-red-700'
                }`}>
                    <div className="flex items-center gap-3">
                        <svg className="w-5 h-5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                            <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                        </svg>
                        <span className="text-sm font-semibold">{message.text}</span>
                    </div>
                    <button onClick={() => setMessage(null)} className="font-bold text-lg hover:opacity-75 transition-opacity px-2">&times;</button>
                </div>
            )}

            <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 bg-[#F7F7F7] border border-[#DACDCA] rounded-xl p-4 shadow-sm">
                <div className="text-sm font-medium text-[#7A7A7A]">
                    {t('adminReviews.showing')} <span className="font-bold text-[#1A1A1A]">{reviews.length}</span> {t('adminReviews.reviewCount', { count: reviews.length})}
                </div>

                <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-4 w-full md:w-auto">
                    <div className="flex items-center justify-between sm:justify-start gap-2 w-full sm:w-auto">
                        <span className="text-sm font-medium text-gray-500 shrink-0">
                            {t('adminReviews.dateType')}:
                        </span>
                        <div className="relative flex-1 sm:flex-initial sm:w-40">
                            <select
                                className="appearance-none block w-full bg-solid-white border border-[#DACDCA] rounded-lg py-2 pl-3 pr-10 text-sm text-[#1A1A1A] font-semibold focus:outline-none focus:ring-2 focus:ring-[#42211D]/20 focus:border-[#42211D] transition-all shadow-sm cursor-pointer"
                                value={dateType}
                                onChange={(e) => setDateType(e.target.value)}
                            >
                                <option value="createdAt">{t('adminReviews.createdAt')}</option>
                                <option value="updatedAt">{t('adminReviews.updatedAt')}</option>
                            </select>
                            <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-3">
                                <ChevronDown className="h-4 w-4 text-[#7A7A7A]" />
                            </div>
                        </div>
                    </div>

                    <div className="flex flex-col sm:flex-row sm:items-center gap-2 w-full sm:w-auto">
                        <div className="flex items-center justify-between w-full sm:w-auto">
                            <span className="text-sm font-medium text-gray-500">
                                {t('adminReviews.dateRange')}:
                            </span>
                            {(startDate || endDate) && (
                                <button
                                    onClick={() => {
                                        setStartDate("");
                                        setEndDate("");
                                    }}
                                    className="text-sm font-medium text-gray-500 hover:text-[#42211D] transition-colors shrink-0 cursor-pointer sm:hidden"
                                >
                                    {t('common.clear')}
                                </button>
                            )}
                        </div>

                        <div className="flex items-center justify-center gap-2 bg-solid-white border border-[#DACDCA] rounded-lg p-1.5 shadow-sm w-full sm:w-auto">
                            <SharedDatePicker
                                selected={parseDateString(startDate)}
                                onChange={(date) => setStartDate(date ? format(date, "yyyy-MM-dd") : "")}
                                selectsStart
                                startDate={parseDateString(startDate)}
                                endDate={parseDateString(endDate)}
                                allowPastDates={true}
                                placeholderText={t('search.startDate')}
                                className="bg-transparent text-sm font-semibold text-[#1A1A1A] outline-none cursor-pointer w-24 text-center"
                                wrapperClassName="w-auto"
                            />
                            <span className="text-gray-400 font-medium text-xs">→</span>
                            <SharedDatePicker
                                selected={parseDateString(endDate)}
                                onChange={(date) => setEndDate(date ? format(date, "yyyy-MM-dd") : "")}
                                selectsEnd
                                startDate={parseDateString(startDate)}
                                endDate={parseDateString(endDate)}
                                minDate={parseDateString(startDate)}
                                allowPastDates={true}
                                placeholderText={t('search.endDate')}
                                className="bg-transparent text-sm font-semibold text-[#1A1A1A] outline-none cursor-pointer w-24 text-center"
                                wrapperClassName="w-auto"
                            />
                        </div>

                        {(startDate || endDate) && (
                            <button
                                onClick={() => {
                                    setStartDate("");
                                    setEndDate("");
                                }}
                                className="text-sm font-medium text-gray-500 hover:text-[#42211D] transition-colors shrink-0 cursor-pointer hidden sm:block"
                            >
                                {t('common.clear')}
                            </button>
                        )}
                    </div>
                </div>
            </div>

            <div className="bg-transparent md:bg-white md:border md:border-[#DACDCA] md:rounded-xl md:shadow-sm overflow-hidden">
                {loading ? (
                    <div className="p-8 text-center text-gray-500 font-medium">
                        {t('reviews.loading')}
                    </div>
                ) : (
                    <table className="min-w-full block md:table">
                        <thead className="hidden md:table-header-group bg-[#F7F7F7] border-b border-[#DACDCA]">
                        <tr>
                            <th className="px-6 py-4 text-sm font-semibold text-[#7A7A7A] text-left">{t('adminReviews.reviewId')}</th>
                            <th className="px-6 py-4 text-sm font-semibold text-[#7A7A7A] text-left">{t('adminReviews.rating')}</th>
                            <th className="px-6 py-4 text-sm font-semibold text-[#7A7A7A] text-left">{t('adminReviews.datesFull')}</th>
                            <th className="px-6 py-4 text-sm font-semibold text-[#7A7A7A] text-left">{t('adminReviews.comment')}</th>
                            <th className="px-6 py-4 text-sm font-semibold text-[#7A7A7A] text-center">{t('common.actions')}</th>
                        </tr>
                        </thead>
                        <tbody className="block md:table-row-group divide-y-0 md:divide-y divide-[#DACDCA]/60 bg-transparent md:bg-white">
                        {reviews.map((review) => (
                            <tr key={review.id} className="block md:table-row bg-white md:bg-transparent rounded-xl border border-[#DACDCA] md:border-0 shadow-sm md:shadow-none mb-4 md:mb-0 p-5 md:p-0 space-y-3 md:space-y-0 hover:bg-transparent md:hover:bg-[#F7F7F7] transition-colors">
                                <td className="block md:table-cell px-0 md:px-6 py-2 md:py-4 text-sm font-semibold text-[#1A1A1A] border-b border-gray-100 md:border-0">
                                    <div className="flex justify-between items-center md:block">
                                        <span className="font-medium text-[#7A7A7A] md:hidden">ID:</span>
                                        <span className="font-mono text-xs">#{review.id.slice(-8)}</span>
                                    </div>
                                </td>
                                <td className="px-0 md:px-6 py-2 md:py-4 text-sm text-[#1A1A1A] border-b border-gray-100 md:border-0">
                                    <div className="flex justify-between items-center gap-2">
                                        <span className="font-medium text-[#7A7A7A] md:hidden">
                                            {t('adminReviews.rating')}:
                                        </span>

                                        <div className="flex items-center gap-1.5 text-amber-500 font-bold whitespace-nowrap">
                                            <Star size={14} className="fill-amber-500 shrink-0" />
                                            <span>{review.rating} / 5</span>
                                        </div>
                                    </div>
                                </td>
                                <td className="block md:table-cell px-0 md:px-6 py-2 md:py-4 text-sm text-[#1A1A1A] border-b border-gray-100 md:border-0">
                                    <div className="flex justify-between items-center md:block">
                                        <span className="font-medium text-[#7A7A7A] md:hidden">{t('adminReviews.dates')}:</span>
                                        <div className="text-xs space-y-0.5">
                                            <div><span className="text-gray-400">Added:</span> {review.createdAt ? review.createdAt.substring(0, 10) : '-'}</div>
                                            {review.updatedAt && (
                                                <div><span className="text-gray-400">Updated:</span> {review.updatedAt.substring(0, 10)}</div>
                                            )}
                                        </div>
                                    </div>
                                </td>
                                <td className="block md:table-cell px-0 md:px-6 py-2 md:py-4 text-sm text-[#1A1A1A] border-b border-gray-100 md:border-0 max-w-xs truncate">
                                    <div className="flex justify-between items-center md:block">
                                        <span className="font-medium text-[#7A7A7A] md:hidden">{t('adminReviews.comment')}:</span>
                                        <span className="truncate block max-w-xs text-gray-600">{review.comment || '-'}</span>
                                    </div>
                                </td>
                                <td className="block md:table-cell px-0 md:px-6 py-3 md:py-4 text-sm text-center">
                                    <div className="flex flex-row flex-nowrap justify-center items-center gap-1.5">
                                        <Link
                                            to={`/reviews/${review.id}`}
                                            className="px-2.5 py-1 text-xs font-semibold text-white bg-[#42211D] hover:bg-[#321815] rounded-md transition-all inline-flex items-center justify-center gap-1 shadow-sm whitespace-nowrap"
                                        >
                                            <Info size={13} />
                                            {t('adminReviews.viewReview')}
                                        </Link>

                                        <Link
                                            to={`/reservations/${review.reservationId}`}
                                            className="px-2.5 py-1 text-xs font-semibold text-white bg-[#42211D] hover:bg-[#321815] rounded-md transition-all inline-flex items-center justify-center gap-1 shadow-sm whitespace-nowrap"
                                        >
                                            <Info size={13} />
                                            {t('adminReviews.viewReservation')}
                                        </Link>

                                        <button
                                            onClick={() => handleDeleteReview(review.id)}
                                            className="px-2.5 py-1 text-xs font-semibold text-red-600 bg-red-50 hover:bg-red-100 rounded-md transition-all inline-flex items-center justify-center gap-1 cursor-pointer whitespace-nowrap"
                                        >
                                            <Trash2 size={13} />
                                            {t('common.delete')}
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ))}
                        {reviews.length === 0 && (
                            <tr className="block md:table-row bg-white rounded-xl border border-[#DACDCA] md:border-0 shadow-sm md:shadow-none p-6 md:p-0">
                                <td colSpan={5} className="block md:table-cell px-6 py-8 text-center text-[#7A7A7A] font-medium bg-white">
                                    {t('adminReviews.noReviews')}
                                </td>
                            </tr>
                        )}
                        </tbody>
                    </table>
                )}
            </div>
        </div>
    );
}