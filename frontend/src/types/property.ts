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
    latitude: number;
    longitude: number;
    amenities: string[];
    propertyType?: string;
}

export interface PropertyCreateRequest {
    title: string;
    description: string;
    city: string;
    country: string;
    postalCode: string;
    street: string;
    streetNumber: string;
    amenities?: string[];
    propertyType?: string;
}

export interface PropertyUpdateRequest {
    title?: string;
    description?: string;
    city?: string;
    country?: string;
    postalCode?: string;
    street?: string;
    streetNumber?: string;
    amenities?: string[];
    propertyType?: string;
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
    unitType: string;
    unitNumber: string;
    floor: number;
    amenities: string[];
    bedrooms: number;
    beds: number;
}

export interface UnitSettlementItem {
    id: string;
    unitId: string;
    settlementItemType: "ACCOMMODATION" | "DEPOSIT" | "ELECTRICITY" | "WATER" | "CLEANING_FEE" | string;
    pricePerUnit: number;
    measurementUnit?: "M3" | "KWH" | string | null;
    billingType: "FIXED" | "PER_USAGE" | string;
}

export interface UnitCreateRequest {
    name: string;
    description: string;
    pricePerNight: number;
    capacity: number;
    unitType: string;
    unitNumber: string;
    floor: number;
    amenities?: string[];
    bedrooms?: number;
    beds?: number;
}

export interface UnitUpdateRequest {
    name?: string;
    description?: string;
    pricePerNight?: number;
    capacity?: number;
    unitType?: string;
    unitNumber?: string;
    floor?: number;
    amenities?: string[];
    bedrooms?: number;
    beds?: number;
}
