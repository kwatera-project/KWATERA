import React, { useState, useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { createReservation } from "../api/reservationApi";
import { createCheckoutSession } from "../api/billingApi";
import { getUserProfile } from "../api/userApi";
import type { Property, Unit } from "../types/property";
import { format, parseISO } from "date-fns";
import {useTranslation} from "react-i18next"

interface CheckoutState {
    property: Property;
    unit: Unit;
    checkIn: string;
    checkOut: string;
    nights: number;
    totalPrice: number;
    currency: string;
}

interface GuestForm {
    firstName: string;
    lastName: string;
    email: string;
    phone: string;
    message: string;
    adults: number | "";
    children: number | "";
    estimatedArrivalTime: string;
    needInvoice: boolean;
    companyName: string;
    taxId: string;
    companyAddress: string;
    acceptRules: boolean;
    acceptPrivacy: boolean;
}

interface FormErrors {
    firstName?: string;
    lastName?: string;
    email?: string;
    phone?: string;
    capacity?: string;
    companyName?: string;
    taxId?: string;
    companyAddress?: string;
}

export default function CheckoutPage() {
    const location = useLocation();
    const navigate = useNavigate();
    const state = location.state as CheckoutState | null;

    const [form, setForm] = useState<GuestForm>({
        firstName: "",
        lastName: "",
        email: "",
        phone: "",
        message: "",
        adults: 1,
        children: 0,
        estimatedArrivalTime: "Not sure yet",
        needInvoice: false,
        companyName: "",
        taxId: "",
        companyAddress: "",
        acceptRules: false,
        acceptPrivacy: false
    });
    const [errors, setErrors] = useState<FormErrors>({});
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [submitError, setSubmitError] = useState<string | null>(null);
    const {t} = useTranslation();

    useEffect(() => {
        getUserProfile()
            .then(profile => {
                setForm(prev => ({
                    ...prev,
                    firstName: profile.firstName || "",
                    lastName: profile.lastName || "",
                    email: profile.email || ""
                }));
            })
            .catch(() => {
            });
    }, []);

    if (!state) {
        return (
            <div className="max-w-md mx-auto my-16 p-8 bg-white border border-brand-accent rounded-xl shadow-sm text-center space-y-6">
                <div className="flex justify-center">
                    <div className="p-3 bg-red-50 rounded-full text-red-500">
                        <svg className="w-8 h-8" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                        </svg>
                    </div>
                </div>
                <h2 className="text-2xl font-bold text-brand-main tracking-tight">{t('checkout.noBooking')}</h2>
                <p className="text-sm text-brand-muted leading-relaxed">{t('checkout.noBookingDesc')}</p>
                <button
                    onClick={() => navigate("/properties")}
                    className="w-full py-3 bg-brand-primary text-white font-bold rounded-lg hover:bg-brand-primary-hover transition-colors shadow-sm focus:outline-none"
                >
                    {t('checkout.browseProperties')}
                </button>
            </div>
        );
    }

    const { property, unit, checkIn, checkOut, nights, totalPrice, currency } = state;

    const validate = (): boolean => {
        const tempErrors: FormErrors = {};
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

        const adultsVal = form.adults === "" ? 0 : Number(form.adults);
        const childrenVal = form.children === "" ? 0 : Number(form.children);
        const totalGuests = adultsVal + childrenVal;

        if (form.adults === "" || adultsVal < 1) {
            tempErrors.capacity = t('checkout.adultRequired');
        } else if (totalGuests > unit.capacity) {
            tempErrors.capacity = t('checkout.capacityExceeded', {totalGuests, capacity: unit.capacity});
        }

        if (form.needInvoice) {
            if (!form.companyName.trim()) {
                tempErrors.companyName = t('checkout.companyNameRequired');
            }
            if (!form.taxId.trim()) {
                tempErrors.taxId = t('checkout.taxIdRequired');
            } else if (form.taxId.trim().length < 5) {
                tempErrors.taxId = t('checkout.taxIdTooShort');
            }
            if (!form.companyAddress.trim()) {
                tempErrors.companyAddress = t('checkout.companyAddressRequired');
            }
        }

        setErrors(tempErrors);
        return Object.keys(tempErrors).length === 0;
    };

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
        const { name, value, type } = e.target;
        const checked = (e.target as HTMLInputElement).checked;

        let finalValue: string | number | boolean;
        if (type === "checkbox") {
            finalValue = checked;
        } else if (type === "number") {
            finalValue = value === "" ? "" : Number(value);
        } else {
            finalValue = value;
        }

        setForm(prev => ({
            ...prev,
            [name]: finalValue
        }));

        if (errors[name as keyof FormErrors]) {
            setErrors(prev => ({ ...prev, [name]: undefined }));
        }

        if (name === "adults" || name === "children") {
            setErrors(prev => ({ ...prev, capacity: undefined }));
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!validate()) return;
        if (!form.acceptRules || !form.acceptPrivacy) return;

        setIsSubmitting(true);
        setSubmitError(null);

        const adultsVal = form.adults === "" ? 0 : Number(form.adults);
        const childrenVal = form.children === "" ? 0 : Number(form.children);
        const totalGuests = adultsVal + childrenVal;

        const extraPayload: Record<string, unknown> = {
            guestDetails: {
                firstName: form.firstName.trim(),
                lastName: form.lastName.trim(),
                email: form.email.trim(),
                phone: form.phone.trim(),
            },
            guestMessage: form.message.trim() || undefined,
            adults: adultsVal,
            children: childrenVal,
            totalGuests,
            estimatedArrivalTime: form.estimatedArrivalTime,
            needInvoice: form.needInvoice,
            invoiceDetails: form.needInvoice ? {
                companyName: form.companyName.trim(),
                taxId: form.taxId.trim(),
                companyAddress: form.companyAddress.trim()
            } : null
        };

        try {
            const reservation = await createReservation(unit.id, checkIn, checkOut, currency, extraPayload);

            const checkoutUrl = await createCheckoutSession(reservation.id, {
                type: "ACCOMMODATION",
                description: `Accommodation fee at ${property.title} - ${unit.name}`,
                quantity: 1,
                unitPrice: reservation.totalPrice
            });

            window.location.assign(checkoutUrl);
        } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : t('checkout.unexpectedError');
            setSubmitError(msg);
            setIsSubmitting(false);
        }
    };

    const formatDate = (dateStr: string) => {
        try {
            return format(parseISO(dateStr), "EEEE, MMM dd, yyyy");
        } catch {
            return dateStr;
        }
    };

    const displayNightPrice = unit.convertedPricePerNight && unit.currencyInfo && unit.currencyInfo.displayCurrency !== "PLN"
        ? `${unit.convertedPricePerNight.toFixed(2)} ${unit.currencyInfo.displayCurrency}`
        : `${unit.pricePerNight} PLN`;

    const displayTotalPrice = unit.convertedPricePerNight && unit.currencyInfo && unit.currencyInfo.displayCurrency !== "PLN"
        ? `${(nights * unit.convertedPricePerNight).toFixed(2)} ${unit.currencyInfo.displayCurrency}`
        : `${totalPrice} PLN`;

    const arrivalTimeOptions = [
        "12:00 - 13:00",
        "13:00 - 14:00",
        "14:00 - 15:00",
        "15:00 - 16:00",
        "16:00 - 17:00",
        "17:00 - 18:00",
        "18:00 - 19:00",
        "19:00 - 20:00",
        "After 20:00",
        "Not sure yet"
    ];

    const isLegalUnchecked = !form.acceptRules || !form.acceptPrivacy;

    return (
        <div className="max-w-7xl mx-auto p-4 sm:p-8 min-h-screen text-brand-main space-y-8">
            <div className="flex items-center gap-2 text-sm text-brand-muted">
                <button
                    onClick={() => navigate(-1)}
                    className="flex items-center gap-1 hover:text-brand-main transition-colors focus:outline-none"
                >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
                    </svg>
                    {t('checkout.backToProperty')}
                </button>
            </div>

            <div className="border-b border-brand-accent pb-6">
                <h1 className="text-3xl font-bold tracking-tight text-brand-main">{t('checkout.title')}</h1>
                <p className="text-sm text-brand-muted mt-1">{t('checkout.subtitle')}</p>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
                <div className="lg:col-span-2 space-y-6">
                    <form onSubmit={handleSubmit} className="bg-white border border-brand-accent rounded-xl shadow-sm p-6 sm:p-8 space-y-8">
                        
                        <div className="space-y-6">
                            <div className="flex items-center gap-2 border-b border-brand-accent pb-3">
                                <svg className="w-5 h-5 text-brand-primary" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                                </svg>
                                <h2 className="text-xl font-bold text-brand-main">{t('manualReservation.guestInfo')}</h2>
                            </div>

                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                <div className="space-y-1">
                                    <label htmlFor="firstName" className="block text-xs font-bold text-brand-muted uppercase tracking-wider">{t('register.firstName')}</label>
                                    <input
                                        type="text"
                                        id="firstName"
                                        name="firstName"
                                        value={form.firstName}
                                        onChange={handleChange}
                                        className={`w-full px-4 py-2.5 border rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary transition-all ${
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
                                        className={`w-full px-4 py-2.5 border rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary transition-all ${
                                            errors.lastName ? "border-red-500" : "border-brand-accent"
                                        }`}
                                    />
                                    {errors.lastName && <p className="text-xs font-semibold text-red-500">{errors.lastName}</p>}
                                </div>
                            </div>

                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                <div className="space-y-1">
                                    <label htmlFor="email" className="block text-xs font-bold text-brand-muted uppercase tracking-wider">{t('login.email')}</label>
                                    <input
                                        type="email"
                                        id="email"
                                        name="email"
                                        value={form.email}
                                        onChange={handleChange}
                                        className={`w-full px-4 py-2.5 border rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary transition-all ${
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
                                        className={`w-full px-4 py-2.5 border rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary transition-all ${
                                            errors.phone ? "border-red-500" : "border-brand-accent"
                                        }`}
                                    />
                                    {errors.phone && <p className="text-xs font-semibold text-red-500">{errors.phone}</p>}
                                </div>
                            </div>
                        </div>

                        <div className="space-y-6 pt-4">
                            <div className="flex items-center gap-2 border-b border-brand-accent pb-3">
                                <svg className="w-5 h-5 text-brand-primary" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                                </svg>
                                <h2 className="text-xl font-bold text-brand-main">{t('checkout.stayGuestDetails')}</h2>
                            </div>

                            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                                <div className="space-y-1">
                                    <label htmlFor="adults" className="block text-xs font-bold text-brand-muted uppercase tracking-wider">{t('checkout.adults')}</label>
                                    <input
                                        type="number"
                                        id="adults"
                                        name="adults"
                                        value={form.adults}
                                        onChange={handleChange}
                                        min="1"
                                        className="w-full px-4 py-2.5 border border-brand-accent rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary transition-all font-medium"
                                    />
                                </div>

                                <div className="space-y-1">
                                    <label htmlFor="children" className="block text-xs font-bold text-brand-muted uppercase tracking-wider">{t('checkout.children')}</label>
                                    <input
                                        type="number"
                                        id="children"
                                        name="children"
                                        value={form.children}
                                        onChange={handleChange}
                                        min="0"
                                        className="w-full px-4 py-2.5 border border-brand-accent rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary transition-all font-medium"
                                    />
                                </div>

                                <div className="space-y-1">
                                    <label htmlFor="estimatedArrivalTime" className="block text-xs font-bold text-brand-muted uppercase tracking-wider">{t('checkout.arrivalTime')}</label>
                                    <select
                                        id="estimatedArrivalTime"
                                        name="estimatedArrivalTime"
                                        value={form.estimatedArrivalTime}
                                        onChange={handleChange}
                                        className="w-full px-4 py-2.5 border border-brand-accent rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary transition-all font-medium"
                                    >
                                        {arrivalTimeOptions.map(opt => (
                                            <option key={opt} value={opt}>
                                                {opt === "After 20:00"
                                                    ? t("checkout.after20")
                                                    : opt === "Not sure yet"
                                                        ? t("checkout.notSureYet")
                                                        : opt}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                            </div>

                            {errors.capacity && (
                                <p className="text-xs font-bold text-red-500 bg-red-50 border border-red-200 px-3 py-2 rounded-lg">{errors.capacity}</p>
                            )}

                            <div className="space-y-1">
                                <label htmlFor="message" className="block text-xs font-bold text-brand-muted uppercase tracking-wider">{t('checkout.messageHost')}</label>
                                <textarea
                                    id="message"
                                    name="message"
                                    value={form.message}
                                    onChange={handleChange}
                                    placeholder={t('checkout.messagePlaceholder')}
                                    rows={3}
                                    className="w-full px-4 py-2.5 border border-brand-accent rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary transition-all resize-y text-sm"
                                />
                            </div>
                        </div>

                        <div className="space-y-6 pt-4">
                            <div className="flex items-center gap-2 border-b border-brand-accent pb-3">
                                <svg className="w-5 h-5 text-brand-primary" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                                </svg>
                                <h2 className="text-xl font-bold text-brand-main">{t('checkout.billingDetails')}</h2>
                            </div>

                            <div className="flex items-center gap-3">
                                <input
                                    type="checkbox"
                                    id="needInvoice"
                                    name="needInvoice"
                                    checked={form.needInvoice}
                                    onChange={handleChange}
                                    className="w-5 h-5 accent-brand-primary rounded border-brand-accent focus:ring-brand-primary text-brand-primary cursor-pointer"
                                />
                                <label htmlFor="needInvoice" className="text-sm font-bold text-brand-main select-none cursor-pointer">
                                    {t('checkout.needInvoice')}
                                </label>
                            </div>

                            {form.needInvoice && (
                                <div className="p-5 bg-brand-bg/60 border border-brand-accent rounded-xl grid grid-cols-1 sm:grid-cols-2 gap-4 animate-fade-in">
                                    <div className="sm:col-span-2">
                                        <p className="text-xs text-brand-muted font-bold mb-1 uppercase tracking-wider">{t('checkout.invoiceFields')}</p>
                                    </div>
                                    
                                    <div className="space-y-1">
                                        <label htmlFor="companyName" className="block text-xs font-bold text-brand-muted uppercase tracking-wider">{t('checkout.companyName')}</label>
                                        <input
                                            type="text"
                                            id="companyName"
                                            name="companyName"
                                            value={form.companyName}
                                            onChange={handleChange}
                                            placeholder={t("checkout.companyNamePlaceholder")}
                                            className={`w-full px-4 py-2 border rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary transition-all text-sm ${
                                                errors.companyName ? "border-red-500" : "border-brand-accent"
                                            }`}
                                        />
                                        {errors.companyName && <p className="text-xs font-semibold text-red-500">{errors.companyName}</p>}
                                    </div>

                                    <div className="space-y-1">
                                        <label htmlFor="taxId" className="block text-xs font-bold text-brand-muted uppercase tracking-wider">{t('checkout.taxId')}</label>
                                        <input
                                            type="text"
                                            id="taxId"
                                            name="taxId"
                                            value={form.taxId}
                                            onChange={handleChange}
                                            placeholder={t("checkout.taxIdPlaceholder")}
                                            className={`w-full px-4 py-2 border rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary transition-all text-sm ${
                                                errors.taxId ? "border-red-500" : "border-brand-accent"
                                            }`}
                                        />
                                        {errors.taxId && <p className="text-xs font-semibold text-red-500">{errors.taxId}</p>}
                                    </div>

                                    <div className="sm:col-span-2 space-y-1">
                                        <label htmlFor="companyAddress" className="block text-xs font-bold text-brand-muted uppercase tracking-wider">{t('checkout.companyAddress')}</label>
                                        <input
                                            type="text"
                                            id="companyAddress"
                                            name="companyAddress"
                                            value={form.companyAddress}
                                            onChange={handleChange}
                                            placeholder={t("checkout.companyAddressPlaceholder")}
                                            className={`w-full px-4 py-2 border rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary transition-all text-sm ${
                                                errors.companyAddress ? "border-red-500" : "border-brand-accent"
                                            }`}
                                        />
                                        {errors.companyAddress && <p className="text-xs font-semibold text-red-500">{errors.companyAddress}</p>}
                                    </div>
                                </div>
                            )}
                        </div>

                        <div className="space-y-4 pt-4 border-t border-brand-accent">
                            <p className="text-xs font-bold text-brand-muted uppercase tracking-wider mb-2">{t('checkout.legal')}</p>
                            
                            <div className="flex items-start gap-3">
                                <input
                                    type="checkbox"
                                    id="acceptRules"
                                    name="acceptRules"
                                    checked={form.acceptRules}
                                    onChange={handleChange}
                                    className="w-5 h-5 accent-brand-primary rounded border-brand-accent focus:ring-brand-primary text-brand-primary mt-0.5 cursor-pointer flex-shrink-0"
                                />
                                <label htmlFor="acceptRules" className="text-sm text-brand-muted font-medium select-none cursor-pointer leading-tight">
                                    {t('checkout.acceptRules')} <a href="#" className="text-brand-primary font-bold hover:underline" onClick={(e) => e.preventDefault()}>{t('checkout.houseRules')}</a> {t('checkout.acceptRulesAnd')} <a href="#" className="text-brand-primary font-bold hover:underline" onClick={(e) => e.preventDefault()}>{t('checkout.terms')}</a>. <span className="text-red-500">*</span>
                                </label>
                            </div>

                            <div className="flex items-start gap-3">
                                <input
                                    type="checkbox"
                                    id="acceptPrivacy"
                                    name="acceptPrivacy"
                                    checked={form.acceptPrivacy}
                                    onChange={handleChange}
                                    className="w-5 h-5 accent-brand-primary rounded border-brand-accent focus:ring-brand-primary text-brand-primary mt-0.5 cursor-pointer flex-shrink-0"
                                />
                                <label htmlFor="acceptPrivacy" className="text-sm text-brand-muted font-medium select-none cursor-pointer leading-tight">
                                    {t('checkout.acceptPrivacy')}{" "}<a href="#" className="text-brand-primary font-bold hover:underline" onClick={(e) => e.preventDefault()}>{t('checkout.privacy')}</a> {t('checkout.acceptPrivacyEnd')}<span className="text-red-500">*</span>
                                </label>
                            </div>
                        </div>

                        {submitError && (
                            <div className="flex items-center gap-3 p-4 bg-red-50 border-l-4 border-red-500 rounded-r-xl text-red-700 animate-fade-in shadow-sm">
                                <svg className="w-5 h-5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                                    <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd"/>
                                </svg>
                                <span className="text-sm font-semibold leading-relaxed">{submitError}</span>
                            </div>
                        )}

                        <div className="pt-4 border-t border-brand-accent hidden lg:block">
                            <button
                                type="submit"
                                disabled={isSubmitting || isLegalUnchecked}
                                className={`w-full py-3.5 px-6 font-bold rounded-lg shadow-sm transition-all duration-200 flex items-center justify-center gap-2 cursor-pointer ${
                                    isSubmitting
                                        ? "bg-brand-muted text-white cursor-wait"
                                        : isLegalUnchecked
                                            ? "bg-brand-accent text-brand-muted opacity-60 cursor-not-allowed"
                                            : "bg-brand-primary text-white hover:bg-brand-primary-hover active:scale-[0.99]"
                                }`}
                            >
                                {isSubmitting ? (
                                    <>
                                        <svg className="animate-spin h-5 w-5 text-white" fill="none" viewBox="0 0 24 24">
                                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                                        </svg>
                                        {t('checkout.processing')}
                                    </>
                                ) : (
                                    <>
                                        <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                                        </svg>
                                        {t('checkout.confirmPay')}
                                    </>
                                )}
                            </button>
                            {isLegalUnchecked && (
                                <p className="text-center text-xs text-brand-muted mt-2 font-medium">{t('checkout.acceptLegalHint')}</p>
                            )}
                        </div>
                    </form>
                </div>

                <div className="lg:sticky lg:top-8 space-y-6">
                    <div className="bg-white border border-brand-accent rounded-xl shadow-sm overflow-hidden">
                        <img
                            src={unit.imageUrl || property.imageUrl}
                            alt={property.title}
                            className="w-full h-48 object-cover border-b border-brand-accent"
                        />
                        <div className="p-6 space-y-6">
                            <div>
                                <h3 className="font-bold text-lg leading-snug">{property.title}</h3>
                                <p className="text-xs text-brand-muted font-medium mt-0.5">{property.city}, {property.country}</p>
                                <div className="mt-3 inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-brand-bg border border-brand-accent text-brand-muted">
                                    {t('checkout.unit')}: {unit.name}
                                </div>
                            </div>

                            <hr className="border-brand-accent" />

                            <div className="space-y-4 text-sm">
                                <h4 className="font-bold text-xs uppercase tracking-wider text-brand-muted">{t('checkout.stayDetails')}</h4>
                                
                                <div className="flex justify-between items-start gap-4">
                                    <span className="text-brand-muted font-medium">{t('manualReservation.checkIn')}</span>
                                    <span className="font-semibold text-right">{formatDate(checkIn)}</span>
                                </div>

                                <div className="flex justify-between items-start gap-4">
                                    <span className="text-brand-muted font-medium">{t('manualReservation.checkOut')}</span>
                                    <span className="font-semibold text-right">{formatDate(checkOut)}</span>
                                </div>

                                <div className="flex justify-between items-start gap-4 border-t border-dashed border-brand-accent pt-3">
                                    <span className="text-brand-muted font-medium">{t('checkout.duration')}</span>
                                    <span className="font-semibold text-brand-main">{nights} {t('checkout.night', {count: nights})}</span>
                                </div>

                                <div className="flex justify-between items-start gap-4 border-t border-dashed border-brand-accent pt-3">
                                    <span className="text-brand-muted font-medium">{t('checkout.guestSelection')}</span>
                                    <span className="font-semibold text-brand-main">
                                        {form.adults !== "" ? `${form.adults} ${t('checkout.adult', {count: Number(form.adults)})}` : `0 ${t('checkout.adult', {count: 0})}`}
                                        {form.children !== "" && Number(form.children) > 0 ? `, ${form.children} ${t('checkout.child', {count: Number(form.children)})}` : ""}
                                    </span>
                                </div>
                            </div>

                            <hr className="border-brand-accent" />

                            <div className="space-y-4 text-sm">
                                <h4 className="font-bold text-xs uppercase tracking-wider text-brand-muted">{t('checkout.priceDetails')}</h4>

                                <div className="flex justify-between items-center">
                                    <span className="text-brand-muted font-medium">{displayNightPrice} x {nights} {t('checkout.nights')}</span>
                                    <span className="font-semibold">{displayTotalPrice}</span>
                                </div>

                                <div className="flex justify-between items-center border-t border-brand-accent pt-4 text-base font-bold text-brand-primary">
                                    <span>{t('checkout.totalPrice')}</span>
                                    <span className="text-lg">{displayTotalPrice}</span>
                                </div>
                            </div>

                            <div className="pt-2 block lg:hidden">
                                <button
                                    onClick={handleSubmit}
                                    disabled={isSubmitting || isLegalUnchecked}
                                    className={`w-full py-3.5 px-6 font-bold rounded-lg shadow-sm transition-all duration-200 flex items-center justify-center gap-2 cursor-pointer ${
                                        isSubmitting
                                            ? "bg-brand-muted text-white cursor-wait"
                                            : isLegalUnchecked
                                                ? "bg-brand-accent text-brand-muted opacity-60 cursor-not-allowed"
                                                : "bg-brand-primary text-white hover:bg-brand-primary-hover active:scale-[0.99]"
                                    }`}
                                >
                                    {isSubmitting ? (
                                        <>
                                            <svg className="animate-spin h-5 w-5 text-white" fill="none" viewBox="0 0 24 24">
                                                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                                            </svg>
                                            {t('checkout.processingShort')}
                                        </>
                                    ) : (
                                        <>
                                            <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                                            </svg>
                                            {t('checkout.confirmPayShort')}
                                        </>
                                    )}
                                </button>
                                {isLegalUnchecked && (
                                    <p className="text-center text-xs text-brand-muted mt-2 font-medium">{t('checkout.acceptLegalHintMobile')}</p>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
