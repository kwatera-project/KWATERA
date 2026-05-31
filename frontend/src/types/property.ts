export interface Property {
    id: string;
    title: string;
    description: string;
    city: string;
    country: string;
    postalCode: string;
    street: string;
    streetNumber: string;
    imageUrl: string;
}

export interface Unit {
    id: string;
    propertyId: string;
    name: string;
    description: string;
    pricePerNight: number;
    capacity: number;
    imageUrl?: string;
    convertedPricePerNight?: number;
    currencyInfo?: {
        baseCurrency: string;
        displayCurrency: string;
        exchangeRate: number;
        rateEffectiveDate: string;
    };
    unit_type: string;
    unit_number: string;
    floor: number;
}