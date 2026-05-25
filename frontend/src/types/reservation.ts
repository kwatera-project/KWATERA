export interface CurrencyMetadata {
    baseCurrency: string;
    displayCurrency: string;
    exchangeRate: number;
    rateEffectiveDate: string;
}

export interface ReservationOverview {
    id: string;
    guestName: string;
    unitName: string;
    startDate: string;
    endDate: string;
    status: string;
    userId?: string;
}

export interface ReservationDetails extends ReservationOverview {
    unitId: string;
    createdAt: string;
    pricePerNightSnapshot?: number;
    totalPrice?: number;
    convertedTotalPrice?: number;
    currencyInfo?: CurrencyMetadata;
}

export interface GuestReservation {
    id: string;
    unitId: string;
    startDate: string;
    endDate: string;
    status: string;
    totalPrice?: number;
    convertedTotalPrice?: number;
    currencyInfo?: CurrencyMetadata;
}