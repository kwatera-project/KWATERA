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
    city?: string;
    guestEmail?: string;
    pricePerNightSnapshot?: number;
    totalPrice?: number;
    convertedTotalPrice?: number;
    currencyInfo?: CurrencyMetadata;
    ownerName?: string;
    ownerEmail?: string;
    guestMessage?: string;
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

export interface Occupancy {
    reservationId: string;
    unitId: string;
    unitName?: string;
    startDate: string;
    endDate: string;
    status: string;
    guestName?: string;
    totalPrice?: number;
    guestEmail?: string;
}