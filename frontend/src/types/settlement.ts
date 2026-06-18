import type {CurrencyMetadata} from "./reservation";

export interface SettlementItemDetails {
    id: string;
    settlementId: string;
    type: "ACCOMMODATION" | "ELECTRICITY" | "WATER" | "CLEANING_FEE" | "DEPOSIT";
    description?: string;
    quantity: number;
    unitPrice: number;
    amount: number;
    createdAt: string;
}

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

    convertedTotalAmount?: number;
    convertedAmountPaid?: number;
    convertedBalanceDue?: number;
    convertedAccommodationAmount?: number;
    convertedUtilitiesAmount?: number;
    convertedDepositAmount?: number;
    currencyInfo?: CurrencyMetadata;

    issuedAt: string | null;
    paidAt: string | null;
    createdAt: string;
    updatedAt: string;
    invoiceRequested?: boolean;
    invoicePdfPath?: string;
    finalized: boolean;
    items?: SettlementItemDetails[];
}