import { Link, useNavigate, useParams, useSearchParams } from "react-router-dom";
import { createReview } from "../api/reviewApi";
import ReviewForm, { type ReviewFormData } from "../contexts/ReviewForm";
import { useTranslation } from "react-i18next";

export default function CreateReviewPage() {
    const navigate = useNavigate();
    const { t } = useTranslation();
    const { reservationId: paramReservationId } = useParams<{ reservationId?: string }>();
    const [searchParams] = useSearchParams();

    const reservationId = paramReservationId || searchParams.get("reservationId");

    const handleCreate = async (data: ReviewFormData) => {
        if (!reservationId) {
            alert(t('createReview.missingReservationId'));
            return;
        }

        try {
            await createReview(reservationId, data.rating, data.comment);
            navigate(`/reservations/${reservationId}`);
        } catch (error) {
            console.error(error);
            alert(t('createReview.failed'));
        }
    };

    return (
        <div className="p-4 sm:p-8 max-w-7xl mx-auto min-h-screen text-[#1A1A1A] space-y-6">
            <div>
                <Link
                    to={reservationId ? `/reservations/${reservationId}` : "/my-reservations"}
                    className="inline-flex items-center text-sm font-semibold text-[#7A7A7A] hover:text-[#1A1A1A] transition-colors mb-2"
                >
                    &larr; {t('createReview.back')}
                </Link>
            </div>

            <div className="border-b border-[#DACDCA] pb-4 mb-6">
                <h1 className="text-3xl font-black text-[#1A1A1A] tracking-tight">
                    {t('createReview.title')}
                </h1>
                <p className="text-sm font-semibold text-[#7A7A7A] mt-1">
                    {t('createReview.subtitle')}
                </p>
            </div>

            <div className="max-w-4xl mx-auto bg-white border border-[#DACDCA] rounded-xl shadow-sm p-5 sm:p-8 mt-6">
                <ReviewForm
                    submitLabel={t('createReview.submit')}
                    onSubmit={handleCreate}
                />
            </div>
        </div>
    );
}