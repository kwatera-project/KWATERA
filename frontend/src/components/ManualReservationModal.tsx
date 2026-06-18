import React, { useState } from "react";
import { createManualReservation } from "../api/reservationApi";
import { format, isAfter, addDays } from "date-fns";
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

interface ManualReservationModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
    units: Unit[];
    initialStartDate?: Date | null;
    occupancies: Occupancy[];
}

interface ReservationForm {
    unitId: string;
    firstName: string;
    lastName: string;
    email: string;
    phone: string;
    checkIn: string;
    checkOut: string;
    note: string;
}

interface FormErrors {
    unitId?: string;
    firstName?: string;
    lastName?: string;
    email?: string;
    phone?: string;
    checkIn?: string;
    checkOut?: string;
}

export default function ManualReservationModal({
    isOpen,
    onClose,
    onSuccess,
    units,
    initialStartDate,
    occupancies
}: ManualReservationModalProps) {
    const [form, setForm] = useState<ReservationForm>(() => ({
        unitId: units[0]?.id || "",
        firstName: "",
        lastName: "",
        email: "",
        phone: "",
        checkIn: initialStartDate ? format(initialStartDate, "yyyy-MM-dd") : "",
        checkOut: initialStartDate ? format(addDays(initialStartDate, 1), "yyyy-MM-dd") : "",
        note: ""
    }));

    const [errors, setErrors] = useState<FormErrors>({});
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [submitError, setSubmitError] = useState<string | null>(null);
    const {t} = useTranslation();

    if (!isOpen) return null;

    const validate = (): boolean => {
        const tempErrors: FormErrors = {};
        if (!form.unitId) tempErrors.unitId = t('blockDates.selectUnit');
        
        if (!form.firstName.trim()) {
            tempErrors.firstName = t('manualReservation.firstNameRequired');
        } else if (form.firstName.trim().length < 2) {
            tempErrors.firstName = t('manualReservation.minTwoChars');
        }

        if (!form.lastName.trim()) {
            tempErrors.lastName = t('manualReservation.lastNameRequired');
        } else if (form.lastName.trim().length < 2) {
            tempErrors.lastName = t('manualReservation.minTwoChars');
        }

        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!form.email.trim()) {
            tempErrors.email = t('manualReservation.emailRequired');
        } else if (!emailRegex.test(form.email)) {
            tempErrors.email = t('manualReservation.emailInvalid');
        }

        const phoneRegex = /^\+?[0-9\s\-()]{7,20}$/;
        if (!form.phone.trim()) {
            tempErrors.phone = t('manualReservation.phoneRequired');
        } else if (!phoneRegex.test(form.phone)) {
            tempErrors.phone = t('manualReservation.phoneInvalid');
        }

        if (!form.checkIn) {
            tempErrors.checkIn =t('manualReservation.checkInRequired');
        }

        if (!form.checkOut) {
            tempErrors.checkOut =  t('manualReservation.checkOutRequired');
        } else if (form.checkIn && !isAfter(new Date(form.checkOut), new Date(form.checkIn))) {
            tempErrors.checkOut = t('manualReservation.checkOutAfterCheckIn');
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
            await createManualReservation(
                form.unitId,
                form.checkIn,
                form.checkOut,
                form.email.trim(),
                {
                    firstName: form.firstName.trim(),
                    lastName: form.lastName.trim(),
                    phone: form.phone.trim(),
                    note: form.note.trim() || undefined
                }
            );
            onSuccess();
            onClose();
        } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : t('manualReservation.failed');
            setSubmitError(msg);
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="fixed inset-0 z-[9998] flex items-center justify-center p-4">
            <div className="fixed inset-0 bg-black/50 backdrop-blur-sm transition-opacity" onClick={onClose} />
            
            <div className="relative bg-[#FFFFFF] rounded-2xl shadow-2xl border border-brand-accent w-full max-w-lg overflow-hidden flex flex-col z-[9999] animate-in fade-in zoom-in-95 duration-200">
                <div className="px-6 py-5 border-b border-brand-accent flex items-center justify-between">
                    <div>
                        <h2 className="text-xl font-black text-brand-main tracking-tight">{t('manualReservation.title')}</h2>
                        <p className="text-xs text-brand-muted mt-0.5">{t('manualReservation.subtitle')}</p>
                    </div>
                    <button onClick={onClose} className="p-2 text-brand-muted hover:text-brand-main hover:bg-brand-bg rounded-full transition-colors">
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-5 max-h-[75vh]">
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

                    <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-1">
                            <label htmlFor="checkIn" className="block text-xs font-bold text-brand-muted uppercase tracking-wider">{t('manualReservation.checkIn')}</label>
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
                                placeholderText={t('manualReservation.selectCheckIn')}
                                className={`w-full px-4 py-2 border rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary transition-all text-sm font-semibold cursor-pointer text-left ${
                                    errors.checkIn ? "border-red-500" : "border-brand-accent"
                                }`}
                                wrapperClassName="w-full"
                            />
                            {errors.checkIn && <p className="text-xs font-semibold text-red-500">{errors.checkIn}</p>}
                        </div>

                        <div className="space-y-1">
                            <label htmlFor="checkOut" className="block text-xs font-bold text-brand-muted uppercase tracking-wider">{t('manualReservation.checkOut')}</label>
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
                                placeholderText={t('manualReservation.selectCheckOut')}
                                className={`w-full px-4 py-2 border rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary transition-all text-sm font-semibold cursor-pointer text-left ${
                                    errors.checkOut ? "border-red-500" : "border-brand-accent"
                                }`}
                                wrapperClassName="w-full"
                            />
                            {errors.checkOut && <p className="text-xs font-semibold text-red-500">{errors.checkOut}</p>}
                        </div>
                    </div>

                    <div className="border-t border-brand-accent/50 pt-4 space-y-4">
                        <h3 className="text-sm font-black text-brand-main uppercase tracking-wider">{t('manualReservation.guestInfo')}</h3>
                        
                        <div className="grid grid-cols-2 gap-4">
                            <div className="space-y-1">
                                <label htmlFor="firstName" className="block text-xs font-bold text-brand-muted uppercase tracking-wider">{t('register.firstName')}</label>
                                <input
                                    type="text"
                                    id="firstName"
                                    name="firstName"
                                    value={form.firstName}
                                    onChange={handleChange}
                                    className={`w-full px-4 py-2.5 border rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary transition-all text-sm font-medium ${
                                        errors.firstName ? "border-red-500" : "border-brand-accent"
                                    }`}
                                />
                                {errors.firstName && <p className="text-xs font-semibold text-red-500">{errors.firstName}</p>}
                            </div>

                            <div className="space-y-1">
                                <label htmlFor="lastName" className="block text-xs font-bold text-brand-muted uppercase tracking-wider">{t('register.lastName')}</label>
                                <input
                                    type="text"
                                    id="lastName"
                                    name="lastName"
                                    value={form.lastName}
                                    onChange={handleChange}
                                    className={`w-full px-4 py-2.5 border rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary transition-all text-sm font-medium ${
                                        errors.lastName ? "border-red-500" : "border-brand-accent"
                                    }`}
                                />
                                {errors.lastName && <p className="text-xs font-semibold text-red-500">{errors.lastName}</p>}
                            </div>
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                            <div className="space-y-1">
                                <label htmlFor="email" className="block text-xs font-bold text-brand-muted uppercase tracking-wider">{t('login.email')}</label>
                                <input
                                    type="email"
                                    id="email"
                                    name="email"
                                    value={form.email}
                                    onChange={handleChange}
                                    className={`w-full px-4 py-2.5 border rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary transition-all text-sm font-medium ${
                                        errors.email ? "border-red-500" : "border-brand-accent"
                                    }`}
                                />
                                {errors.email && <p className="text-xs font-semibold text-red-500">{errors.email}</p>}
                            </div>

                            <div className="space-y-1">
                                <label htmlFor="phone" className="block text-xs font-bold text-brand-muted uppercase tracking-wider">{t('manualReservation.phone')}</label>
                                <input
                                    type="tel"
                                    id="phone"
                                    name="phone"
                                    value={form.phone}
                                    onChange={handleChange}
                                    placeholder="+48 123 456 789"
                                    className={`w-full px-4 py-2.5 border rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary transition-all text-sm font-medium ${
                                        errors.phone ? "border-red-500" : "border-brand-accent"
                                    }`}
                                />
                                {errors.phone && <p className="text-xs font-semibold text-red-500">{errors.phone}</p>}
                            </div>
                        </div>
                    </div>

                    <div className="space-y-1 border-t border-brand-accent/50 pt-4">
                        <label htmlFor="note" className="block text-xs font-bold text-brand-muted uppercase tracking-wider">{t('manualReservation.note')}</label>
                        <textarea
                            id="note"
                            name="note"
                            value={form.note}
                            onChange={handleChange}
                            rows={3}
                            placeholder={t('manualReservation.notePlaceholder')}
                            className="w-full px-4 py-2 border border-brand-accent rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary transition-all text-sm resize-none"
                        />
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
                                    {t('manualReservation.creating')}
                                </>
                            ) : (
                                t('manualReservation.confirm')
                            )}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
