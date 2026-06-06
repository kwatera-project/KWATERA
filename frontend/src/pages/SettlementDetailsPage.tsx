import {useEffect, useState, useCallback} from "react";
import {useParams, Link} from "react-router-dom";
import {getSettlementDetails} from "../api/settlementApi";
import type {SettlementDetails, SettlementItemDetails} from "../types/settlement";
import {GATEWAY_BASE_URL} from "../api/apiConfig.ts";
import {getReservationDetails} from "../api/reservationApi.ts";
import {getUserRoles} from "../utils/jwtUtils";

export default function SettlementDetailsPage() {
    const {id} = useParams();
    const [settlement, setSettlement] = useState<SettlementDetails | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const [settlementState, setSettlementState] = useState<
        Record<string, { loading: boolean; success?: boolean, error?: string }>
    >({});

    useEffect(() => {
        if (!id) return;

        getSettlementDetails(id)
            .then(setSettlement)
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false));
    }, [id]);

    type PaymentButton = {
        type: string;
        quantity: number;
        unitPrice: number;
    };

    type SettlementItem = {
        settlementItemType: string;
    };

    const [paymentButtons, setPaymentButtons] = useState<PaymentButton[]>([]);

    const loadPaymentButtons = useCallback((
        settlement: SettlementDetails,
        settlementItemTypes: string[]
    ) => {
        const buttons: PaymentButton[] = [];

        if (settlementItemTypes.includes("DEPOSIT") && settlement.depositAmount > 0) {
            const depositPaid = settlement.items?.some(item => item.type === "DEPOSIT");
            if (!depositPaid) {
                buttons.push({
                    type: "DEPOSIT",
                    quantity: 1,
                    unitPrice: settlement.depositAmount
                });
            }
        }

        if (settlement.accommodationAmount > 0) {
            const accommodationPaid = settlement.items?.some(item => item.type === "ACCOMMODATION");
            if (!accommodationPaid) {
                buttons.push({
                    type: "ACCOMMODATION",
                    quantity: 1,
                    unitPrice: settlement.accommodationAmount
                });
            }
        }

        const utilityTypes = ["WATER", "ELECTRICITY", "CLEANING_FEE"];
        for (const type of utilityTypes) {
            if (!settlementItemTypes.includes(type)) {
                continue;
            }

            const matchingItems = settlement.items?.filter(item => item.type === type) || [];
            if (matchingItems.length === 1) {
                buttons.push({
                    type,
                    quantity: matchingItems[0].quantity,
                    unitPrice: matchingItems[0].unitPrice
                });
            }
        }

        setPaymentButtons(buttons);
    }, []);

    const getUnitSettlementItemsType = useCallback(async (reservationId: string) => {
        try {
            const res = await getReservationDetails(reservationId);
            const unitId = res.unitId;

            const unitSettlementItemsRes = await fetch(
                `${GATEWAY_BASE_URL}/api/properties/units/${unitId}/settlement-items`,
                { method: "GET" }
            )

            if (!unitSettlementItemsRes.ok) {
                throw new Error(`Fetch unit settlement item failed: ${unitSettlementItemsRes.status}`);
            }

            const data = await unitSettlementItemsRes.json();
            return data.map((item: SettlementItem) => item.settlementItemType);

        } catch (err: unknown) {
            const message = err instanceof Error ? err.message : "An error occurred";
            console.error(message);
        }
    }, []);

    useEffect(() => {
        if (!settlement?.reservationId) return;

        getUnitSettlementItemsType(settlement.reservationId)
            .then((types) => {
                if (!types) return;
                loadPaymentButtons(settlement, types);
            });

    }, [settlement, loadPaymentButtons, getUnitSettlementItemsType]);

    const handlePayment = async (reservationId: string,
                                 settlementType: string,
                                 settlementId: string,
                                 quantity: number,
                                 unitPrice: number) => {

        const stateKey = `${settlementId}-${settlementType}`;

        setSettlementState(prev => ({ ...prev, [stateKey]: {loading: true} }));

        try {
            const token = localStorage.getItem("token");
            const name = settlementType[0] + settlementType.slice(1).toLowerCase();

            const checkoutRes = await fetch(
                `${GATEWAY_BASE_URL}/api/billing/checkout/${reservationId}`,
                {
                    method: "POST",
                    headers: {
                        Authorization: `Bearer ${token}`,
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        type: `${settlementType}`,
                        description: `${name} fee`,
                        quantity: quantity,
                        unitPrice: unitPrice
                    })
                }
            );

            if (!checkoutRes.ok) {
                throw new Error(`Checkout failed: ${checkoutRes.status}`);
            }

            const checkoutUrl = await checkoutRes.text();

            try {
                new URL(checkoutUrl);
            } catch {
                throw new Error("Invalid checkout URL received");
            }

            setSettlementState(prev => ({
                ...prev,
                [stateKey]: { loading: false, success: true }
            }));

            setTimeout(() => {
                window.location.assign(checkoutUrl);
            }, 800);

        } catch (err: unknown) {
            const message = err instanceof Error ? err.message : "An error occurred";
            setSettlementState(prev => ({
                ...prev,
                [stateKey]: { loading: false, error: message }
            }));
        }
    };

    const token = localStorage.getItem("token");
    const roles = getUserRoles(token);
    const isAdminOrOwner = roles.includes("ROLE_ADMIN") || roles.includes("ROLE_OWNER");
    const returnPath = isAdminOrOwner ? "/admin/reservations" : "/my-reservations";
    const returnLabel = isAdminOrOwner ? "Back to Reservations Overview" : "Back to My Reservations";

    if (loading) return <div className="p-6">Loading settlement details...</div>;
    if (error) {
        const isForbidden = error.toLowerCase().includes("forbidden") || error.toLowerCase().includes("access denied") || error.toLowerCase().includes("403");

        if (isForbidden) {
            return (
                <div className="max-w-3xl mx-auto p-8 min-h-screen text-[#1A1A1A] space-y-6 flex flex-col">
                    <Link
                        to={returnPath}
                        className="px-4 py-2 text-xs font-bold text-[#42211D] bg-[#F7F7F7] border border-[#DACDCA] hover:bg-gray-100 rounded-lg transition-colors shadow-sm inline-flex items-center gap-1.5 w-fit"
                    >
                        &larr; {returnLabel}
                    </Link>
                    <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-8 text-center space-y-4">
                        <div className="w-16 h-16 bg-red-50 rounded-full flex items-center justify-center mx-auto mb-2">
                            <svg className="w-8 h-8 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 15v2m0-6V9m0 12a9 9 0 110-18 9 9 0 010 18z" />
                            </svg>
                        </div>
                        <h2 className="text-xl font-bold text-gray-900">Access Denied</h2>
                        <p className="text-sm text-[#7A7A7A] max-w-md mx-auto leading-relaxed">
                            You do not have permission to view this settlement billing page. If you believe this is an error, please contact support.
                        </p>
                    </div>
                </div>
            );
        }

        return (
            <div className="max-w-3xl mx-auto p-8 min-h-screen text-[#1A1A1A] space-y-6 flex flex-col">
                <Link
                    to={returnPath}
                    className="px-4 py-2 text-xs font-bold text-[#42211D] bg-[#F7F7F7] border border-[#DACDCA] hover:bg-gray-100 rounded-lg transition-colors shadow-sm inline-flex items-center gap-1.5 w-fit"
                >
                    &larr; {returnLabel}
                </Link>
                <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-6 text-red-600 font-semibold">
                    {error}
                </div>
            </div>
        );
    }
    if (!settlement) {
        return (
            <div className="max-w-3xl mx-auto p-8 min-h-screen text-[#1A1A1A] space-y-6 flex flex-col">
                <Link
                    to={returnPath}
                    className="px-4 py-2 text-xs font-bold text-[#42211D] bg-[#F7F7F7] border border-[#DACDCA] hover:bg-gray-100 rounded-lg transition-colors shadow-sm inline-flex items-center gap-1.5 w-fit"
                >
                    &larr; {returnLabel}
                </Link>
                <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-6 text-gray-500 font-semibold">
                    Settlement not found.
                </div>
            </div>
        );
    }

    const displayCurrency = settlement.currencyInfo?.displayCurrency || 'PLN';

    const renderAmount = (convertedAmount?: number, originalAmount?: number) => {
        if (displayCurrency !== 'PLN' && convertedAmount !== undefined) {
            if (isAdminOrOwner) {
                return (
                    <span>
                        {originalAmount} PLN <span className="text-xs font-semibold text-[#7A7A7A]">({convertedAmount} {displayCurrency})</span>
                    </span>
                );
            }
            return (
                <span>
                    {convertedAmount} {displayCurrency}
                </span>
            );
        }
        return <span>{originalAmount} PLN</span>;
    };

    const isPaidOrZero = settlement.status === "PAID" || settlement.balanceDue === 0;

    return (
        <div className="max-w-3xl mx-auto p-8 min-h-screen text-[#1A1A1A] space-y-6 flex flex-col">
            <Link
                to={returnPath}
                className="px-4 py-2 text-xs font-bold text-[#42211D] bg-[#F7F7F7] border border-[#DACDCA] hover:bg-gray-100 rounded-lg transition-colors shadow-sm inline-flex items-center gap-1.5 w-fit"
            >
                &larr; {returnLabel}
            </Link>

            <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-8 hover:shadow-md transition-all duration-300">
                <div className="border-b border-[#DACDCA] pb-4 mb-6">
                    <h1 className="text-3xl font-bold text-[#1A1A1A] tracking-tight">Settlement Details</h1>
                    <p className="text-sm text-[#7A7A7A] mt-1">Detailed pricing breakdown and settlement transactions.</p>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div className="space-y-1">
                        <p className="text-sm font-semibold text-[#7A7A7A]">Settlement ID</p>
                        <p className="font-mono text-base font-bold text-[#1A1A1A]" title={settlement.id}>
                            #SET-{settlement.id.slice(-8)}
                        </p>
                    </div>
                    <div className="space-y-1">
                        <p className="text-sm font-semibold text-[#7A7A7A]">Reservation ID</p>
                        <p className="font-mono text-base font-bold text-[#1A1A1A]" title={settlement.reservationId}>
                            #RES-{settlement.reservationId.slice(-8)}
                        </p>
                    </div>
                    <div className="space-y-1">
                        <p className="text-sm font-semibold text-[#7A7A7A]">Status</p>
                        <div className="pt-1">
                            <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold border ${
                                settlement.status === 'PAID' ? 'bg-emerald-50 border-emerald-200 text-emerald-800' :
                                'bg-amber-50 border-amber-200 text-amber-800'
                            }`}>
                                {settlement.status}
                            </span>
                        </div>
                    </div>
                    <div className="space-y-1">
                        <p className="text-sm font-semibold text-[#7A7A7A]">Balance Due</p>
                        <p className={`text-xl font-black ${isPaidOrZero ? "text-emerald-700" : "text-red-600 font-bold"}`}>
                            {renderAmount(settlement.convertedBalanceDue, settlement.balanceDue)}
                        </p>
                    </div>

                    {isAdminOrOwner && displayCurrency !== 'PLN' && settlement.currencyInfo && (
                        <div className="space-y-1 md:col-span-2 bg-[#F7F7F7] border border-[#DACDCA] rounded-xl p-4 mt-2">
                            <p className="text-xs font-bold text-[#42211D] uppercase tracking-wider">Currency Snapshot Info (Admin/Owner)</p>
                            <div className="grid grid-cols-2 gap-4 mt-2">
                                <div>
                                    <span className="block text-[10px] font-bold text-[#7A7A7A] uppercase tracking-wider">Guest Selected Currency</span>
                                    <p className="text-sm font-bold text-[#1A1A1A] mt-0.5">{displayCurrency}</p>
                                </div>
                                <div>
                                    <span className="block text-[10px] font-bold text-[#7A7A7A] uppercase tracking-wider">Exchange Rate Snapshot</span>
                                    <p className="text-sm font-bold text-[#1A1A1A] mt-0.5">{settlement.currencyInfo.exchangeRate.toFixed(4)} {displayCurrency}/PLN</p>
                                </div>
                            </div>
                        </div>
                    )}

                    <div className="md:col-span-2 border-t border-[#DACDCA] mt-2 pt-6">
                        <h3 className="text-lg font-bold text-[#1A1A1A] tracking-tight border-b border-[#DACDCA] pb-2 mb-4">Price Breakdown</h3>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                            <div className="space-y-1">
                                <p className="text-sm font-semibold text-[#7A7A7A]">Accommodation Amount</p>
                                <p className="font-bold text-base text-[#1A1A1A]">
                                    {renderAmount(settlement.convertedAccommodationAmount, settlement.accommodationAmount)}
                                </p>
                            </div>
                            <div className="space-y-2">
                                <p className="text-sm font-semibold text-[#7A7A7A]">Utilities Amount</p>
                                <p className="font-bold text-base text-[#1A1A1A]">
                                    {renderAmount(settlement.convertedUtilitiesAmount, settlement.utilitiesAmount)}
                                </p>
                                {settlement.items && settlement.items.length > 0 && (
                                    <div className="pl-4 border-l-2 border-[#DACDCA] mt-2">
                                        <ul className="space-y-1.5">
                                            {settlement.items
                                                .filter((item: SettlementItemDetails) => ["ELECTRICITY", "WATER", "CLEANING_FEE"].includes(item.type))
                                                .map((item: SettlementItemDetails) => {
                                                    const rate = settlement.currencyInfo?.exchangeRate || 1;
                                                    const convertedAmount = displayCurrency !== 'PLN'
                                                        ? Number((item.amount / rate).toFixed(2))
                                                        : item.amount;

                                                    return (
                                                        <li key={item.id} className="text-xs text-[#7A7A7A] flex justify-between gap-4 font-medium">
                                                            <span>
                                                                {item.description || item.type} ({item.quantity} x {displayCurrency !== 'PLN' ? Number((item.unitPrice / rate).toFixed(2)) : item.unitPrice} {displayCurrency})
                                                            </span>
                                                            <span className="font-bold text-[#1A1A1A]">
                                                                {convertedAmount} {displayCurrency}
                                                            </span>
                                                        </li>
                                                    );
                                                })}
                                        </ul>
                                    </div>
                                )}
                            </div>
                            <div className="space-y-1">
                                <p className="text-sm font-semibold text-[#7A7A7A]">Deposit Amount</p>
                                <p className="font-bold text-base text-[#1A1A1A]">
                                    {renderAmount(settlement.convertedDepositAmount, settlement.depositAmount)}
                                </p>
                            </div>
                            <div className="space-y-1">
                                <p className="text-sm font-semibold text-[#7A7A7A]">Total Amount</p>
                                <p className="font-black text-lg text-[#1A1A1A]">
                                    {renderAmount(settlement.convertedTotalAmount, settlement.totalAmount)}
                                </p>
                            </div>
                            <div className="space-y-1 md:col-span-2">
                                <p className="text-sm font-semibold text-[#7A7A7A]">Amount Paid</p>
                                <p className="font-bold text-base text-emerald-700">
                                    {renderAmount(settlement.convertedAmountPaid, settlement.amountPaid)}
                                </p>
                            </div>
                        </div>
                    </div>

                    <div className="md:col-span-2 border-t border-[#DACDCA] mt-2 pt-6 grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div className="space-y-1">
                            <p className="text-sm font-semibold text-[#7A7A7A]">Issued At</p>
                            <p className="font-bold text-base text-[#1A1A1A]">{settlement.issuedAt ? new Date(settlement.issuedAt).toLocaleString() : 'N/A'}</p>
                        </div>
                        <div className="space-y-1">
                            <p className="text-sm font-semibold text-[#7A7A7A]">Paid At</p>
                            <p className="font-bold text-base text-[#1A1A1A]">{settlement.paidAt ? new Date(settlement.paidAt).toLocaleString() : 'N/A'}</p>
                        </div>
                    </div>
                </div>
            </div>

            {settlement.status !== "PAID" && (
                <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-6 hover:shadow-md transition-all duration-300 space-y-4">
                    <p className="text-xs text-[#7A7A7A] leading-relaxed italic">
                        * Note: Despite the selected display currency, the payment transaction on the Stripe gateway will be processed in the system's base currency (PLN). Any potential foreign exchange conversion fees depend on your bank.
                        Exchange rate applied: {settlement.currencyInfo?.exchangeRate} {settlement.currencyInfo?.displayCurrency}/PLN.
                    </p>
                    <div className="flex flex-wrap gap-3 border-t border-[#DACDCA] pt-4">
                        {paymentButtons.map((button) => {
                            const stateKey = `${settlement.id}-${button.type}`;
                            const state = settlementState[stateKey];

                            return (
                                <button
                                    key={button.type}
                                    onClick={() =>
                                        handlePayment(
                                            settlement.reservationId,
                                            button.type,
                                            settlement.id,
                                            button.quantity,
                                            button.unitPrice
                                        )
                                    }
                                    disabled={state?.loading || state?.success}
                                    className={`px-5 py-2.5 font-bold rounded-lg shadow-sm text-sm transition-all border cursor-pointer ${
                                        state?.success
                                            ? "bg-green-600 text-white border-green-600"
                                            : state?.loading
                                                ? "bg-gray-400 text-white border-gray-400 cursor-wait"
                                                : "bg-[#42211D] text-white hover:bg-[#2a1412] border-[#DACDCA]"
                                    }`}
                                >
                                    {state?.loading
                                        ? "Processing..."
                                        : state?.success
                                            ? "Redirecting..."
                                            : `Pay ${button.type}`}
                                </button>
                            );
                        })}
                    </div>
                </div>
            )}
        </div>
    );
}