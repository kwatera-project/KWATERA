import type { CurrencyMetadata, GuestReservation, ReservationDetails } from "../types/reservation";
import { demoProperties, demoUnitsByProperty } from "./demoProperties";

const ownerName = "Piotr Wisniewski";
const ownerEmail = "owner.demo@kwatera.local";

const demoGuestProfiles: Record<string, { name: string; email: string }> = {
    "demo-user-guest": { name: "Adam Krawczyk", email: "guest.demo@kwatera.local" },
    "demo-user-guest-4": { name: "Ewa Piotrowska", email: "guest4@example.com" },
    "demo-user-guest-5": { name: "Karol Grabowski", email: "guest5@example.com" },
    "demo-user-guest-6": { name: "Joanna Pawlak", email: "guest6@example.com" },
    "demo-user-guest-7": { name: "Mateusz Michalski", email: "guest7@example.com" },
    "demo-user-guest-8": { name: "Natalia Krol", email: "guest8@example.com" },
    "demo-user-guest-9": { name: "Lukasz Wieczorek", email: "guest9@example.com" },
    "demo-user-guest-10": { name: "Paulina Mazur", email: "guest10@example.com" },
    "demo-user-owner": { name: "Maria Wojcik", email: "owner4@example.com" },
};

function currencyInfo(displayCurrency: string, exchangeRate: number): CurrencyMetadata {
    return {
        baseCurrency: "PLN",
        displayCurrency,
        exchangeRate,
        rateEffectiveDate: "2026-06-17",
    };
}

function unit(unitId: string) {
    for (const [propertyId, units] of Object.entries(demoUnitsByProperty)) {
        const found = units.find((item) => item.id === unitId);
        if (found) {
            return {
                propertyId,
                property: demoProperties.find((property) => property.id === propertyId),
                unit: found,
            };
        }
    }

    throw new Error(`Missing demo unit ${unitId}`);
}

function reservation(
    id: string,
    unitId: string,
    userId: string,
    startDate: string,
    endDate: string,
    status: string,
    createdAt: string,
    currency: string,
    exchangeRate: number,
    guestMessage?: string
): ReservationDetails {
    const unitInfo = unit(unitId);
    const guest = demoGuestProfiles[userId] ?? demoGuestProfiles["demo-user-guest"];
    const nights = Math.round((Date.parse(endDate) - Date.parse(startDate)) / 86400000);
    const totalPrice = unitInfo.unit.pricePerNight * nights;
    const info = currencyInfo(currency, exchangeRate);

    return {
        id,
        unitId,
        guestName: guest.name,
        guestEmail: guest.email,
        unitName: unitInfo.unit.name,
        city: unitInfo.property?.city,
        startDate,
        endDate,
        status,
        userId,
        createdAt,
        pricePerNightSnapshot: unitInfo.unit.pricePerNight,
        totalPrice,
        convertedTotalPrice: Number((totalPrice / exchangeRate).toFixed(2)),
        currencyInfo: info,
        ownerName,
        ownerEmail,
        guestMessage,
    };
}

export const demoReservations: ReservationDetails[] = [
    reservation("demo-reservation-0001", "demo-unit-entire-forest-cabin", "demo-user-guest", "2026-01-18", "2026-01-22", "COMPLETED", "2026-01-13T10:00:00Z", "PLN", 1, "Looking forward to a relaxing forest break!"),
    reservation("demo-reservation-0002", "demo-unit-entire-mountain-cabin", "demo-user-guest-4", "2026-02-17", "2026-02-24", "COMPLETED", "2026-02-12T10:00:00Z", "EUR", 4.35, "Traveling with the whole family, hoping for great views."),
    reservation("demo-reservation-0003", "demo-unit-entire-nature-cabin", "demo-user-guest-5", "2026-03-19", "2026-03-22", "COMPLETED", "2026-03-14T10:00:00Z", "PLN", 1),
    reservation("demo-reservation-0004", "demo-unit-attic-room", "demo-user-guest-6", "2026-04-18", "2026-04-20", "COMPLETED", "2026-04-13T10:00:00Z", "PLN", 1, "Quick weekend getaway."),
    reservation("demo-reservation-0005", "demo-unit-entire-lakeside-cottage", "demo-user-guest-7", "2026-05-03", "2026-05-08", "COMPLETED", "2026-04-28T10:00:00Z", "PLN", 1, "Bringing kayaking gear, hope that is fine!"),
    reservation("demo-reservation-0006", "demo-unit-city-center-apartment", "demo-user-guest-8", "2026-05-18", "2026-05-22", "COMPLETED", "2026-05-13T10:00:00Z", "USD", 3.95),
    reservation("demo-reservation-0007", "demo-unit-historic-center-apartment", "demo-user-guest-9", "2026-05-28", "2026-05-31", "COMPLETED", "2026-05-23T10:00:00Z", "PLN", 1, "Celebrating our anniversary."),
    reservation("demo-reservation-0008", "demo-unit-odra-view-apartment", "demo-user-guest-10", "2026-06-07", "2026-06-09", "COMPLETED", "2026-06-02T10:00:00Z", "PLN", 1),
    reservation("demo-reservation-0009", "demo-unit-beachside-apartment", "demo-user-guest", "2026-03-29", "2026-04-01", "CANCELLED", "2026-03-24T14:30:00Z", "PLN", 1, "Just checking availability."),
    reservation("demo-reservation-0010", "demo-unit-premium-city-apartment", "demo-user-owner", "2026-05-08", "2026-05-10", "CANCELLED", "2026-05-03T14:30:00Z", "PLN", 1),
    reservation("demo-reservation-0011", "demo-unit-executive-apartment", "demo-user-guest-5", "2026-05-23", "2026-05-27", "CANCELLED", "2026-05-16T09:15:00Z", "PLN", 1, "Plans changed unexpectedly, sorry for the trouble."),
    reservation("demo-reservation-0012", "demo-unit-riverside-deluxe-apartment", "demo-user-guest-6", "2026-06-02", "2026-06-04", "CANCELLED", "2026-05-26T09:15:00Z", "EUR", 4.35),
    reservation("demo-reservation-0013", "demo-unit-entire-forest-cabin", "demo-user-guest-7", "2026-06-15", "2026-06-21", "CONFIRMED", "2026-06-10T16:45:00Z", "PLN", 1, "Excited for the fireplace evenings."),
    reservation("demo-reservation-0014", "demo-unit-seaside-luxury-apartment", "demo-user-guest-8", "2026-06-16", "2026-06-20", "CONFIRMED", "2026-06-11T16:45:00Z", "PLN", 1),
    reservation("demo-reservation-0015", "demo-unit-executive-plus-apartment", "demo-user-guest-9", "2026-06-22", "2026-06-25", "CONFIRMED", "2026-06-17T16:45:00Z", "PLN", 1, "Business trip, need a quiet workspace."),
    reservation("demo-reservation-0016", "demo-unit-old-town-comfort-apartment", "demo-user-guest-10", "2026-06-27", "2026-07-02", "CONFIRMED", "2026-06-22T16:45:00Z", "PLN", 1),
    reservation("demo-reservation-0017", "demo-unit-entire-mountain-cabin", "demo-user-guest", "2026-07-07", "2026-07-14", "CONFIRMED", "2026-07-02T16:45:00Z", "PLN", 1, "Group hiking trip with friends."),
    reservation("demo-reservation-0018", "demo-unit-entire-nature-cabin", "demo-user-guest-4", "2026-08-01", "2026-08-05", "CONFIRMED", "2026-07-27T16:45:00Z", "EUR", 4.35),
    reservation("demo-reservation-0019", "demo-unit-entire-lakeside-cottage", "demo-user-owner", "2026-08-16", "2026-08-19", "CONFIRMED", "2026-08-11T16:45:00Z", "PLN", 1, "Visiting friends nearby."),
    reservation("demo-reservation-0020", "demo-unit-city-center-apartment", "demo-user-guest-6", "2026-09-15", "2026-09-17", "CONFIRMED", "2026-09-10T16:45:00Z", "PLN", 1),
    reservation("demo-reservation-0021", "demo-unit-historic-center-apartment", "demo-user-guest-7", "2026-10-15", "2026-10-21", "CONFIRMED", "2026-10-10T16:45:00Z", "PLN", 1, "Family reunion trip."),
    reservation("demo-reservation-0022", "demo-unit-premium-city-apartment", "demo-user-guest-8", "2026-11-14", "2026-11-17", "CONFIRMED", "2026-11-09T16:45:00Z", "USD", 3.95),
    reservation("demo-reservation-0023", "demo-unit-odra-view-apartment", "demo-user-guest-9", "2026-07-17", "2026-07-19", "PENDING", "2026-06-17T11:55:00Z", "PLN", 1, "Just submitted, awaiting confirmation."),
    reservation("demo-reservation-0024", "demo-unit-beachside-apartment", "demo-user-guest-10", "2026-08-31", "2026-09-04", "PENDING", "2026-06-17T11:58:00Z", "PLN", 1),
    reservation("demo-reservation-0025", "demo-unit-executive-apartment", "demo-user-guest", "2026-09-25", "2026-09-28", "CANCELLED", "2026-05-28T14:30:00Z", "PLN", 1),
    reservation("demo-reservation-0026", "demo-unit-riverside-deluxe-apartment", "demo-user-guest-5", "2026-12-04", "2026-12-06", "CANCELLED", "2026-05-28T14:30:00Z", "PLN", 1, "Had to cancel due to a scheduling conflict."),
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
