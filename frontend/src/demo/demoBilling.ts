import type { SettlementDetails, SettlementItemDetails } from "../types/settlement";
import { demoReservations } from "./demoReservations";

type SettlementSeed = {
    reservationId: string;
    status: string;
    waterM3: number | null;
    depositAmount: number;
    discountAmount: number;
};

const settlementSeeds: SettlementSeed[] = [
    { reservationId: "demo-reservation-0001", waterM3: 2.4, depositAmount: 0, discountAmount: 0, status: "PAID" },
    { reservationId: "demo-reservation-0002", waterM3: 5.6, depositAmount: 0, discountAmount: 0, status: "PAID" },
    { reservationId: "demo-reservation-0003", waterM3: 2.4, depositAmount: 0, discountAmount: 0, status: "PAID" },
    { reservationId: "demo-reservation-0004", waterM3: 0.4, depositAmount: 0, discountAmount: 0, status: "PAID" },
    { reservationId: "demo-reservation-0005", waterM3: 3, depositAmount: 0, discountAmount: 0, status: "PAID" },
    { reservationId: "demo-reservation-0006", waterM3: 1.6, depositAmount: 0, discountAmount: 0, status: "PAID" },
    { reservationId: "demo-reservation-0007", waterM3: 1.2, depositAmount: 0, discountAmount: 0, status: "PAID" },
    { reservationId: "demo-reservation-0008", waterM3: 0.6, depositAmount: 0, discountAmount: 0, status: "PAID" },
    { reservationId: "demo-reservation-0009", waterM3: null, depositAmount: 0, discountAmount: 0, status: "CANCELLED" },
    { reservationId: "demo-reservation-0010", waterM3: null, depositAmount: 0, discountAmount: 0, status: "CANCELLED" },
    { reservationId: "demo-reservation-0011", waterM3: null, depositAmount: 0, discountAmount: 0, status: "CANCELLED" },
    { reservationId: "demo-reservation-0012", waterM3: null, depositAmount: 0, discountAmount: 0, status: "CANCELLED" },
    { reservationId: "demo-reservation-0013", waterM3: null, depositAmount: 0, discountAmount: 0, status: "PARTIALLY_PAID" },
    { reservationId: "demo-reservation-0014", waterM3: null, depositAmount: 0, discountAmount: 0, status: "PARTIALLY_PAID" },
    { reservationId: "demo-reservation-0015", waterM3: null, depositAmount: 0, discountAmount: 0, status: "DRAFT" },
    { reservationId: "demo-reservation-0016", waterM3: null, depositAmount: 0, discountAmount: 0, status: "PARTIALLY_PAID" },
    { reservationId: "demo-reservation-0017", waterM3: null, depositAmount: 0, discountAmount: 0, status: "DRAFT" },
    { reservationId: "demo-reservation-0018", waterM3: null, depositAmount: 0, discountAmount: 0, status: "DRAFT" },
    { reservationId: "demo-reservation-0019", waterM3: null, depositAmount: 0, discountAmount: 0, status: "PARTIALLY_PAID" },
    { reservationId: "demo-reservation-0020", waterM3: null, depositAmount: 0, discountAmount: 0, status: "DRAFT" },
    { reservationId: "demo-reservation-0021", waterM3: null, depositAmount: 0, discountAmount: 0, status: "PARTIALLY_PAID" },
    { reservationId: "demo-reservation-0022", waterM3: null, depositAmount: 0, discountAmount: 0, status: "DRAFT" },
    { reservationId: "demo-reservation-0023", waterM3: null, depositAmount: 0, discountAmount: 0, status: "DRAFT" },
    { reservationId: "demo-reservation-0024", waterM3: null, depositAmount: 0, discountAmount: 0, status: "DRAFT" },
    { reservationId: "demo-reservation-0025", waterM3: null, depositAmount: 0, discountAmount: 0, status: "CANCELLED" },
    { reservationId: "demo-reservation-0026", waterM3: null, depositAmount: 0, discountAmount: 0, status: "CANCELLED" },
];

function settlementIdFor(reservationId: string) {
    return reservationId.replace("demo-reservation-", "demo-settlement-");
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
    const utilitiesAmount = Number(((seed.waterM3 ?? 0) * 18.5).toFixed(2));
    const totalAmount = seed.status === "CANCELLED"
        ? 0
        : Number((accommodationAmount + utilitiesAmount + seed.depositAmount - seed.discountAmount).toFixed(2));
    const amountPaid = seed.status === "PAID"
        ? totalAmount
        : seed.status === "PARTIALLY_PAID"
            ? Number((totalAmount * 0.5).toFixed(2))
            : 0;
    const balanceDue = seed.status === "DRAFT" || seed.status === "PARTIALLY_PAID"
        ? Number(Math.max(0, totalAmount - amountPaid).toFixed(2))
        : 0;
    const issuedAt = seed.status === "CANCELLED" ? null : reservation.createdAt;
    const paidAt = seed.status === "PAID" || seed.status === "PARTIALLY_PAID" ? `${reservation.endDate}T11:00:00Z` : null;
    const items: SettlementItemDetails[] = [];

    if (seed.status !== "CANCELLED") {
        const nights = Math.round((Date.parse(reservation.endDate) - Date.parse(reservation.startDate)) / 86400000);
        items.push(item(`${settlementId}-accommodation`, settlementId, "ACCOMMODATION", `Accommodation payment for ${nights} night(s)`, nights, reservation.pricePerNightSnapshot ?? 0, issuedAt ?? reservation.createdAt));
    }

    if (seed.waterM3 !== null && seed.status !== "CANCELLED") {
        items.push(item(`${settlementId}-water`, settlementId, "WATER", "Water consumption settlement (rate PLN 18.50/m3)", seed.waterM3, 18.5, `${reservation.endDate}T11:00:00Z`));
    }

    return {
        id: settlementId,
        reservationId: reservation.id,
        status: seed.status,
        accommodationAmount: seed.status === "CANCELLED" ? 0 : accommodationAmount,
        utilitiesAmount: seed.status === "CANCELLED" ? 0 : utilitiesAmount,
        depositAmount: seed.depositAmount,
        discountAmount: seed.discountAmount,
        totalAmount,
        amountPaid,
        balanceDue,
        convertedTotalAmount: Number((totalAmount / (reservation.currencyInfo?.exchangeRate ?? 1)).toFixed(2)),
        convertedAmountPaid: Number((amountPaid / (reservation.currencyInfo?.exchangeRate ?? 1)).toFixed(2)),
        convertedBalanceDue: Number((balanceDue / (reservation.currencyInfo?.exchangeRate ?? 1)).toFixed(2)),
        convertedAccommodationAmount: Number(((seed.status === "CANCELLED" ? 0 : accommodationAmount) / (reservation.currencyInfo?.exchangeRate ?? 1)).toFixed(2)),
        convertedUtilitiesAmount: Number(((seed.status === "CANCELLED" ? 0 : utilitiesAmount) / (reservation.currencyInfo?.exchangeRate ?? 1)).toFixed(2)),
        convertedDepositAmount: Number((seed.depositAmount / (reservation.currencyInfo?.exchangeRate ?? 1)).toFixed(2)),
        currencyInfo: reservation.currencyInfo,
        issuedAt,
        paidAt,
        createdAt: reservation.createdAt,
        updatedAt: paidAt ?? issuedAt ?? reservation.createdAt,
        finalized: seed.status === "PAID" || seed.status === "PARTIALLY_PAID",
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
