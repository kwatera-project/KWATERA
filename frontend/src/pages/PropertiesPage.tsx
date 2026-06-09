import { useEffect, useMemo, useState } from "react";
import { getProperties, getUnits } from "../api/propertyApi";
import { checkAvailability } from "../api/availabilityApi";
import type { Property, Unit } from "../types/property";
import { Link, useSearchParams } from "react-router-dom";
import PropertySearchBar, { type PropertySearchValues } from "../components/PropertySearchBar";
import { formatSearchDate, parseGuests, parseSearchDate } from "../utils/searchDates";
import { getCitySuggestions } from "../utils/citySuggestions";
import PropertyMap from "../components/PropertyMap";
import { useCurrency } from "../contexts/CurrencyContext";

interface AvailabilityResponse {
    available: boolean;
    message: string;
}

const PROPERTIES_LOAD_ERROR = "Could not load properties. Please try again later.";
const UNITS_FILTER_ERROR = "Could not load units for filtering. Please try again later.";
const AVAILABILITY_FILTER_ERROR = "Could not verify availability. Please try again or adjust your filters.";

function propertyMatchesCity(property: Property, city: string) {
    const normalizeCityText = (value: string) => value.toLowerCase().replace(/[,\s]+/g, " ").trim();
    const normalizedCity = normalizeCityText(city);
    if (!normalizedCity) return true;

    const searchableText = [
        property.city,
        property.country,
    ]
        .filter(Boolean)
        .join(" ")
        .toLowerCase();

    return normalizeCityText(searchableText).includes(normalizedCity);
}

export default function PropertiesPage() {
    const [properties, setProperties] = useState<Property[]>([]);
    const [filteredProperties, setFilteredProperties] = useState<Property[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [isFilteringAvailability, setIsFilteringAvailability] = useState(false);
    const [propertiesError, setPropertiesError] = useState<string | null>(null);
    const [filterError, setFilterError] = useState<string | null>(null);
    const [searchParams, setSearchParams] = useSearchParams();

    const [mapBounds, setMapBounds] = useState<{
        minLat: number;
        maxLat: number;
        minLng: number;
        maxLng: number;
    } | null>(null);
    const [selectedProperty, setSelectedProperty] = useState<Property | null>(null);
    const [showMap, setShowMap] = useState<boolean>(false);
    const [openPopupPropertyId, setOpenPopupPropertyId] = useState<string | null>(null);

    const { currency } = useCurrency();
    const [propertyPrices, setPropertyPrices] = useState<Record<string, number>>({});

    const searchValues = useMemo(() => ({
        city: searchParams.get("city") ?? "",
        checkIn: parseSearchDate(searchParams.get("checkIn")),
        checkOut: parseSearchDate(searchParams.get("checkOut")),
        guests: searchParams.get("guests") ?? "",
    }), [searchParams]);

    const hasCompleteDateRange = !!searchValues.checkIn && !!searchValues.checkOut && searchValues.checkIn < searchValues.checkOut;
    const requestedGuests = parseGuests(searchValues.guests);
    const citySuggestions = useMemo(() => getCitySuggestions(properties), [properties]);
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
        if (filteredProperties.length === 0) return;

        filteredProperties.forEach((property) => {
            getUnits(property.id, currency)
                .then((units: Unit[]) => {
                    if (units && units.length > 0) {
                        const pricesList = units
                            .map((u) => (u.convertedPricePerNight && currency !== "PLN" ? u.convertedPricePerNight : u.pricePerNight))
                            .filter((p) => p !== undefined && p !== null);
                        if (pricesList.length > 0) {
                            const minVal = Math.min(...pricesList);
                            setPropertyPrices((prev) => ({
                                ...prev,
                                [property.id]: minVal,
                            }));
                        }
                    }
                })
                .catch((err) => console.error("Error loading price for card:", err));
        });
    }, [filteredProperties, currency]);

    useEffect(() => {
        if (selectedProperty && showMap) {
            const element = document.getElementById(`property-card-${selectedProperty.id}`);
            if (element) {
                element.scrollIntoView({ behavior: "smooth", block: "nearest" });
            }
        }
    }, [selectedProperty, showMap]);

    useEffect(() => {
        let cancelled = false;

        async function filterProperties() {
            const cityFiltered = properties.filter((property) => propertyMatchesCity(property, searchValues.city));

            if (!requestedGuests && (!hasCompleteDateRange || !searchValues.checkIn || !searchValues.checkOut)) {
                setIsFilteringAvailability(false);
                setFilterError(null);
                setFilteredProperties(cityFiltered);
                return;
            }

            setIsFilteringAvailability(true);
            try {
                const checkIn = searchValues.checkIn ? formatSearchDate(searchValues.checkIn) : null;
                const checkOut = searchValues.checkOut ? formatSearchDate(searchValues.checkOut) : null;

                const filteredResults = await Promise.all(
                    cityFiltered.map(async (property) => {
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
    }, [properties, searchValues.city, searchValues.checkIn, searchValues.checkOut, searchValues.guests, hasCompleteDateRange, requestedGuests]);

    const visibleProperties = useMemo(() => {
        if (!mapBounds) return filteredProperties;
        return filteredProperties.filter((p) => {
            if (p.latitude === undefined || p.longitude === undefined || p.latitude === null || p.longitude === null) {
                return false;
            }
            const lat = Number(p.latitude);
            const lng = Number(p.longitude);
            return (
                lat >= mapBounds.minLat &&
                lat <= mapBounds.maxLat &&
                lng >= mapBounds.minLng &&
                lng <= mapBounds.maxLng
            );
        });
    }, [filteredProperties, mapBounds]);

    const handleSearch = ({ city, checkIn, checkOut, guests }: PropertySearchValues) => {
        const params = new URLSearchParams();
        if (city) params.set("city", city);
        if (checkIn) params.set("checkIn", formatSearchDate(checkIn));
        if (checkOut) params.set("checkOut", formatSearchDate(checkOut));
        if (guests) params.set("guests", guests);
        setSearchParams(params);
    };

    const toggleMap = () => {
        setShowMap((prev) => {
            const next = !prev;
            if (!next) {
                setSelectedProperty(null);
                setOpenPopupPropertyId(null);
            }
            return next;
        });
    };

    return (
        <div className="p-4 md:p-8 max-w-7xl mx-auto min-h-screen text-[#1A1A1A] flex flex-col">
            <div className="mb-6 space-y-4 shrink-0">
                <div className="flex justify-between items-center">
                    <div>
                        <h1 className="text-3xl font-bold text-[#1A1A1A] tracking-tight">Properties</h1>
                        <p className="text-sm text-[#7A7A7A] mt-1">Explore our curated selection of properties.</p>
                    </div>
                    <button
                        onClick={toggleMap}
                        className="hidden md:flex items-center gap-2 bg-[#42211D] hover:bg-[#5c2e29] text-white px-5 py-2.5 rounded-xl shadow-sm font-semibold transition-all duration-300 cursor-pointer animate-fade-in"
                    >
                        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            {showMap ? (
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                            ) : (
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" />
                            )}
                        </svg>
                        {showMap ? "Hide map" : "Show map"}
                    </button>
                </div>
                <PropertySearchBar
                    key={searchParams.toString()}
                    initialValues={searchValues}
                    onSearch={handleSearch}
                    citySuggestions={citySuggestions}
                    className="max-w-none"
                />
            </div>

            {(isLoading || isFilteringAvailability) && (
                <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-6 text-center text-[#7A7A7A] font-medium mb-6 shrink-0 animate-pulse">
                    {isLoading ? "Loading properties..." : "Checking availability..."}
                </div>
            )}

            {(propertiesError || filterError) && (
                <div className="bg-red-50 border border-red-200 rounded-xl p-6 text-red-700 font-semibold mb-6 shrink-0">
                    {propertiesError || filterError}
                </div>
            )}

            {!isLoading && !propertiesError && (
                <div className="flex-1 flex flex-col md:flex-row gap-6 min-h-0 relative">
                    <div
                        className={`flex-1 overflow-y-auto pr-2 ${
                            showMap ? "hidden md:block" : "block"
                        }`}
                        style={{ maxHeight: "calc(100vh - 280px)" }}
                    >
                        {filteredProperties.length === 0 ? (
                            <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-8 text-center">
                                <h2 className="text-xl font-bold text-[#1A1A1A] tracking-tight">No properties found</h2>
                                <p className="text-sm text-[#7A7A7A] mt-2">
                                    Try a different city or date range.
                                </p>
                            </div>
                        ) : visibleProperties.length === 0 && showMap ? (
                            <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-8 text-center">
                                <h2 className="text-xl font-bold text-[#1A1A1A] tracking-tight">No properties in this map area</h2>
                                <p className="text-sm text-[#7A7A7A] mt-2">
                                    Pan the map or zoom out to see more properties.
                                </p>
                            </div>
                        ) : (
                            <div
                                className={`grid gap-6 pb-6 ${
                                    showMap
                                        ? "grid-cols-1 lg:grid-cols-2"
                                        : "grid-cols-1 md:grid-cols-2 lg:grid-cols-3"
                                }`}
                            >
                                {(showMap ? visibleProperties : filteredProperties).map((p) => {
                                    const isSelected = selectedProperty?.id === p.id;
                                    return (
                                        <div
                                            key={p.id}
                                            id={`property-card-${p.id}`}
                                            onMouseEnter={() => showMap && setSelectedProperty(p)}
                                            className="relative p-1"
                                        >
                                            <Link
                                                to={`/property/${p.id}${propertyDetailsSearch}`}
                                                onClick={(e) => {
                                                    if (showMap) {
                                                        e.preventDefault();
                                                        setSelectedProperty(p);
                                                        setOpenPopupPropertyId(p.id);
                                                    }
                                                }}
                                                className="group block"
                                            >
                                                <div
                                                    className={`bg-white border rounded-xl p-4 hover:shadow-md transition-all duration-300 transform hover:-translate-y-1 ${
                                                        isSelected
                                                            ? "border-[#42211D] ring-2 ring-inset ring-[#42211D]/30 bg-[#42211D]/5 shadow-md"
                                                            : "border-[#DACDCA] shadow-sm"
                                                    }`}
                                                >
                                                    <div className="relative aspect-[4/3] w-full overflow-hidden rounded-lg bg-gray-50">
                                                        <img
                                                            src={p.imageUrl}
                                                            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                                                            alt={p.title}
                                                        />
                                                    </div>
                                                    <div className="flex items-baseline justify-between mt-3">
                                                        <div className="min-w-0 flex-1">
                                                            <h2 className="text-lg font-bold text-[#1A1A1A] tracking-tight group-hover:text-[#42211D] transition-colors line-clamp-1">
                                                                {p.title}
                                                            </h2>
                                                            <p className="text-xs text-[#7A7A7A] mt-0.5 font-medium">{p.city}</p>
                                                        </div>
                                                        <div className="text-right shrink-0 ml-4">
                                                            <span className="text-[9px] text-[#7A7A7A] block font-bold uppercase tracking-wider leading-none mb-0.5">From</span>
                                                            <span className="font-black text-base text-[#42211D]">
                                                                {propertyPrices[p.id] !== undefined ? (
                                                                    `${Math.round(propertyPrices[p.id])} ${currency}`
                                                                ) : (
                                                                    <span className="text-xs font-normal text-[#7A7A7A]">...</span>
                                                                )}
                                                                <span className="text-[10px] text-[#7A7A7A] font-normal ml-0.5">/night</span>
                                                            </span>
                                                        </div>
                                                    </div>
                                                </div>
                                            </Link>
                                        </div>
                                    );
                                })}
                            </div>
                        )}
                    </div>

                    {showMap && (
                        <div
                            className="flex-1 h-[450px] md:h-auto md:sticky md:top-6"
                            style={{ height: "calc(100vh - 280px)" }}
                        >
                            <div className="w-full h-full rounded-xl overflow-hidden border border-[#DACDCA] shadow-sm relative z-0">
                                <PropertyMap
                                    properties={filteredProperties}
                                    onBoundsChange={setMapBounds}
                                    onPropertySelect={setSelectedProperty}
                                    propertyDetailsSearch={propertyDetailsSearch}
                                    selectedProperty={selectedProperty}
                                    openPopupPropertyId={openPopupPropertyId}
                                    setOpenPopupPropertyId={setOpenPopupPropertyId}
                                />
                            </div>
                        </div>
                    )}
                </div>
            )}

            {!isLoading && !propertiesError && filteredProperties.length > 0 && (
                <button
                    onClick={toggleMap}
                    className="fixed bottom-6 left-1/2 transform -translate-x-1/2 z-[1000] bg-[#42211D] hover:bg-[#5c2e29] text-white px-6 py-3 rounded-full shadow-lg font-bold flex items-center gap-2 md:hidden transition-all duration-300 cursor-pointer"
                >
                    {showMap ? (
                        <>
                            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                            </svg>
                            Hide map
                        </>
                    ) : (
                        <>
                            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" />
                            </svg>
                            Show map
                        </>
                    )}
                </button>
            )}
        </div>
    );
}
