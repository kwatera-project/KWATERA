import type { SettlementDetails, SettlementItemDetails } from "../types/settlement";
import { demoReservations } from "./demoReservations";

type SettlementSeed = {
    reservationId: string;
    status: string;
    depositAmount: number;
    amountPaid: number;
    issuedAt: string | null;
    paidAt: string | null;
    waterQuantity?: number;
    electricityQuantity?: number;
};

const settlementSeeds: SettlementSeed[] = [
    { reservationId: "demo-reservation-1001", status: "PAID", depositAmount: 400, amountPaid: 2182, issuedAt: "2026-05-09T12:00:00Z", paidAt: "2026-05-10T08:30:00Z", waterQuantity: 8.4, electricityQuantity: 42 },
    { reservationId: "demo-reservation-1002", status: "PAID", depositAmount: 800, amountPaid: 5060, issuedAt: "2026-05-31T12:00:00Z", paidAt: "2026-06-02T08:30:00Z", waterQuantity: 16.2, electricityQuantity: 67 },
    { reservationId: "demo-reservation-1003", status: "ISSUED", depositAmount: 500, amountPaid: 1620, issuedAt: "2026-05-21T11:00:00Z", paidAt: null, waterQuantity: 5.1, electricityQuantity: 28 },
    { reservationId: "demo-reservation-1004", status: "CANCELLED", depositAmount: 0, amountPaid: 0, issuedAt: null, paidAt: null },
    { reservationId: "demo-reservation-1005", status: "ISSUED", depositAmount: 250, amountPaid: 500, issuedAt: "2026-06-09T12:00:00Z", paidAt: null, waterQuantity: 4.2, electricityQuantity: 21 },
    { reservationId: "demo-reservation-1006", status: "ISSUED", depositAmount: 500, amountPaid: 1680, issuedAt: "2026-06-22T12:00:00Z", paidAt: null, waterQuantity: 12.8, electricityQuantity: 46 },
    { reservationId: "demo-reservation-1007", status: "DRAFT", depositAmount: 700, amountPaid: 0, issuedAt: "2026-06-03T14:20:00Z", paidAt: null },
    { reservationId: "demo-reservation-1008", status: "ISSUED", depositAmount: 500, amountPaid: 1120, issuedAt: "2026-06-30T12:00:00Z", paidAt: null, waterQuantity: 14.6, electricityQuantity: 58 },
    { reservationId: "demo-reservation-1009", status: "PAID", depositAmount: 300, amountPaid: 1812, issuedAt: "2026-06-05T12:00:00Z", paidAt: "2026-06-06T09:15:00Z", waterQuantity: 3.7, electricityQuantity: 31 },
    { reservationId: "demo-reservation-1010", status: "DRAFT", depositAmount: 900, amountPaid: 0, issuedAt: "2026-06-28T15:00:00Z", paidAt: null },
    { reservationId: "demo-reservation-1011", status: "ISSUED", depositAmount: 500, amountPaid: 1350, issuedAt: "2026-07-20T12:00:00Z", paidAt: null, waterQuantity: 18.1, electricityQuantity: 74 },
    { reservationId: "demo-reservation-1012", status: "PAID", depositAmount: 250, amountPaid: 1354, issuedAt: "2026-07-29T12:00:00Z", paidAt: "2026-07-29T18:45:00Z", waterQuantity: 3.1, electricityQuantity: 19 },
    { reservationId: "demo-reservation-1013", status: "CANCELLED", depositAmount: 0, amountPaid: 0, issuedAt: null, paidAt: null },
    { reservationId: "demo-reservation-1014", status: "ISSUED", depositAmount: 400, amountPaid: 910, issuedAt: "2026-07-08T12:00:00Z", paidAt: null, waterQuantity: 9.7, electricityQuantity: 37 },
];

function settlementIdFor(reservationId: string) {
    return reservationId.replace("demo-reservation-10", "demo-settlement-50");
}

function item(
    id: string,
    settlementId: string,
    type: SettlementItemDetails["type"],
    description: string,
    quantity: number,
    unitPrice: number,
    createdAt: string
): SettlementItemDetails {
    const amount = Number((quantity * unitPrice).toFixed(2));

    return {
        id,
        settlementId,
        type,
        description,
        quantity,
        unitPrice,
        amount,
        createdAt,
    };
}

function createSettlement(seed: SettlementSeed): SettlementDetails {
    const reservation = demoReservations.find((item) => item.id === seed.reservationId);
    if (!reservation) throw new Error(`Missing reservation for settlement ${seed.reservationId}`);

    const settlementId = settlementIdFor(seed.reservationId);
    const accommodationAmount = reservation.totalPrice ?? 0;
    const createdAt = reservation.createdAt;
    const issuedAt = seed.issuedAt;
    const items: SettlementItemDetails[] = [];

    if (accommodationAmount > 0 && seed.status !== "CANCELLED") {
        items.push(item(`${settlementId}-accommodation`, settlementId, "ACCOMMODATION", "Accommodation fee", 1, accommodationAmount, issuedAt ?? createdAt));
    }

    if (seed.depositAmount > 0) {
        items.push(item(`${settlementId}-deposit`, settlementId, "DEPOSIT", "Refundable security deposit", 1, seed.depositAmount, issuedAt ?? createdAt));
    }

    if (seed.waterQuantity) {
        items.push(item(`${settlementId}-water`, settlementId, "WATER", "Water utility usage", seed.waterQuantity, 12, issuedAt ?? createdAt));
    }

    if (seed.electricityQuantity) {
        items.push(item(`${settlementId}-electricity`, settlementId, "ELECTRICITY", "Electricity utility usage", seed.electricityQuantity, 2.1, issuedAt ?? createdAt));
    }

    const utilitiesAmount = Number(
        items
            .filter((entry) => entry.type === "WATER" || entry.type === "ELECTRICITY")
            .reduce((sum, entry) => sum + entry.amount, 0)
            .toFixed(2)
    );
    const totalAmount = Number((accommodationAmount + utilitiesAmount + seed.depositAmount).toFixed(2));
    const amountPaid = seed.status === "PAID" ? totalAmount : Math.min(seed.amountPaid, totalAmount);
    const balanceDue = Number(Math.max(0, totalAmount - amountPaid).toFixed(2));

    return {
        id: settlementId,
        reservationId: reservation.id,
        status: seed.status,
        accommodationAmount,
        utilitiesAmount,
        depositAmount: seed.depositAmount,
        discountAmount: 0,
        totalAmount,
        amountPaid,
        balanceDue,
        convertedTotalAmount: totalAmount,
        convertedAmountPaid: amountPaid,
        convertedBalanceDue: balanceDue,
        convertedAccommodationAmount: accommodationAmount,
        convertedUtilitiesAmount: utilitiesAmount,
        convertedDepositAmount: seed.depositAmount,
        currencyInfo: reservation.currencyInfo,
        issuedAt,
        paidAt: seed.paidAt,
        createdAt,
        updatedAt: seed.paidAt ?? issuedAt ?? createdAt,
        finalized: seed.status === "PAID" || seed.status === "ISSUED",
        items,
    };
}

export const demoSettlementsByReservationId: Record<string, SettlementDetails> = Object.fromEntries(
    settlementSeeds.map((seed) => [seed.reservationId, createSettlement(seed)])
);

export const demoSettlementBySettlementId = Object.values(demoSettlementsByReservationId).reduce<Record<string, SettlementDetails>>(
    (acc, settlement) => {
        acc[settlement.id] = settlement;
        return acc;
    },
    {}
);
