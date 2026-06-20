export const COMMON_AMENITIES = [
    "WiFi",
    "Parking",
    "Fireplace",
    "BBQ",
    "Hot Tub",
    "Terrace",
    "Sauna",
    "Kayaks",
    "Elevator",
    "Air Conditioning",
    "Balcony",
    "Gym",
    "Shared Kitchen",
    "River View",
    "Sea View",
    "Historic View"
];

export const UNIT_TYPES = ["Entire apartment", "Entire cottage", "Private room in home"
];

export interface FilterState {
    selectedAmenities: string[];
    minPrice: string;
    maxPrice: string;
    unitTypes: string[];
    guests: number;
    bedrooms: number;
    beds: number;
}

export const EMPTY_FILTERS: FilterState = {
    selectedAmenities: [],
    minPrice: "",
    maxPrice: "",
    unitTypes: [],
    guests: 1,
    bedrooms: 0,
    beds: 0,
};
