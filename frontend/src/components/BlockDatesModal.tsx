import React, { useState } from "react";
import { createBlock } from "../api/availabilityApi";
import { format } from "date-fns";
import SharedDatePicker from "./SharedDatePicker";
import {useTranslation} from "react-i18next"

const parseDateString = (str: string): Date | null => {
    if (!str) return null;
    const parts = str.split("-");
    if (parts.length !== 3) return null;
    const year = parseInt(parts[0], 10);
    const month = parseInt(parts[1], 10) - 1;
    const day = parseInt(parts[2], 10);
    return new Date(year, month, day);
};

interface Unit {
    id: string;
    name: string;
}

interface Occupancy {
    reservationId: string;
    unitId: string;
    unitName?: string;
    startDate: string;
    endDate: string;
    status: string;
    guestName?: string;
    totalPrice?: number;
}

const getOccupiedRange = (startDateStr: string, endDateStr: string) => {
    if (startDateStr < endDateStr) {
        const parts = endDateStr.split('-');
        const y = parseInt(parts[0], 10);
        const m = parseInt(parts[1], 10) - 1;
        const d = parseInt(parts[2], 10);
        const endDate = new Date(y, m, d);
        endDate.setDate(endDate.getDate() - 1);
        
        const yyyy = endDate.getFullYear();
        const mm = String(endDate.getMonth() + 1).padStart(2, '0');
        const dd = String(endDate.getDate()).padStart(2, '0');
        return { start: startDateStr, end: `${yyyy}-${mm}-${dd}` };
    }
    return { start: startDateStr, end: startDateStr };
};

const checkOverlap = (start1: string, end1: string, start2: string, end2: string): boolean => {
    const range1 = getOccupiedRange(start1, end1);
    const range2 = getOccupiedRange(start2, end2);
    return range1.start <= range2.end && range2.start <= range1.end;
};

interface BlockDatesModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
    units: Unit[];
    initialStartDate?: Date | null;
    occupancies: Occupancy[];
}

interface BlockForm {
    unitId: string;
    checkIn: string;
    checkOut: string;
    reason: string;
}

interface FormErrors {
    unitId?: string;
    checkIn?: string;
    checkOut?: string;
    reason?: string;
}

export default function BlockDatesModal({
    isOpen,
    onClose,
    onSuccess,
    units,
    initialStartDate,
    occupancies
}: BlockDatesModalProps) {
    const [form, setForm] = useState<BlockForm>(() => ({
        unitId: units[0]?.id || "",
        checkIn: initialStartDate ? format(initialStartDate, "yyyy-MM-dd") : "",
        checkOut: initialStartDate ? format(initialStartDate, "yyyy-MM-dd") : "",
        reason: ""
    }));

    const [errors, setErrors] = useState<FormErrors>({});
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [submitError, setSubmitError] = useState<string | null>(null);
    const {t} = useTranslation();


    if (!isOpen) return null;

    const validate = (): boolean => {
        const tempErrors: FormErrors = {};
        if (!form.unitId) tempErrors.unitId = t('blockDates.selectUnit');
        
        if (!form.checkIn) {
            tempErrors.checkIn = t('blockDates.startDateRequired');
        }

        if (!form.checkOut) {
            tempErrors.checkOut = t('blockDates.endDateRequired');
        } else if (form.checkIn && form.checkOut < form.checkIn) {
            tempErrors.checkOut = t('blockDates.endBeforeStart');
        } else if (form.unitId && form.checkIn && form.checkOut) {
            const hasOverlap = occupancies.some(occ => {
                if (occ.unitId !== form.unitId) return false;
                if (occ.status === "CANCELLED" || occ.status === "COMPLETED") return false;
                return checkOverlap(form.checkIn, form.checkOut, occ.startDate, occ.endDate);
            });
            if (hasOverlap) {
                tempErrors.checkIn = t('blockDates.overlap');
                tempErrors.checkOut = t('blockDates.overlap');
            }
        }

        if (!form.reason.trim()) {
            tempErrors.reason = t('blockDates.reasonRequired')
        } else if (form.reason.trim().length < 3) {
            tempErrors.reason = t('blockDates.reasonTooShort');
        }

        setErrors(tempErrors);
        return Object.keys(tempErrors).length === 0;
    };

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
        const { name, value } = e.target;
        setForm(prev => ({ ...prev, [name]: value }));
        if (errors[name as keyof FormErrors]) {
            setErrors(prev => ({ ...prev, [name]: undefined }));
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!validate()) return;

        setIsSubmitting(true);
        setSubmitError(null);

        try {
            await createBlock(
                form.unitId,
                form.checkIn,
                form.checkOut,
                form.reason.trim()
            );
            onSuccess();
            onClose();
        } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : t('blockDates.failed');
            setSubmitError(msg);
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="fixed inset-0 z-[9998] flex items-center justify-center p-4">
            <div className="fixed inset-0 bg-black/50 backdrop-blur-sm transition-opacity" onClick={onClose} />
            
            <div className="relative bg-[#FFFFFF] rounded-2xl shadow-2xl border border-brand-accent w-full max-w-md overflow-hidden flex flex-col z-[9999] animate-in fade-in zoom-in-95 duration-200">
                <div className="px-6 py-5 border-b border-brand-accent flex items-center justify-between">
                    <div>
                        <h2 className="text-xl font-black text-brand-main tracking-tight">{t('blockDates.title')}</h2>
                        <p className="text-xs text-brand-muted mt-0.5">{t('blockDates.subtitle')}</p>
                    </div>
                    <button onClick={onClose} className="p-2 text-brand-muted hover:text-brand-main hover:bg-brand-bg rounded-full transition-colors">
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="p-6 space-y-5">
                    {submitError && (
                        <div className="p-4 bg-red-50 border-l-4 border-red-500 rounded-r-xl text-red-700 text-sm font-semibold flex gap-2">
                            <svg className="w-5 h-5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                                <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd"/>
                            </svg>
                            <span>{submitError}</span>
                        </div>
                    )}

                    <div className="space-y-1">
                        <label htmlFor="unitId" className="block text-xs font-bold text-brand-muted uppercase tracking-wider">{t('blockDates.propertyUnit')}</label>
                        <select
                            id="unitId"
                            name="unitId"
                            value={form.unitId}
                            onChange={handleChange}
                            className={`w-full px-4 py-2.5 border rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary transition-all font-semibold ${
                                errors.unitId ? "border-red-500" : "border-brand-accent"
                            }`}
                        >
                            <option value="">{t('blockDates.selectUnitOption')}</option>
                            {units.map(unit => (
                                <option key={unit.id} value={unit.id}>{unit.name}</option>
                            ))}
                        </select>
                        {errors.unitId && <p className="text-xs font-semibold text-red-500">{errors.unitId}</p>}
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                        <div className="space-y-1">
                            <label htmlFor="checkIn" className="block text-xs font-bold text-brand-muted uppercase tracking-wider">{t('blockDates.startDate')}</label>
                            <SharedDatePicker
                                selected={parseDateString(form.checkIn)}
                                onChange={(date) => {
                                    const formatted = date ? format(date, "yyyy-MM-dd") : "";
                                    setForm(prev => ({ ...prev, checkIn: formatted }));
                                    if (errors.checkIn) {
                                        setErrors(prev => ({ ...prev, checkIn: undefined }));
                                    }
                                }}
                                selectsStart
                                startDate={parseDateString(form.checkIn)}
                                endDate={parseDateString(form.checkOut)}
                                placeholderText={t('blockDates.selectStartDate')}
                                className={`w-full px-4 py-2 border rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary transition-all text-sm font-semibold cursor-pointer text-left ${
                                    errors.checkIn ? "border-red-500" : "border-brand-accent"
                                }`}
                                wrapperClassName="w-full"
                            />
                            {errors.checkIn && <p className="text-xs font-semibold text-red-500">{errors.checkIn}</p>}
                        </div>

                        <div className="grid grid-cols-1 space-y-1">
                            <label htmlFor="checkOut" className="block text-xs font-bold text-brand-muted uppercase tracking-wider">{t('blockDates.endDate')}</label>
                            <SharedDatePicker
                                selected={parseDateString(form.checkOut)}
                                onChange={(date) => {
                                    const formatted = date ? format(date, "yyyy-MM-dd") : "";
                                    setForm(prev => ({ ...prev, checkOut: formatted }));
                                    if (errors.checkOut) {
                                        setErrors(prev => ({ ...prev, checkOut: undefined }));
                                    }
                                }}
                                selectsEnd
                                startDate={parseDateString(form.checkIn)}
                                endDate={parseDateString(form.checkOut)}
                                minDate={parseDateString(form.checkIn)}
                                placeholderText={t('blockDates.selectEndDate')}
                                className={`w-full px-4 py-2 border rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary transition-all text-sm font-semibold cursor-pointer text-left ${
                                    errors.checkOut ? "border-red-500" : "border-brand-accent"
                                }`}
                                wrapperClassName="w-full"
                            />
                            {errors.checkOut && <p className="text-xs font-semibold text-red-500">{errors.checkOut}</p>}
                        </div>
                    </div>

                    <div className="space-y-1">
                        <label htmlFor="reason" className="block text-xs font-bold text-brand-muted uppercase tracking-wider">{t('blockDates.reason')}</label>
                        <textarea
                            id="reason"
                            name="reason"
                            value={form.reason}
                            onChange={handleChange}
                            rows={3}
                            placeholder={t('blockDates.reasonPlaceholder')}
                            className={`w-full px-4 py-2.5 border rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary transition-all text-sm resize-none ${
                                errors.reason ? "border-red-500" : "border-brand-accent"
                            }`}
                        />
                        {errors.reason && <p className="text-xs font-semibold text-red-500">{errors.reason}</p>}
                    </div>

                    <div className="pt-4 border-t border-brand-accent flex items-center justify-end gap-3 bg-[#FFFFFF]">
                        <button
                            type="button"
                            onClick={onClose}
                            className="px-4 py-2.5 border border-brand-accent rounded-lg text-brand-main font-bold hover:bg-gray-50 text-sm transition"
                        >
                            {t('common.cancel')}
                        </button>
                        <button
                            type="submit"
                            disabled={isSubmitting}
                            className={`px-5 py-2.5 bg-brand-primary text-white font-bold text-sm rounded-lg hover:bg-brand-primary-hover shadow-sm transition flex items-center justify-center gap-1.5 ${
                                isSubmitting ? "opacity-75 cursor-wait" : ""
                            }`}
                        >
                            {isSubmitting ? (
                                <>
                                    <svg className="animate-spin h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                                    </svg>
                                    {t('blockDates.blocking')}
                                </>
                            ) : (
                                t('blockDates.apply')
                            )}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
