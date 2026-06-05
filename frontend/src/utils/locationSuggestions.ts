import type { Property } from "../types/property";

export function getLocationSuggestions(properties: Property[]) {
    return Array.from(
        new Set(
            properties
                .map((property) => {
                    if (property.city && property.country) {
                        return `${property.city}, ${property.country}`;
                    }

                    return property.city || property.country;
                })
                .filter((location): location is string => Boolean(location))
        )
    ).sort((a, b) => a.localeCompare(b));
}
