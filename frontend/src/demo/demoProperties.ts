import type { Property, Unit } from "../types/property";

const img = (id: number) => `https://images.pexels.com/photos/${id}/pexels-photo-${id}.jpeg?auto=compress&cs=tinysrgb&w=1200`;

export const demoProperties: Property[] = [
    {
        id: "demo-property-sopot",
        title: "Baltic Harbor Apartments",
        description: "Serviced seaside apartments with automated bookings, deposits, and utility settlement tracking.",
        city: "Sopot",
        country: "Poland",
        postalCode: "81-718",
        street: "Grunwaldzka",
        streetNumber: "12",
        imageUrl: img(1571460),
    },
    {
        id: "demo-property-zakopane",
        title: "Tatra View Lodge",
        description: "Year-round mountain lodge for families, with owner dashboards and seasonal occupancy reporting.",
        city: "Zakopane",
        country: "Poland",
        postalCode: "34-500",
        street: "Koscieliska",
        streetNumber: "44",
        imageUrl: img(754268),
    },
    {
        id: "demo-property-mazury",
        title: "Masurian Lakeside Retreat",
        description: "Small lakeside rental property with multiple rooms, meter readings, and summer settlement examples.",
        city: "Mikolajki",
        country: "Poland",
        postalCode: "11-730",
        street: "Jeziorna",
        streetNumber: "8",
        imageUrl: img(261102),
    },
    {
        id: "demo-property-krakow",
        title: "Old Town City Suites",
        description: "Urban short-stay suites for business and weekend guests, including pending and cancelled bookings.",
        city: "Krakow",
        country: "Poland",
        postalCode: "31-042",
        street: "Grodzka",
        streetNumber: "27",
        imageUrl: img(271624),
    },
];

export const demoUnitsByProperty: Record<string, Unit[]> = {
    "demo-property-sopot": [
        {
            id: "demo-unit-sopot-a",
            propertyId: "demo-property-sopot",
            name: "Harbor Studio",
            description: "Compact studio for two guests with balcony access and quick self check-in.",
            pricePerNight: 420,
            capacity: 2,
            imageUrl: img(271624),
            unitType: "STUDIO",
            unitNumber: "A1",
            floor: 2,
        },
        {
            id: "demo-unit-sopot-b",
            propertyId: "demo-property-sopot",
            name: "Family Sea Apartment",
            description: "Two bedrooms, living area, and kitchenette for family beach stays.",
            pricePerNight: 690,
            capacity: 5,
            imageUrl: img(1643383),
            unitType: "APARTMENT",
            unitNumber: "B4",
            floor: 4,
        },
        {
            id: "demo-unit-sopot-c",
            propertyId: "demo-property-sopot",
            name: "Executive Pier Suite",
            description: "Premium sea-facing suite used in the demo to show higher-value bookings.",
            pricePerNight: 860,
            capacity: 3,
            imageUrl: img(1457842),
            unitType: "SUITE",
            unitNumber: "C7",
            floor: 6,
        },
    ],
    "demo-property-zakopane": [
        {
            id: "demo-unit-zakopane-a",
            propertyId: "demo-property-zakopane",
            name: "Giewont Chalet",
            description: "Wooden chalet with fireplace, terrace, and parking for longer family stays.",
            pricePerNight: 780,
            capacity: 6,
            imageUrl: img(106399),
            unitType: "HOUSE",
            unitNumber: "1",
            floor: 0,
        },
        {
            id: "demo-unit-zakopane-b",
            propertyId: "demo-property-zakopane",
            name: "Trail Room",
            description: "Private room for two guests, often used for short weekend reservations.",
            pricePerNight: 330,
            capacity: 2,
            imageUrl: img(259588),
            unitType: "ROOM",
            unitNumber: "2",
            floor: 1,
        },
    ],
    "demo-property-mazury": [
        {
            id: "demo-unit-mazury-a",
            propertyId: "demo-property-mazury",
            name: "Marina Room",
            description: "Twin room with lake view and simple water meter settlement.",
            pricePerNight: 360,
            capacity: 2,
            imageUrl: img(271619),
            unitType: "ROOM",
            unitNumber: "3",
            floor: 1,
        },
        {
            id: "demo-unit-mazury-b",
            propertyId: "demo-property-mazury",
            name: "Lake Apartment",
            description: "Four-person apartment with terrace and utility charges in the settlement.",
            pricePerNight: 540,
            capacity: 4,
            imageUrl: img(1457842),
            unitType: "APARTMENT",
            unitNumber: "7",
            floor: 2,
        },
    ],
    "demo-property-krakow": [
        {
            id: "demo-unit-krakow-a",
            propertyId: "demo-property-krakow",
            name: "Market Square Suite",
            description: "Central suite with higher weekday demand and dynamic pricing suggestions.",
            pricePerNight: 510,
            capacity: 3,
            imageUrl: img(271624),
            unitType: "SUITE",
            unitNumber: "11",
            floor: 3,
        },
        {
            id: "demo-unit-krakow-b",
            propertyId: "demo-property-krakow",
            name: "Business Loft",
            description: "Loft-style unit for business travelers and city breaks.",
            pricePerNight: 470,
            capacity: 2,
            imageUrl: img(1571460),
            unitType: "LOFT",
            unitNumber: "12",
            floor: 3,
        },
    ],
};

const propertyImageIds: Record<string, number[]> = {
    "demo-property-sopot": [1571460, 271624, 1643383],
    "demo-property-zakopane": [754268, 106399, 259588],
    "demo-property-mazury": [261102, 271619, 1457842],
    "demo-property-krakow": [271624, 1571460, 1643383],
};

export const demoPropertyImages: Record<string, string[]> = Object.fromEntries(
    demoProperties.map((property) => [
        property.id,
        (propertyImageIds[property.id] ?? []).map(img),
    ])
);

export const demoOccupiedDatesByUnit: Record<string, { startDate: string; endDate: string }[]> = {
    "demo-unit-sopot-a": [
        { startDate: "2026-05-05", endDate: "2026-05-09" },
        { startDate: "2026-06-18", endDate: "2026-06-22" },
        { startDate: "2026-07-04", endDate: "2026-07-08" },
    ],
    "demo-unit-sopot-b": [
        { startDate: "2026-06-24", endDate: "2026-06-28" },
    ],
    "demo-unit-sopot-c": [
        { startDate: "2026-07-10", endDate: "2026-07-13" },
    ],
    "demo-unit-zakopane-a": [
        { startDate: "2026-05-26", endDate: "2026-05-31" },
    ],
    "demo-unit-zakopane-b": [
        { startDate: "2026-06-06", endDate: "2026-06-09" },
        { startDate: "2026-07-26", endDate: "2026-07-29" },
    ],
    "demo-unit-mazury-a": [],
    "demo-unit-mazury-b": [
        { startDate: "2026-06-26", endDate: "2026-06-30" },
        { startDate: "2026-07-15", endDate: "2026-07-20" },
    ],
    "demo-unit-krakow-a": [
        { startDate: "2026-05-18", endDate: "2026-05-21" },
    ],
    "demo-unit-krakow-b": [
        { startDate: "2026-06-02", endDate: "2026-06-05" },
    ],
};

export function getDemoUnits(propertyId: string, currency = "PLN") {
    const units = demoUnitsByProperty[propertyId] ?? [];
    if (currency === "PLN") return units;

    const rates: Record<string, number> = { EUR: 4.32, USD: 3.98 };
    const rate = rates[currency] ?? 1;

    return units.map((unit) => ({
        ...unit,
        convertedPricePerNight: Number((unit.pricePerNight / rate).toFixed(2)),
        currencyInfo: {
            baseCurrency: "PLN",
            displayCurrency: currency,
            exchangeRate: rate,
            rateEffectiveDate: "2026-06-09",
        },
    }));
}
