import { useEffect, useMemo, useState } from "react";
import { getProperties, getUnits } from "../api/propertyApi";
import { checkAvailability } from "../api/availabilityApi";
import type { Property, Unit } from "../types/property";
import { Link, useSearchParams } from "react-router-dom";
import { isValid, parseISO } from "date-fns";
import PropertySearchBar, { type PropertySearchValues } from "../components/PropertySearchBar";
import { formatSearchDate } from "../utils/searchDates";

interface AvailabilityResponse {
    available: boolean;
    message: string;
}

function parseSearchDate(value: string | null) {
    if (!value) return null;
    const parsed = parseISO(value);
    return isValid(parsed) ? parsed : null;
}

function propertyMatchesLocation(property: Property, location: string) {
    const normalizedLocation = location.trim().toLowerCase();
    if (!normalizedLocation) return true;

    const searchableText = [
        property.title,
        property.city,
        property.country,
        property.postalCode,
        property.street,
        property.streetNumber,
    ]
        .filter(Boolean)
        .join(" ")
        .toLowerCase();

    return searchableText.includes(normalizedLocation);
}

export default function PropertiesPage() {
    const [properties, setProperties] = useState<Property[]>([]);
    const [filteredProperties, setFilteredProperties] = useState<Property[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [isFilteringAvailability, setIsFilteringAvailability] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [searchParams, setSearchParams] = useSearchParams();

    const searchValues = useMemo(() => ({
        location: searchParams.get("location") ?? "",
        checkIn: parseSearchDate(searchParams.get("checkIn")),
        checkOut: parseSearchDate(searchParams.get("checkOut")),
    }), [searchParams]);

    const hasCompleteDateRange = !!searchValues.checkIn && !!searchValues.checkOut && searchValues.checkIn < searchValues.checkOut;

    useEffect(() => {
        getProperties()
            .then((data: Property[]) => {
                setProperties(data);
                setError(null);
            })
            .catch((err: unknown) => {
                const message = err instanceof Error ? err.message : "Unable to load properties.";
                setError(message);
            })
            .finally(() => setIsLoading(false));
    }, []);

    useEffect(() => {
        let cancelled = false;

        async function filterProperties() {
            const locationFiltered = properties.filter((property) => propertyMatchesLocation(property, searchValues.location));

            if (!hasCompleteDateRange || !searchValues.checkIn || !searchValues.checkOut) {
                setFilteredProperties(locationFiltered);
                return;
            }

            setIsFilteringAvailability(true);
            try {
                const checkIn = formatSearchDate(searchValues.checkIn);
                const checkOut = formatSearchDate(searchValues.checkOut);

                const availabilityResults = await Promise.all(
                    locationFiltered.map(async (property) => {
                        try {
                            const units: Unit[] = await getUnits(property.id);
                            if (units.length === 0) return null;

                            const unitResults = await Promise.all(
                                units.map((unit) =>
                                    checkAvailability(unit.id, checkIn, checkOut)
                                        .then((availability: AvailabilityResponse) => availability.available)
                                        .catch(() => false)
                                )
                            );

                            return unitResults.some(Boolean) ? property : null;
                        } catch {
                            return null;
                        }
                    })
                );

                if (!cancelled) {
                    setFilteredProperties(availabilityResults.filter((property): property is Property => property !== null));
                }
            } finally {
                if (!cancelled) {
                    setIsFilteringAvailability(false);
                }
            }
        }

        filterProperties();

        return () => {
            cancelled = true;
        };
    }, [properties, searchValues.location, searchValues.checkIn, searchValues.checkOut, hasCompleteDateRange]);

    const handleSearch = ({ location, checkIn, checkOut }: PropertySearchValues) => {
        const params = new URLSearchParams();
        if (location) params.set("location", location);
        if (checkIn) params.set("checkIn", formatSearchDate(checkIn));
        if (checkOut) params.set("checkOut", formatSearchDate(checkOut));
        setSearchParams(params);
    };

    return (
        <div className="p-8 max-w-7xl mx-auto min-h-screen text-[#1A1A1A]">
            <div className="mb-8 space-y-6">
                <h1 className="text-3xl font-bold text-[#1A1A1A] tracking-tight">Properties</h1>
                <p className="text-sm text-[#7A7A7A] mt-1">Explore our curated selection of properties.</p>
                <PropertySearchBar
                    key={searchParams.toString()}
                    initialValues={searchValues}
                    onSearch={handleSearch}
                    className="max-w-none"
                />
            </div>

            {(isLoading || isFilteringAvailability) && (
                <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-6 text-center text-[#7A7A7A] font-medium mb-8">
                    {isLoading ? "Loading properties..." : "Checking availability..."}
                </div>
            )}

            {error && (
                <div className="bg-red-50 border border-red-200 rounded-xl p-6 text-red-700 font-semibold mb-8">
                    {error}
                </div>
            )}

            {!isLoading && !isFilteringAvailability && !error && filteredProperties.length === 0 && (
                <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-8 text-center mb-8">
                    <h2 className="text-xl font-bold text-[#1A1A1A] tracking-tight">No properties found</h2>
                    <p className="text-sm text-[#7A7A7A] mt-2">
                        Try a different location or date range.
                    </p>
                </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                {filteredProperties.map(p => (
                    <Link key={p.id} to={`/property/${p.id}`} className="group block">
                        <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-6 hover:shadow-md transition-all duration-300 transform hover:-translate-y-1">
                            <img src={p.imageUrl} className="w-full h-100 object-cover rounded-lg" alt={p.title} />
                            <h2 className="text-2xl font-bold text-[#1A1A1A] tracking-tight mt-4 group-hover:text-[#42211D] transition-colors">{p.title}</h2>
                            <p className="text-sm text-[#7A7A7A] mt-1 font-medium">{p.city}</p>
                        </div>
                    </Link>
                ))}
            </div>
        </div>
    );
}
