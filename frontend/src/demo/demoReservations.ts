import type { GuestReservation, ReservationDetails } from "../types/reservation";
import { demoProperties, demoUnitsByProperty } from "./demoProperties";

const currencyInfo = { baseCurrency: "PLN", displayCurrency: "PLN", exchangeRate: 1, rateEffectiveDate: "2026-06-09" };
const ownerName = "Marcus Green";
const ownerEmail = "owner.demo@kwatera.local";

function propertyCity(propertyId: string) {
    return demoProperties.find((property) => property.id === propertyId)?.city;
}

function unit(propertyId: string, unitId: string) {
    const found = demoUnitsByProperty[propertyId]?.find((item) => item.id === unitId);
    if (!found) throw new Error(`Missing demo unit ${unitId}`);
    return found;
}

function reservation(
    id: string,
    propertyId: string,
    unitId: string,
    guestName: string,
    guestEmail: string,
    startDate: string,
    endDate: string,
    status: string,
    userId: string,
    createdAt: string,
    pricePerNightSnapshot: number,
    totalPrice: number
): ReservationDetails {
    const unitInfo = unit(propertyId, unitId);

    return {
        id,
        unitId,
        guestName,
        guestEmail,
        unitName: unitInfo.name,
        city: propertyCity(propertyId),
        startDate,
        endDate,
        status,
        userId,
        createdAt,
        pricePerNightSnapshot,
        totalPrice,
        convertedTotalPrice: totalPrice,
        currencyInfo,
        ownerName,
        ownerEmail,
    };
}

export const demoReservations: ReservationDetails[] = [
    reservation(
        "demo-reservation-1001",
        "demo-property-sopot",
        "demo-unit-sopot-a",
        "Alice Morgan",
        "guest.demo@kwatera.local",
        "2026-05-05",
        "2026-05-09",
        "COMPLETED",
        "demo-user-guest",
        "2026-04-26T10:15:00Z",
        410,
        1640
    ),
    reservation(
        "demo-reservation-1002",
        "demo-property-zakopane",
        "demo-unit-zakopane-a",
        "Mia Carter",
        "mia.carter@example.local",
        "2026-05-26",
        "2026-05-31",
        "COMPLETED",
        "demo-user-guest-2",
        "2026-05-12T09:00:00Z",
        810,
        4050
    ),
    reservation(
        "demo-reservation-1003",
        "demo-property-krakow",
        "demo-unit-krakow-a",
        "Noah Adams",
        "noah.adams@example.local",
        "2026-05-18",
        "2026-05-21",
        "CONFIRMED",
        "demo-user-guest-3",
        "2026-05-10T08:40:00Z",
        540,
        1620
    ),
    reservation(
        "demo-reservation-1004",
        "demo-property-mazury",
        "demo-unit-mazury-a",
        "Liam Brooks",
        "liam.brooks@example.local",
        "2026-05-14",
        "2026-05-17",
        "CANCELLED",
        "demo-user-guest-4",
        "2026-05-01T16:30:00Z",
        360,
        1080
    ),
    reservation(
        "demo-reservation-1005",
        "demo-property-zakopane",
        "demo-unit-zakopane-b",
        "Alice Morgan",
        "guest.demo@kwatera.local",
        "2026-06-06",
        "2026-06-09",
        "CONFIRMED",
        "demo-user-guest",
        "2026-05-23T13:20:00Z",
        330,
        990
    ),
    reservation(
        "demo-reservation-1006",
        "demo-property-sopot",
        "demo-unit-sopot-a",
        "Alice Morgan",
        "guest.demo@kwatera.local",
        "2026-06-18",
        "2026-06-22",
        "CONFIRMED",
        "demo-user-guest",
        "2026-06-02T10:15:00Z",
        420,
        1680
    ),
    reservation(
        "demo-reservation-1007",
        "demo-property-sopot",
        "demo-unit-sopot-b",
        "Ethan Reed",
        "ethan.reed@example.local",
        "2026-06-24",
        "2026-06-28",
        "PENDING",
        "demo-user-guest-5",
        "2026-06-03T14:20:00Z",
        690,
        2760
    ),
    reservation(
        "demo-reservation-1008",
        "demo-property-mazury",
        "demo-unit-mazury-b",
        "Grace Hill",
        "grace.hill@example.local",
        "2026-06-26",
        "2026-06-30",
        "CONFIRMED",
        "demo-user-guest-6",
        "2026-06-09T12:45:00Z",
        560,
        2240
    ),
    reservation(
        "demo-reservation-1009",
        "demo-property-krakow",
        "demo-unit-krakow-b",
        "Sophia Turner",
        "sophia.turner@example.local",
        "2026-06-02",
        "2026-06-05",
        "COMPLETED",
        "demo-user-guest-7",
        "2026-05-24T11:10:00Z",
        480,
        1440
    ),
    reservation(
        "demo-reservation-1010",
        "demo-property-sopot",
        "demo-unit-sopot-c",
        "Lucas Bell",
        "lucas.bell@example.local",
        "2026-07-10",
        "2026-07-13",
        "PENDING",
        "demo-user-guest-8",
        "2026-06-28T15:00:00Z",
        880,
        2640
    ),
    reservation(
        "demo-reservation-1011",
        "demo-property-mazury",
        "demo-unit-mazury-b",
        "Alice Morgan",
        "guest.demo@kwatera.local",
        "2026-07-15",
        "2026-07-20",
        "CONFIRMED",
        "demo-user-guest",
        "2026-06-29T18:25:00Z",
        540,
        2700
    ),
    reservation(
        "demo-reservation-1012",
        "demo-property-zakopane",
        "demo-unit-zakopane-b",
        "Nora Scott",
        "nora.scott@example.local",
        "2026-07-26",
        "2026-07-29",
        "CONFIRMED",
        "demo-user-guest-9",
        "2026-07-01T09:50:00Z",
        350,
        1050
    ),
    reservation(
        "demo-reservation-1013",
        "demo-property-krakow",
        "demo-unit-krakow-a",
        "Oliver Price",
        "oliver.price@example.local",
        "2026-07-22",
        "2026-07-25",
        "CANCELLED",
        "demo-user-guest-10",
        "2026-07-03T10:05:00Z",
        510,
        1530
    ),
    reservation(
        "demo-reservation-1014",
        "demo-property-sopot",
        "demo-unit-sopot-a",
        "Harper Lee",
        "harper.lee@example.local",
        "2026-07-04",
        "2026-07-08",
        "CONFIRMED",
        "demo-user-guest-11",
        "2026-06-22T16:15:00Z",
        455,
        1820
    ),
];

export const demoAdminReservations = demoReservations.map((item) => ({
    id: item.id,
    guestName: item.guestName,
    unitName: item.unitName,
    startDate: item.startDate,
    endDate: item.endDate,
    status: item.status,
    userId: item.userId,
    pricePerNightSnapshot: item.pricePerNightSnapshot,
    totalPrice: item.totalPrice,
}));

export const demoOccupancy = demoReservations
    .filter((item) => item.status !== "CANCELLED")
    .map((item) => ({
        reservationId: item.id,
        unitId: item.unitId,
        unitName: item.unitName,
        startDate: item.startDate,
        endDate: item.endDate,
        status: item.status,
        guestName: item.guestName,
        totalPrice: item.totalPrice,
    }));

export const demoGuestReservations: GuestReservation[] = demoReservations
    .filter((item) => item.userId === "demo-user-guest")
    .map((item) => ({
        id: item.id,
        unitId: item.unitId,
        startDate: item.startDate,
        endDate: item.endDate,
        status: item.status,
        totalPrice: item.totalPrice,
        convertedTotalPrice: item.convertedTotalPrice,
        currencyInfo: item.currencyInfo,
    }));
