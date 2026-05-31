import {useEffect, useState, useCallback} from "react";
import {useParams, Link} from "react-router-dom";
import {getSettlementDetails} from "../api/settlementApi";
import type {SettlementDetails} from "../types/settlement";
import {GATEWAY_BASE_URL} from "../api/apiConfig.ts";
import {getReservationDetails} from "../api/reservationApi.ts";

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

    if (loading) return <div className="p-6">Loading settlement details...</div>;
    if (error) {
        const isForbidden = error.toLowerCase().includes("forbidden") || error.toLowerCase().includes("access denied") || error.toLowerCase().includes("403");

        if (isForbidden) {
            return (
                <div className="max-w-md mx-auto mt-12 p-8 bg-card rounded-2xl shadow-xl border border-red-100 text-center animate-fade-in">
                    <div className="w-16 h-16 bg-red-50 rounded-full flex items-center justify-center mx-auto mb-6">
                        <svg className="w-8 h-8 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 15v2m0-6V9m0 12a9 9 0 110-18 9 9 0 010 18z" />
                        </svg>
                    </div>
                    <h2 className="text-xl font-bold text-gray-900 mb-2">Access Denied</h2>
                    <p className="text-gray-600 mb-6">
                        You do not have permission to view this settlement billing page. If you believe this is an error, please contact support.
                    </p>
                    <Link
                        to="/"
                        className="inline-flex items-center justify-center px-6 py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-lg shadow transition duration-150 ease-in-out"
                    >
                        Return to Home
                    </Link>
                </div>
            );
        }

        return <div className="p-6 text-red-500">{error}</div>;
    }
    if (!settlement) return <div className="p-6">Settlement not found.</div>;

    const displayCurrency = settlement.currencyInfo?.displayCurrency || 'PLN';

    const renderAmount = (convertedAmount?: number, originalAmount?: number) => {
        if (displayCurrency !== 'PLN' && convertedAmount !== undefined) {
            return (
                <span>
                    {convertedAmount} {displayCurrency}
                </span>
            );
        }
        return <span>{originalAmount} PLN</span>;
    };

    return (
        <div className="max-w-3xl mx-auto p-6">
            <div className="bg-card rounded-xl p-6 shadow border">
                <div className="flex justify-between items-center mb-6 border-b pb-2">
                    <h1 className="text-2xl font-bold">Settlement Details</h1>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                        <p className="text-gray-500 text-sm">Settlement ID</p>
                        <p className="font-mono text-sm break-all">{settlement.id}</p>
                    </div>
                    <div>
                        <p className="text-gray-500 text-sm">Reservation ID</p>
                        <p className="font-mono text-sm break-all">{settlement.reservationId}</p>
                    </div>
                    <div>
                        <p className="text-gray-500 text-sm">Status</p>
                        <p className="font-bold text-blue-600">{settlement.status}</p>
                    </div>
                    <div>
                        <p className="text-gray-500 text-sm">Balance Due</p>
                        <p className="font-bold text-red-600 text-lg">
                            {renderAmount(settlement.convertedBalanceDue, settlement.balanceDue)}
                        </p>
                    </div>
                    <div className="md:col-span-2 border-t mt-2 pt-4">
                        <p className="font-semibold text-gray-700 mb-2">Price Breakdown</p>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div>
                                <p className="text-gray-500 text-sm">Accommodation Amount</p>
                                <p className="font-medium">
                                    {renderAmount(settlement.convertedAccommodationAmount, settlement.accommodationAmount)}
                                </p>
                            </div>
                            <div>
                                <p className="text-gray-500 text-sm">Utilities Amount</p>
                                <p className="font-medium">
                                    {renderAmount(settlement.convertedUtilitiesAmount, settlement.utilitiesAmount)}
                                </p>
                                {settlement.items && settlement.items.length > 0 && (
                                    <div className="mt-2 pl-4 border-l-2 border-gray-200">
                                        <ul className="space-y-1">
                                            {settlement.items
                                                .filter((item: any) => ["ELECTRICITY", "WATER", "CLEANING_FEE"].includes(item.type))
                                                .map((item: any) => {
                                                    const rate = settlement.currencyInfo?.exchangeRate || 1;
                                                    const convertedAmount = displayCurrency !== 'PLN'
                                                        ? Number((item.amount / rate).toFixed(2))
                                                        : item.amount;

                                                    return (
                                                        <li key={item.id} className="text-xs text-gray-600 flex justify-between gap-4">
                                                            <span>
                                                                {item.description || item.type} ({item.quantity} x {displayCurrency !== 'PLN' ? Number((item.unitPrice / rate).toFixed(2)) : item.unitPrice} {displayCurrency})
                                                            </span>
                                                            <span className="font-semibold text-gray-700">
                                                                {convertedAmount} {displayCurrency}
                                                            </span>
                                                        </li>
                                                    );
                                                })}
                                        </ul>
                                    </div>
                                )}
                            </div>
                            <div>
                                <p className="text-gray-500 text-sm">Deposit Amount</p>
                                <p className="font-medium">
                                    {renderAmount(settlement.convertedDepositAmount, settlement.depositAmount)}
                                </p>
                            </div>
                            <div>
                                <p className="text-gray-500 text-sm">Total Amount</p>
                                <p className="font-medium text-lg">
                                    {renderAmount(settlement.convertedTotalAmount, settlement.totalAmount)}
                                </p>
                            </div>
                            <div>
                                <p className="text-gray-500 text-sm">Amount Paid</p>
                                <p className="font-medium text-green-600">
                                    {renderAmount(settlement.convertedAmountPaid, settlement.amountPaid)}
                                </p>
                            </div>
                        </div>
                    </div>
                    <div className="md:col-span-2 border-t mt-2 pt-4 grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div>
                            <p className="text-gray-500 text-sm">Issued At</p>
                            <p className="font-medium">{settlement.issuedAt ? new Date(settlement.issuedAt).toLocaleString() : 'N/A'}</p>
                        </div>
                        <div>
                            <p className="text-gray-500 text-sm">Paid At</p>
                            <p className="font-medium">{settlement.paidAt ? new Date(settlement.paidAt).toLocaleString() : 'N/A'}</p>
                        </div>
                    </div>
                </div>

                <div className="mt-8 pt-4 border-t">
                    <Link to="/" className="text-blue-500 hover:text-blue-700 hover:underline">
                        &larr; Return to properties
                    </Link>
                </div>
            </div>
            {settlement.status !== "PAID" && (
                <div className="mt-4 bg-gray-50 rounded-xl p-4 border border-gray-200">
                    <p className="text-xs text-gray-500 mb-3">
                        * Note: Despite the selected display currency, the payment transaction on the Stripe gateway will be processed in the system's base currency (PLN). Any potential foreign exchange conversion fees depend on your bank.
                        Exchange rate applied: {settlement.currencyInfo?.exchangeRate} {settlement.currencyInfo?.displayCurrency}/PLN.
                    </p>
                    <div className="flex flex-wrap gap-2">
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
                                    className={`px-4 py-2 font-bold rounded shadow text-sm ${
                                        state?.success
                                            ? "bg-green-500 text-white"
                                            : state?.loading
                                                ? "bg-gray-400 text-white cursor-not-allowed"
                                                : "bg-blue-600 text-white hover:bg-blue-700 transition"
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