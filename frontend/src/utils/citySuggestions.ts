import type { Property } from "../types/property";

export function getCitySuggestions(properties: Property[]) {
    return Array.from(
        new Set(
            properties
                .map((property) => {
                    if (property.city && property.country) {
                        return `${property.city}, ${property.country}`;
                    }

                    return property.city || property.country;
                })
                .filter((city): city is string => Boolean(city))
        )
    ).sort((a, b) => a.localeCompare(b));
}
