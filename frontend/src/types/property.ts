export interface Property {
    id: string;
    title: string;
    description: string;
    location: string;
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
}