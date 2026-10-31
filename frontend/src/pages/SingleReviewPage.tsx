import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { getReviewDetails, updateReview } from "../api/reviewApi";
import ReviewForm, { type ReviewFormData } from "../contexts/ReviewForm";
import type { ReviewResponseDto } from "../types/review";
import { useTranslation } from "react-i18next";

export default function SingleReviewPage() {
    const { reviewId } = useParams<{ reviewId: string }>();
    const navigate = useNavigate();
    const { t } = useTranslation();

    const [review, setReview] = useState<ReviewResponseDto | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        if (!reviewId) return;

        getReviewDetails(reviewId)
            .then(setReview)
            .catch((err) => {
                console.error(err);
                setError(t('singleReview.loadError'));
            })
            .finally(() => setLoading(false));
    }, [reviewId, t]);

    const handleUpdate = async (data: ReviewFormData) => {
        if (!reviewId) return;

        try {
            await updateReview(reviewId, data.rating, data.comment);
            alert(t('singleReview.updateSuccess'));
            navigate(-1);
        } catch (err) {
            console.error(err);
            alert(t('singleReview.updateFailed'));
        }
    };

    if (loading) {
        return <div className="p-8 text-center text-[#7A7A7A] font-medium">{t('singleReview.loading')}</div>;
    }

    if (error || !review) {
        return <div className="p-8 text-center text-red-600 font-semibold">{error || t('singleReview.notFound')}</div>;
    }

    return (
        <div className="p-4 sm:p-8 max-w-7xl mx-auto min-h-screen text-[#1A1A1A] space-y-6">
            <div>
                <button
                    onClick={() => navigate(-1)}
                    className="inline-flex items-center text-sm font-semibold text-[#7A7A7A] hover:text-[#1A1A1A] transition-colors mb-2"
                >
                    &larr; {t('singleReview.back')}
                </button>
            </div>

            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-[#DACDCA] pb-4 mb-6">
                <div>
                    <h1 className="text-3xl font-black text-[#1A1A1A] tracking-tight">
                        {t('singleReview.title')}
                    </h1>
                    <p className="text-sm font-semibold text-[#7A7A7A] mt-1">
                        {t('singleReview.subtitle')}
                    </p>
                </div>
            </div>

            <div className="max-w-4xl mx-auto bg-white border border-[#DACDCA] rounded-xl shadow-sm p-5 sm:p-8 mt-6">
                <ReviewForm
                    initialValues={{
                        rating: review.rating,
                        comment: review.comment || "",
                    }}
                    submitLabel={t('singleReview.submit')}
                    onSubmit={handleUpdate}
                />
            </div>
        </div>
    );
}