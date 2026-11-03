import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Star } from "lucide-react";

export interface ReviewFormData {
    rating: number;
    comment: string;
}

interface ReviewFormProps {
    initialValues?: ReviewFormData;
    onSubmit: (data: ReviewFormData) => Promise<void>;
    submitLabel: string;
}

export default function ReviewForm({
                                       initialValues,
                                       onSubmit,
                                       submitLabel,
                                   }: ReviewFormProps) {
    const [form, setForm] = useState<ReviewFormData>(() => {
        if (initialValues) {
            return {
                ...initialValues,
                rating: initialValues.rating ?? 5,
                comment: initialValues.comment ?? ""
            };
        }
        return {
            rating: 5,
            comment: ""
        };
    });

    const [hoverRating, setHoverRating] = useState<number>(0);

    const handleChange = (
        e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
    ) => {
        setForm(prev => ({
            ...prev,
            [e.target.name]: e.target.value,
        }));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        await onSubmit(form);
    };

    const inputClasses = "w-full border border-[#DACDCA] rounded-lg p-3 text-sm font-semibold text-[#1A1A1A] placeholder-[#7A7A7A]/70 focus:outline-none focus:border-[#42211D] focus:ring-1 focus:ring-[#42211D] transition-all bg-transparent resize-none";
    const { t } = useTranslation();

    return (
        <form onSubmit={handleSubmit} className="space-y-5">
            <div className="space-y-1">
                <label className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">
                    {t('reviewForm.ratingLabel')}
                </label>
                <div className="flex items-center gap-1.5 py-2">
                    {[1, 2, 3, 4, 5].map((star) => {
                        const isFilled = star <= (hoverRating || form.rating);
                        return (
                            <button
                                key={star}
                                type="button"
                                onClick={() => setForm(prev => ({ ...prev, rating: star }))}
                                onMouseEnter={() => setHoverRating(star)}
                                onMouseLeave={() => setHoverRating(0)}
                                className="p-1 focus:outline-none transition-transform active:scale-110"
                            >
                                <Star
                                    size={32}
                                    className={isFilled ? "text-amber-400 fill-amber-400" : "text-gray-300"}
                                />
                            </button>
                        );
                    })}
                    <span className="ml-3 text-sm font-bold text-[#1A1A1A]">
                        {hoverRating || form.rating} / 5
                    </span>
                </div>
            </div>

            <div className="space-y-1">
                <label className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">
                    {t('reviewForm.commentLabel')}
                </label>
                <textarea
                    name="comment"
                    rows={5}
                    placeholder={t('reviewForm.commentPlaceholder')}
                    value={form.comment}
                    onChange={handleChange}
                    className={inputClasses}
                />
            </div>

            <div className="pt-4">
                <button
                    type="submit"
                    className="w-full md:w-auto px-6 py-3 bg-[#42211D] text-white font-bold hover:bg-[#5C2E29] text-sm rounded-lg transition-colors border border-[#DACDCA] shadow-sm tracking-tight"
                >
                    {submitLabel}
                </button>
            </div>
        </form>
    );
}