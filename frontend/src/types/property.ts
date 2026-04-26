export interface Property {
    id: string;
    title: string;
    description: string;
    location: string;
    imageUrl: string;
}

export interface Unit {
    id: string;
    name: string;
    description: string;
    pricePerNight: number;
    capacity: number;
    imageUrl: string;
}