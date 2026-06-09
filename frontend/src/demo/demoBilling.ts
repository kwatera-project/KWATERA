import type { SettlementDetails } from "../types/settlement";

const baseDate = "2026-06-09T12:00:00Z";

export const demoSettlementsByReservationId: Record<string, SettlementDetails> = {
    "demo-reservation-1001": {
        id: "demo-settlement-5001",
        reservationId: "demo-reservation-1001",
        status: "ISSUED",
        accommodationAmount: 1680,
        utilitiesAmount: 156,
        depositAmount: 500,
        discountAmount: 0,
        totalAmount: 2336,
        amountPaid: 1680,
        balanceDue: 656,
        convertedTotalAmount: 2336,
        convertedAmountPaid: 1680,
        convertedBalanceDue: 656,
        convertedAccommodationAmount: 1680,
        convertedUtilitiesAmount: 156,
        convertedDepositAmount: 500,
        currencyInfo: { baseCurrency: "PLN", displayCurrency: "PLN", exchangeRate: 1, rateEffectiveDate: "2026-06-09" },
        issuedAt: baseDate,
        paidAt: null,
        createdAt: "2026-06-02T10:15:00Z",
        updatedAt: baseDate,
        finalized: true,
        items: [
            { id: "demo-item-1", settlementId: "demo-settlement-5001", type: "ACCOMMODATION", description: "Accommodation fee", quantity: 4, unitPrice: 420, amount: 1680, createdAt: baseDate },
            { id: "demo-item-2", settlementId: "demo-settlement-5001", type: "WATER", description: "Water media settlement", quantity: 13, unitPrice: 12, amount: 156, createdAt: baseDate },
        ],
    },
    "demo-reservation-1002": {
        id: "demo-settlement-5002",
        reservationId: "demo-reservation-1002",
        status: "ISSUED",
        accommodationAmount: 2760,
        utilitiesAmount: 0,
        depositAmount: 700,
        discountAmount: 0,
        totalAmount: 3460,
        amountPaid: 0,
        balanceDue: 3460,
        issuedAt: baseDate,
        paidAt: null,
        createdAt: baseDate,
        updatedAt: baseDate,
        finalized: false,
        items: [],
    },
    "demo-reservation-1003": {
        id: "demo-settlement-5003",
        reservationId: "demo-reservation-1003",
        status: "PAID",
        accommodationAmount: 4050,
        utilitiesAmount: 210,
        depositAmount: 800,
        discountAmount: 0,
        totalAmount: 5060,
        amountPaid: 5060,
        balanceDue: 0,
        issuedAt: "2026-06-01T12:00:00Z",
        paidAt: "2026-06-02T08:30:00Z",
        createdAt: "2026-05-26T09:00:00Z",
        updatedAt: "2026-06-02T08:30:00Z",
        finalized: true,
        items: [
            { id: "demo-item-3", settlementId: "demo-settlement-5003", type: "ACCOMMODATION", description: "Accommodation fee", quantity: 5, unitPrice: 810, amount: 4050, createdAt: baseDate },
            { id: "demo-item-4", settlementId: "demo-settlement-5003", type: "WATER", description: "Water media settlement", quantity: 21, unitPrice: 10, amount: 210, createdAt: baseDate },
        ],
    },
};

export const demoSettlementBySettlementId = Object.values(demoSettlementsByReservationId).reduce<Record<string, SettlementDetails>>(
    (acc, settlement) => {
        acc[settlement.id] = settlement;
        return acc;
    },
    {}
);
