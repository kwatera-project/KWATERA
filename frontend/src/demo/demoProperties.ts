import type { Property, Unit } from "../types/property";

const img = (id: number) => `https://images.pexels.com/photos/${id}/pexels-photo-${id}.jpeg?auto=compress&cs=tinysrgb&w=1200`;

export const demoProperties: Property[] = [
    {
        id: "demo-property-sopot",
        title: "Apartament Nadmorski Sopot",
        description: "Jasny apartament blisko plaży, z rozliczeniem mediów i automatycznym panelem rezerwacji.",
        city: "Sopot",
        country: "Polska",
        postalCode: "81-718",
        street: "Grunwaldzka",
        streetNumber: "12",
        imageUrl: img(1571460),
    },
    {
        id: "demo-property-zakopane",
        title: "Domek Widokowy Zakopane",
        description: "Całoroczny domek z widokiem na Tatry, przygotowany pod dłuższe pobyty rodzinne.",
        city: "Zakopane",
        country: "Polska",
        postalCode: "34-500",
        street: "Kościeliska",
        streetNumber: "44",
        imageUrl: img(754268),
    },
    {
        id: "demo-property-mazury",
        title: "Mazurska Przystań",
        description: "Kameralny obiekt nad jeziorem z kilkoma jednostkami i sezonowym obłożeniem.",
        city: "Mikołajki",
        country: "Polska",
        postalCode: "11-730",
        street: "Jeziorna",
        streetNumber: "8",
        imageUrl: img(261102),
    },
];

export const demoUnitsByProperty: Record<string, Unit[]> = {
    "demo-property-sopot": [
        {
            id: "demo-unit-sopot-a",
            propertyId: "demo-property-sopot",
            name: "Studio Baltic",
            description: "Kompaktowe studio dla pary z balkonem i szybkim check-in.",
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
            name: "Apartament Rodzinny",
            description: "Dwie sypialnie, salon i aneks kuchenny dla rodzinnych wyjazdów.",
            pricePerNight: 690,
            capacity: 5,
            imageUrl: img(1643383),
            unitType: "APARTMENT",
            unitNumber: "B4",
            floor: 4,
        },
    ],
    "demo-property-zakopane": [
        {
            id: "demo-unit-zakopane-a",
            propertyId: "demo-property-zakopane",
            name: "Domek Giewont",
            description: "Drewniany domek z kominkiem, tarasem i miejscem parkingowym.",
            pricePerNight: 780,
            capacity: 6,
            imageUrl: img(106399),
            unitType: "HOUSE",
            unitNumber: "1",
            floor: 0,
        },
    ],
    "demo-property-mazury": [
        {
            id: "demo-unit-mazury-a",
            propertyId: "demo-property-mazury",
            name: "Pokój Portowy",
            description: "Pokój dla dwóch osób z widokiem na marinę.",
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
            name: "Apartament Jeziorny",
            description: "Przestronny apartament dla czterech osób z aneksem i tarasem.",
            pricePerNight: 540,
            capacity: 4,
            imageUrl: img(1457842),
            unitType: "APARTMENT",
            unitNumber: "7",
            floor: 2,
        },
    ],
};

export const demoPropertyImages: Record<string, string[]> = Object.fromEntries(
    demoProperties.map((property) => [
        property.id,
        [
            property.imageUrl,
            img(property.id.includes("sopot") ? 271624 : property.id.includes("zakopane") ? 106399 : 271619),
            img(property.id.includes("sopot") ? 1643383 : property.id.includes("zakopane") ? 259588 : 1457842),
        ],
    ])
);

export const demoOccupiedDatesByUnit: Record<string, { startDate: string; endDate: string }[]> = {
    "demo-unit-sopot-a": [{ startDate: "2026-06-14", endDate: "2026-06-18" }],
    "demo-unit-sopot-b": [{ startDate: "2026-06-20", endDate: "2026-06-24" }],
    "demo-unit-zakopane-a": [{ startDate: "2026-06-16", endDate: "2026-06-21" }],
    "demo-unit-mazury-a": [{ startDate: "2026-06-12", endDate: "2026-06-15" }],
    "demo-unit-mazury-b": [{ startDate: "2026-06-26", endDate: "2026-06-30" }],
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
