import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { getSettlementDetails } from "../api/settlementApi";
import type { SettlementDetails } from "../types/settlement";
import {getSettlementItemInfoByType} from "../api/settlementApi.ts";
import {GATEWAY_BASE_URL} from "../api/apiConfig.ts";

export default function SettlementDetailsPage() {
    const { id } = useParams();
    const [settlement, setSettlement] = useState<SettlementDetails | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const [settlementState, setSettlementState] = useState<
        Record<string, { loading: boolean; success?: boolean, error?: string}>
    >({});

    useEffect(() => {
        if (!id) return;

        getSettlementDetails(id)
            .then(setSettlement)
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false));
    }, [id]);

    const handlePayment = async (reservationId: string, settlementType: string, settlementId: string) => {
        setSettlementState(prev => ({
            ...prev,
            [settlementId]: { loading: true }
        }));

        try {
            const res = await getSettlementItemInfoByType(reservationId, settlementType);

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
                        type: `${name}`,
                        description: `${name} fee`,
                        quantity: res.quantity,
                        unitPrice: res.unitPrice
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
            setSettlementState(prev => ({ ...prev, [settlementId]: { loading: false, error: message } }));
        }
    };


    if (loading) return <div className="p-6">Loading settlement details...</div>;
    if (error) return <div className="p-6 text-red-500">{error}</div>;
    if (!settlement) return <div className="p-6">Settlement not found.</div>;

    return (
        <div className="max-w-3xl mx-auto p-6">
            <div className="bg-card rounded-xl p-6 shadow border">
                <h1 className="text-2xl font-bold mb-6 border-b pb-2">Settlement Details</h1>

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
                        <p className="font-medium">{settlement.balanceDue}</p>
                    </div>
                    <div>
                        <p className="text-gray-500 text-sm">Accommodation Amount</p>
                        <p className="font-medium">{settlement.accommodationAmount}</p>
                    </div>
                    <div>
                        <p className="text-gray-500 text-sm">Utilities Amount</p>
                        <p className="font-medium">{settlement.utilitiesAmount}</p>
                    </div>
                    <div>
                        <p className="text-gray-500 text-sm">Deposit Amount</p>
                        <p className="font-medium">{settlement.depositAmount}</p>
                    </div>
                    <div>
                        <p className="text-gray-500 text-sm">Total Amount</p>
                        <p className="font-medium">{settlement.totalAmount}</p>
                    </div>
                    <div>
                        <p className="text-gray-500 text-sm">Amount Paid</p>
                        <p className="font-medium">{settlement.amountPaid}</p>
                    </div>
                    <div>
                        <p className="text-gray-500 text-sm">Issued At</p>
                        <p className="font-medium">{settlement.issuedAt}</p>
                    </div>
                    <div>
                        <p className="text-gray-500 text-sm">Paid At</p>
                        <p className="font-medium">{settlement.paidAt}</p>
                    </div>
                </div>

                <div className="mt-4">
                    <button
                        onClick={() => handlePayment(settlement.reservationId, "DEPOSIT", settlement.id)}
                        disabled={settlementState[settlement.id]?.loading || settlementState[settlement.id]?.success}
                        className={`px-4 py-2 font-bold rounded ${
                            settlementState[settlement.id]?.success
                                ? "bg-green-500 text-white cursor-default"
                                : settlementState[settlement.id]?.loading
                                    ? "bg-gray-400 text-white cursor-wait"
                                    : "bg-blue-600 text-white hover:bg-blue-700"
                        }`}
                    >
                        {settlementState[settlement.id]?.loading ? "Processing..." :
                            settlementState[settlement.id]?.success ? "Redirecting to payment..." :
                                "Pay for Deposit"}
                    </button>
                </div>

                <div className="mt-8 pt-4 border-t">
                    <Link to="/" className="text-blue-500 hover:text-blue-700 hover:underline">
                        &larr; Return to properties
                    </Link>
                </div>
            </div>
        </div>
    );
}