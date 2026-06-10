export const COMMON_AMENITIES = [
    "WiFi", "Parking", "Pet Friendly", "TV", "AC", "Kitchen", "Pool", "Gym", "Breakfast",
];

export const PROPERTY_TYPES = ["Apartment", "House", "Villa", "Studio", "Room"];

export interface FilterState {
    selectedAmenities: string[];
    minPrice: string;
    maxPrice: string;
    propertyTypes: string[];
    guests: number;
    bedrooms: number;
    beds: number;
}

export const EMPTY_FILTERS: FilterState = {
    selectedAmenities: [],
    minPrice: "",
    maxPrice: "",
    propertyTypes: [],
    guests: 1,
    bedrooms: 0,
    beds: 0,
};
