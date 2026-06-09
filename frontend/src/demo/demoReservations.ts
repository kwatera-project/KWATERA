import type { GuestReservation, ReservationDetails } from "../types/reservation";
import { demoUnitsByProperty } from "./demoProperties";

const [sopotStudio, sopotFamily] = demoUnitsByProperty["demo-property-sopot"];
const [zakopaneHouse] = demoUnitsByProperty["demo-property-zakopane"];
const [mazuryRoom] = demoUnitsByProperty["demo-property-mazury"];

export const demoReservations: ReservationDetails[] = [
    {
        id: "demo-reservation-1001",
        unitId: sopotStudio.id,
        guestName: "Anna Nowak",
        guestEmail: "guest.demo@kwatera.local",
        unitName: sopotStudio.name,
        city: "Sopot",
        startDate: "2026-06-18",
        endDate: "2026-06-22",
        status: "CONFIRMED",
        userId: "demo-user-guest",
        createdAt: "2026-06-02T10:15:00Z",
        pricePerNightSnapshot: 420,
        totalPrice: 1680,
        convertedTotalPrice: 1680,
        currencyInfo: { baseCurrency: "PLN", displayCurrency: "PLN", exchangeRate: 1, rateEffectiveDate: "2026-06-09" },
        ownerName: "Marek Zieliński",
        ownerEmail: "owner.demo@kwatera.local",
    },
    {
        id: "demo-reservation-1002",
        unitId: sopotFamily.id,
        guestName: "Piotr Kowalski",
        guestEmail: "piotr@example.local",
        unitName: sopotFamily.name,
        city: "Sopot",
        startDate: "2026-06-24",
        endDate: "2026-06-28",
        status: "PENDING",
        userId: "demo-user-guest-2",
        createdAt: "2026-06-03T14:20:00Z",
        pricePerNightSnapshot: 690,
        totalPrice: 2760,
        convertedTotalPrice: 2760,
        currencyInfo: { baseCurrency: "PLN", displayCurrency: "PLN", exchangeRate: 1, rateEffectiveDate: "2026-06-09" },
        ownerName: "Marek Zieliński",
        ownerEmail: "owner.demo@kwatera.local",
    },
    {
        id: "demo-reservation-1003",
        unitId: zakopaneHouse.id,
        guestName: "Karolina Wójcik",
        guestEmail: "karolina@example.local",
        unitName: zakopaneHouse.name,
        city: "Zakopane",
        startDate: "2026-06-15",
        endDate: "2026-06-20",
        status: "COMPLETED",
        userId: "demo-user-guest-3",
        createdAt: "2026-05-26T09:00:00Z",
        pricePerNightSnapshot: 810,
        totalPrice: 4050,
        convertedTotalPrice: 4050,
        currencyInfo: { baseCurrency: "PLN", displayCurrency: "PLN", exchangeRate: 1, rateEffectiveDate: "2026-06-09" },
        ownerName: "Marek Zieliński",
        ownerEmail: "owner.demo@kwatera.local",
    },
];

export const demoAdminReservations = demoReservations.map((reservation) => ({
    id: reservation.id,
    guestName: reservation.guestName,
    unitName: reservation.unitName,
    startDate: reservation.startDate,
    endDate: reservation.endDate,
    status: reservation.status,
    userId: reservation.userId,
    pricePerNightSnapshot: reservation.pricePerNightSnapshot,
    totalPrice: reservation.totalPrice,
}));

export const demoOccupancy = [
    ...demoAdminReservations.map((reservation) => ({
        reservationId: reservation.id,
        unitId: demoReservations.find((item) => item.id === reservation.id)?.unitId ?? "",
        unitName: reservation.unitName,
        startDate: reservation.startDate,
        endDate: reservation.endDate,
        status: reservation.status,
        guestName: reservation.guestName,
        totalPrice: reservation.totalPrice,
    })),
    {
        reservationId: "demo-reservation-1004",
        unitId: mazuryRoom.id,
        unitName: mazuryRoom.name,
        startDate: "2026-06-10",
        endDate: "2026-06-13",
        status: "CONFIRMED",
        guestName: "Tomasz Maj",
        totalPrice: 1080,
    },
];

export const demoGuestReservations: GuestReservation[] = demoReservations
    .filter((reservation) => reservation.userId === "demo-user-guest")
    .map((reservation) => ({
        id: reservation.id,
        unitId: reservation.unitId,
        startDate: reservation.startDate,
        endDate: reservation.endDate,
        status: reservation.status,
        totalPrice: reservation.totalPrice,
        convertedTotalPrice: reservation.convertedTotalPrice,
        currencyInfo: reservation.currencyInfo,
    }));
