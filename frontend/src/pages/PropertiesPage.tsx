import { useEffect, useMemo, useState } from "react";
import { getProperties, getUnits } from "../api/propertyApi";
import { checkAvailability } from "../api/availabilityApi";
import type { Property, Unit } from "../types/property";
import { Link, useSearchParams } from "react-router-dom";
import PropertySearchBar, { type PropertySearchValues } from "../components/PropertySearchBar";
import { formatSearchDate, parseGuests, parseSearchDate } from "../utils/searchDates";

interface AvailabilityResponse {
    available: boolean;
    message: string;
}

const PROPERTIES_LOAD_ERROR = "Could not load properties. Please try again later.";
const UNITS_FILTER_ERROR = "Could not load units for filtering. Please try again later.";
const AVAILABILITY_FILTER_ERROR = "Could not verify availability. Please try again or adjust your filters.";

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
    const [propertiesError, setPropertiesError] = useState<string | null>(null);
    const [filterError, setFilterError] = useState<string | null>(null);
    const [searchParams, setSearchParams] = useSearchParams();

    const searchValues = useMemo(() => ({
        location: searchParams.get("location") ?? "",
        checkIn: parseSearchDate(searchParams.get("checkIn")),
        checkOut: parseSearchDate(searchParams.get("checkOut")),
        guests: searchParams.get("guests") ?? "",
    }), [searchParams]);

    const hasCompleteDateRange = !!searchValues.checkIn && !!searchValues.checkOut && searchValues.checkIn < searchValues.checkOut;
    const requestedGuests = parseGuests(searchValues.guests);
    const propertyDetailsSearch = useMemo(() => {
        const params = new URLSearchParams();
        const checkIn = searchParams.get("checkIn");
        const checkOut = searchParams.get("checkOut");
        const guests = searchParams.get("guests");

        if (checkIn) params.set("checkIn", checkIn);
        if (checkOut) params.set("checkOut", checkOut);
        if (guests) params.set("guests", guests);

        const serialized = params.toString();
        return serialized ? `?${serialized}` : "";
    }, [searchParams]);

    useEffect(() => {
        getProperties()
            .then((data: Property[]) => {
                if (!Array.isArray(data)) {
                    throw new Error(PROPERTIES_LOAD_ERROR);
                }
                setProperties(data);
                setPropertiesError(null);
            })
            .catch(() => {
                setProperties([]);
                setFilteredProperties([]);
                setPropertiesError(PROPERTIES_LOAD_ERROR);
            })
            .finally(() => setIsLoading(false));
    }, []);

    useEffect(() => {
        let cancelled = false;

        async function filterProperties() {
            const locationFiltered = properties.filter((property) => propertyMatchesLocation(property, searchValues.location));

            if (!requestedGuests && (!hasCompleteDateRange || !searchValues.checkIn || !searchValues.checkOut)) {
                setIsFilteringAvailability(false);
                setFilterError(null);
                setFilteredProperties(locationFiltered);
                return;
            }

            setIsFilteringAvailability(true);
            try {
                const checkIn = searchValues.checkIn ? formatSearchDate(searchValues.checkIn) : null;
                const checkOut = searchValues.checkOut ? formatSearchDate(searchValues.checkOut) : null;

                const filteredResults = await Promise.all(
                    locationFiltered.map(async (property) => {
                        let units: Unit[];
                        try {
                            units = await getUnits(property.id);
                            if (!Array.isArray(units)) {
                                throw new Error(UNITS_FILTER_ERROR);
                            }
                        } catch {
                            throw new Error(UNITS_FILTER_ERROR);
                        }

                        const matchingUnits = requestedGuests
                            ? units.filter((unit) => unit.capacity >= requestedGuests)
                            : units;

                        if (matchingUnits.length === 0) return null;

                        if (!hasCompleteDateRange || !checkIn || !checkOut) {
                            return property;
                        }

                        const unitResults = await Promise.all(
                            matchingUnits.map((unit) =>
                                checkAvailability(unit.id, checkIn, checkOut)
                                    .then((availability: AvailabilityResponse) => {
                                        if (typeof availability.available !== "boolean") {
                                            throw new Error(AVAILABILITY_FILTER_ERROR);
                                        }
                                        return availability.available;
                                    })
                                    .catch(() => {
                                        throw new Error(AVAILABILITY_FILTER_ERROR);
                                    })
                            )
                        );

                        return unitResults.some(Boolean) ? property : null;
                    })
                );

                if (!cancelled) {
                    setFilterError(null);
                    setFilteredProperties(filteredResults.filter((property): property is Property => property !== null));
                }
            } catch (err: unknown) {
                if (!cancelled) {
                    const message = err instanceof Error ? err.message : UNITS_FILTER_ERROR;
                    setFilterError(message === AVAILABILITY_FILTER_ERROR ? AVAILABILITY_FILTER_ERROR : UNITS_FILTER_ERROR);
                    setFilteredProperties([]);
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
    }, [properties, searchValues.location, searchValues.checkIn, searchValues.checkOut, searchValues.guests, hasCompleteDateRange, requestedGuests]);

    const handleSearch = ({ location, checkIn, checkOut, guests }: PropertySearchValues) => {
        const params = new URLSearchParams();
        if (location) params.set("location", location);
        if (checkIn) params.set("checkIn", formatSearchDate(checkIn));
        if (checkOut) params.set("checkOut", formatSearchDate(checkOut));
        if (guests) params.set("guests", guests);
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

            {(propertiesError || filterError) && (
                <div className="bg-red-50 border border-red-200 rounded-xl p-6 text-red-700 font-semibold mb-8">
                    {propertiesError || filterError}
                </div>
            )}

            {!isLoading && !isFilteringAvailability && !propertiesError && !filterError && filteredProperties.length === 0 && (
                <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-8 text-center mb-8">
                    <h2 className="text-xl font-bold text-[#1A1A1A] tracking-tight">No properties found</h2>
                    <p className="text-sm text-[#7A7A7A] mt-2">
                        Try a different location or date range.
                    </p>
                </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                {filteredProperties.map(p => (
                    <Link key={p.id} to={`/property/${p.id}${propertyDetailsSearch}`} className="group block">
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
