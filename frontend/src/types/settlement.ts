export interface SettlementDetails {
    id: string;
    reservationId: string;
    status: string;

    accommodationAmount: number;
    utilitiesAmount: number;
    depositAmount: number;
    discountAmount: number;
    totalAmount: number;
    amountPaid: number;
    balanceDue: number;

    issuedAt: string | null;
    paidAt: string | null;
    createdAt: string;
    updatedAt: string;

    finalized: boolean;
}