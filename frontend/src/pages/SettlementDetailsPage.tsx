import {useEffect, useState, useCallback} from "react";
import {useParams, Link} from "react-router-dom";
import {getSettlementDetails, getSettlementItemInfoByType} from "../api/settlementApi";
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

        setLoading(true);
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

    const loadPaymentButtons = useCallback (async (
        settlement: SettlementDetails,
        settlementItemTypes: string[]
    ) => {

        const buttons: PaymentButton[] = [];

        if (settlementItemTypes.includes("DEPOSIT")) {
            try {
                await getSettlementItemInfoByType(settlement.reservationId, "DEPOSIT");
            } catch {
                buttons.push({
                    type: "DEPOSIT",
                    quantity: 1,
                    unitPrice: settlement.depositAmount
                });
            }
        }

        try {
            await getSettlementItemInfoByType(settlement.reservationId, "ACCOMMODATION");
        } catch {
            buttons.push({
                type: "ACCOMMODATION",
                quantity: 1,
                unitPrice: settlement.accommodationAmount
            });
        }

        const utilityTypes = ["WATER", "ELECTRICITY", "CLEANING_FEE"];

        for (const type of utilityTypes) {
            if (!settlementItemTypes.includes(type)) {
                continue;
            }

            try {
                const res = await getSettlementItemInfoByType(settlement.reservationId, type);
                if (res) {
                    buttons.push({
                        type,
                        quantity: res.quantity,
                        unitPrice: res.unitPrice
                    });
                }
            } catch {
                // settlement item jeszcze nie istnieje
            }
        }

        setPaymentButtons(buttons);
    }, []);

    const getUnitSettlementItemsType = async (reservationId: string) => {
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
    }

    useEffect(() => {
        if (!settlement?.reservationId) return;

        getUnitSettlementItemsType(settlement.reservationId)
            .then((types) => {
                if (!types) return;
                loadPaymentButtons(settlement, types);
            });

    }, [settlement, loadPaymentButtons]);

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

            window.location.assign(checkoutUrl);

        } catch (err: unknown) {
            const message = err instanceof Error ? err.message : "An error occurred";
            setSettlementState(prev => ({
                ...prev,
                [stateKey]: { loading: false, error: message }
            }));
        }
    };

    if (loading) return <div className="p-6">Loading settlement details...</div>;
    if (error) return <div className="p-6 text-red-500">{error}</div>;
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
                                <p className="font-medium">{settlement.accommodationAmount} PLN</p>
                            </div>
                            <div>
                                <p className="text-gray-500 text-sm">Utilities Amount</p>
                                <p className="font-medium">{settlement.utilitiesAmount} PLN</p>
                            </div>
                            <div>
                                <p className="text-gray-500 text-sm">Deposit Amount</p>
                                <p className="font-medium">{settlement.depositAmount} PLN</p>
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
                <div className="mt-4 flex flex-wrap gap-2">
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
                                className={`px-4 py-2 font-bold rounded shadow ${
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
            )}
        </div>
    );
}