export interface ReservationOverview {
    id: string;
    guestName: string;
    unitName: string;
    startDate: string;
    endDate: string;
    status: string;
    userId?: string; // Optional if needed for backwards compatibility
}

export interface ReservationDetails extends ReservationOverview {
    unitId: string;
    createdAt: string;
}
