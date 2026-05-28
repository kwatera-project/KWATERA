import type {CurrencyMetadata} from "./reservation";

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

    finalized: boolean;
}