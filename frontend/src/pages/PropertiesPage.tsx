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
import FilterSidebar from "../components/FilterSidebar";
import {
    EMPTY_FILTERS,
    type FilterState,
} from "../types/filters";
import { useDebounce } from "../hooks/useDebounce";
import { SlidersHorizontal, X } from "lucide-react";

interface AvailabilityResponse {
    available: boolean;
    message: string;
}

const PROPERTIES_LOAD_ERROR = "Could not load properties. Please try again later.";
const UNITS_FILTER_ERROR = "Could not load units for filtering. Please try again later.";
const AVAILABILITY_FILTER_ERROR = "Could not verify availability. Please try again or adjust your filters.";

function propertyMatchesCity(property: Property, city: string) {
    const normalize = (v: string) => v.toLowerCase().replace(/[,\s]+/g, " ").trim();
    const normalizedCity = normalize(city);
    if (!normalizedCity) return true;
    const text = [property.city, property.country].filter(Boolean).join(" ");
    return normalize(text).includes(normalizedCity);
}

interface ActiveFilter {
    id: string;
    label: string;
    onRemove: () => void;
}

function FilterBadge({ label, onRemove }: { label: string; onRemove: () => void }) {
    return (
        <span className="inline-flex items-center gap-1.5 bg-blue-50 text-blue-700 border border-blue-100 rounded-full px-3 py-1 text-xs font-semibold whitespace-nowrap shrink-0 transition-colors hover:bg-blue-100">
            {label}
            <button
                onClick={onRemove}
                aria-label={`Remove filter: ${label}`}
                className="hover:text-blue-900 transition-colors cursor-pointer"
            >
                <X className="w-3 h-3" strokeWidth={2.5} />
            </button>
        </span>
    );
}

export default function PropertiesPage() {
    const [properties, setProperties] = useState<Property[]>([]);
    const [filteredProperties, setFilteredProperties] = useState<Property[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [isFilteringAvailability, setIsFilteringAvailability] = useState(false);
    const [propertiesError, setPropertiesError] = useState<string | null>(null);
    const [filterError, setFilterError] = useState<string | null>(null);
    const [searchParams, setSearchParams] = useSearchParams();

    const [isMobileSidebarOpen, setIsMobileSidebarOpen] = useState(false);
    const [mapBounds, setMapBounds] = useState<{
        minLat: number; maxLat: number; minLng: number; maxLng: number;
    } | null>(null);
    const [selectedProperty, setSelectedProperty] = useState<Property | null>(null);
    const [showMap, setShowMap] = useState(false);
    const [openPopupPropertyId, setOpenPopupPropertyId] = useState<string | null>(null);

    const [filters, setFilters] = useState<FilterState>(EMPTY_FILTERS);

    const debouncedFilters = useDebounce(filters, 450);

    const { currency } = useCurrency();
    const [propertyPrices, setPropertyPrices] = useState<Record<string, number>>({});
    const [unitCapacities, setUnitCapacities] = useState<Record<string, { bedrooms: number; beds: number }>>({});

    const searchValues = useMemo(() => ({
        city: searchParams.get("city") ?? "",
        checkIn: parseSearchDate(searchParams.get("checkIn")),
        checkOut: parseSearchDate(searchParams.get("checkOut")),
        guests: searchParams.get("guests") ?? "",
    }), [searchParams]);

    const hasCompleteDateRange =
        !!searchValues.checkIn &&
        !!searchValues.checkOut &&
        searchValues.checkIn < searchValues.checkOut;

    const urlGuests = parseGuests(searchValues.guests);
    const effectiveGuests = Math.max(urlGuests ?? 0, debouncedFilters.guests > 1 ? debouncedFilters.guests : 0);

    const citySuggestions = useMemo(() => getCitySuggestions(properties), [properties]);

    const propertyDetailsSearch = useMemo(() => {
        const params = new URLSearchParams();
        const checkIn = searchParams.get("checkIn");
        const checkOut = searchParams.get("checkOut");
        const guests = searchParams.get("guests");
        if (checkIn) params.set("checkIn", checkIn);
        if (checkOut) params.set("checkOut", checkOut);
        if (guests) params.set("guests", guests);
        const s = params.toString();
        return s ? `?${s}` : "";
    }, [searchParams]);

    useEffect(() => {
        getProperties(undefined, undefined, undefined, undefined, debouncedFilters.selectedAmenities)
            .then((data: Property[]) => {
                if (!Array.isArray(data)) throw new Error(PROPERTIES_LOAD_ERROR);
                setProperties(data);
                setPropertiesError(null);
            })
            .catch(() => {
                setProperties([]);
                setFilteredProperties([]);
                setPropertiesError(PROPERTIES_LOAD_ERROR);
            })
            .finally(() => setIsLoading(false));
    }, [debouncedFilters.selectedAmenities]);

    useEffect(() => {
        if (filteredProperties.length === 0) return;
        filteredProperties.forEach((property) => {
            getUnits(property.id, currency)
                .then((units: Unit[]) => {
                    if (units && units.length > 0) {
                        const prices = units
                            .map((u) =>
                                u.convertedPricePerNight && currency !== "PLN"
                                    ? u.convertedPricePerNight
                                    : u.pricePerNight
                            )
                            .filter((p) => p !== undefined && p !== null);
                        if (prices.length > 0) {
                            setPropertyPrices((prev) => ({
                                ...prev,
                                [property.id]: Math.min(...prices),
                            }));
                        }
                        const maxBedrooms = Math.max(...units.map((u) => u.bedrooms ?? 0));
                        const maxBeds = Math.max(...units.map((u) => u.beds ?? 0));
                        setUnitCapacities((prev) => ({
                            ...prev,
                            [property.id]: { bedrooms: maxBedrooms, beds: maxBeds },
                        }));
                    }
                })
                .catch((err) => console.error("Error loading price for card:", err));
        });
    }, [filteredProperties, currency]);

    useEffect(() => {
        if (selectedProperty && showMap) {
            document
                .getElementById(`property-card-${selectedProperty.id}`)
                ?.scrollIntoView({ behavior: "smooth", block: "nearest" });
        }
    }, [selectedProperty, showMap]);

    useEffect(() => {
        let cancelled = false;

        async function filterProperties() {
            const cityFiltered = properties.filter((p) =>
                propertyMatchesCity(p, searchValues.city)
            );

            if (
                !effectiveGuests &&
                (!hasCompleteDateRange || !searchValues.checkIn || !searchValues.checkOut)
            ) {
                setIsFilteringAvailability(false);
                setFilterError(null);
                setFilteredProperties(cityFiltered);
                return;
            }

            setIsFilteringAvailability(true);
            try {
                const checkIn = searchValues.checkIn ? formatSearchDate(searchValues.checkIn) : null;
                const checkOut = searchValues.checkOut ? formatSearchDate(searchValues.checkOut) : null;

                const results = await Promise.all(
                    cityFiltered.map(async (property) => {
                        let units: Unit[];
                        try {
                            units = await getUnits(property.id);
                            if (!Array.isArray(units)) throw new Error(UNITS_FILTER_ERROR);
                        } catch {
                            throw new Error(UNITS_FILTER_ERROR);
                        }

                        const matching = effectiveGuests
                            ? units.filter((u) => u.capacity >= effectiveGuests)
                            : units;

                        if (matching.length === 0) return null;

                        if (!hasCompleteDateRange || !checkIn || !checkOut) return property;

                        const available = await Promise.all(
                            matching.map((unit) =>
                                checkAvailability(unit.id, checkIn, checkOut)
                                    .then((a: AvailabilityResponse) => {
                                        if (typeof a.available !== "boolean")
                                            throw new Error(AVAILABILITY_FILTER_ERROR);
                                        return a.available;
                                    })
                                    .catch(() => { throw new Error(AVAILABILITY_FILTER_ERROR); })
                            )
                        );

                        return available.some(Boolean) ? property : null;
                    })
                );

                if (!cancelled) {
                    setFilterError(null);
                    setFilteredProperties(results.filter((p): p is Property => p !== null));
                }
            } catch (err: unknown) {
                if (!cancelled) {
                    const msg = err instanceof Error ? err.message : UNITS_FILTER_ERROR;
                    setFilterError(
                        msg === AVAILABILITY_FILTER_ERROR
                            ? AVAILABILITY_FILTER_ERROR
                            : UNITS_FILTER_ERROR
                    );
                    setFilteredProperties([]);
                }
            } finally {
                if (!cancelled) setIsFilteringAvailability(false);
            }
        }

        filterProperties();
        return () => { cancelled = true; };
    }, [
        properties,
        searchValues.city,
        searchValues.checkIn,
        searchValues.checkOut,
        searchValues.guests,
        hasCompleteDateRange,
        effectiveGuests,
    ]);

    const displayProperties = useMemo(() => {
        let result = filteredProperties;

        if (debouncedFilters.propertyTypes.length > 0) {
            result = result.filter((p) =>
                p.propertyType !== undefined &&
                debouncedFilters.propertyTypes.includes(p.propertyType)
            );
        }

        const min = debouncedFilters.minPrice !== "" ? Number(debouncedFilters.minPrice) : null;
        const max = debouncedFilters.maxPrice !== "" ? Number(debouncedFilters.maxPrice) : null;
        if (min !== null || max !== null) {
            result = result.filter((p) => {
                const price = propertyPrices[p.id];
                if (price === undefined) return true;
                if (min !== null && price < min) return false;
                if (max !== null && price > max) return false;
                return true;
            });
        }

        if (debouncedFilters.bedrooms > 0) {
            result = result.filter((p) => {
                const cap = unitCapacities[p.id];
                if (!cap) return true;
                return cap.bedrooms >= debouncedFilters.bedrooms;
            });
        }

        if (debouncedFilters.beds > 0) {
            result = result.filter((p) => {
                const cap = unitCapacities[p.id];
                if (!cap) return true;
                return cap.beds >= debouncedFilters.beds;
            });
        }

        return result;
    }, [filteredProperties, propertyPrices, unitCapacities, debouncedFilters]);

    const visibleProperties = useMemo(() => {
        if (!mapBounds) return displayProperties;
        return displayProperties.filter((p) => {
            if (p.latitude == null || p.longitude == null) return false;
            const lat = Number(p.latitude);
            const lng = Number(p.longitude);
            return (
                lat >= mapBounds.minLat &&
                lat <= mapBounds.maxLat &&
                lng >= mapBounds.minLng &&
                lng <= mapBounds.maxLng
            );
        });
    }, [displayProperties, mapBounds]);

    const activeFilters = useMemo((): ActiveFilter[] => {
        const result: ActiveFilter[] = [];

        filters.propertyTypes.forEach((type) =>
            result.push({
                id: `type-${type}`,
                label: type,
                onRemove: () =>
                    setFilters((f) => ({
                        ...f,
                        propertyTypes: f.propertyTypes.filter((t) => t !== type),
                    })),
            })
        );

        if (filters.guests > 1)
            result.push({
                id: "guests",
                label: `${filters.guests}+ guests`,
                onRemove: () => setFilters((f) => ({ ...f, guests: 1 })),
            });

        if (filters.bedrooms > 0)
            result.push({
                id: "bedrooms",
                label: `${filters.bedrooms}+ bedrooms`,
                onRemove: () => setFilters((f) => ({ ...f, bedrooms: 0 })),
            });

        if (filters.beds > 0)
            result.push({
                id: "beds",
                label: `${filters.beds}+ beds`,
                onRemove: () => setFilters((f) => ({ ...f, beds: 0 })),
            });

        if (filters.minPrice !== "")
            result.push({
                id: "minPrice",
                label: `Min ${filters.minPrice} ${currency}`,
                onRemove: () => setFilters((f) => ({ ...f, minPrice: "" })),
            });

        if (filters.maxPrice !== "")
            result.push({
                id: "maxPrice",
                label: `Max ${filters.maxPrice} ${currency}`,
                onRemove: () => setFilters((f) => ({ ...f, maxPrice: "" })),
            });

        filters.selectedAmenities.forEach((amenity) =>
            result.push({
                id: `amenity-${amenity}`,
                label: amenity,
                onRemove: () =>
                    setFilters((f) => ({
                        ...f,
                        selectedAmenities: f.selectedAmenities.filter((a) => a !== amenity),
                    })),
            })
        );

        return result;
    }, [filters, currency]);

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

    const activeFilterCount =
        filters.selectedAmenities.length +
        filters.propertyTypes.length +
        (filters.minPrice !== "" ? 1 : 0) +
        (filters.maxPrice !== "" ? 1 : 0) +
        (filters.guests > 1 ? 1 : 0) +
        (filters.bedrooms > 0 ? 1 : 0) +
        (filters.beds > 0 ? 1 : 0);

    return (
        <div className="min-h-screen bg-[#F9F8F7] text-[#1A1A1A]">
            <div className="bg-[#F9F8F7] px-4 md:px-8 pt-6 pb-4 max-w-[1440px] mx-auto">
                <div className="flex justify-between items-start mb-5">
                    <div>
                        <h1 className="text-3xl font-bold text-[#1A1A1A] tracking-tight">Properties</h1>
                        <p className="text-sm text-[#7A7A7A] mt-1">
                            Explore our curated selection of properties.
                        </p>
                    </div>
                    <div className="flex items-center gap-2 shrink-0">
                        <button
                            onClick={() => setIsMobileSidebarOpen(true)}
                            className="md:hidden relative flex items-center gap-2 bg-white border border-gray-200 text-[#1A1A1A] px-4 py-2.5 rounded-xl shadow-sm font-semibold text-sm hover:border-[#42211D]/40 cursor-pointer transition-all"
                        >
                            <SlidersHorizontal className="w-4 h-4" strokeWidth={2.5} />
                            Filters
                            {activeFilterCount > 0 && (
                                <span className="absolute -top-1.5 -right-1.5 bg-[#42211D] text-white text-[10px] font-bold w-5 h-5 rounded-full flex items-center justify-center">
                                    {activeFilterCount}
                                </span>
                            )}
                        </button>

                        <button
                            onClick={toggleMap}
                            className="hidden md:flex items-center gap-2 bg-[#42211D] hover:bg-[#5c2e29] text-white px-5 py-2.5 rounded-xl shadow-sm font-semibold text-sm transition-all duration-200 cursor-pointer"
                        >
                            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                {showMap ? (
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                                          d="M4 6h16M4 12h16M4 18h16" />
                                ) : (
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                                          d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" />
                                )}
                            </svg>
                            {showMap ? "Hide map" : "Show map"}
                        </button>
                    </div>
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
                <div className="px-4 md:px-8 max-w-[1440px] mx-auto mb-4">
                    <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-5 text-center text-[#7A7A7A] font-medium animate-pulse">
                        {isLoading ? "Loading properties…" : "Checking availability…"}
                    </div>
                </div>
            )}

            {(propertiesError || filterError) && (
                <div className="px-4 md:px-8 max-w-[1440px] mx-auto mb-4">
                    <div className="bg-red-50 border border-red-200 rounded-xl p-5 text-red-700 font-semibold">
                        {propertiesError || filterError}
                    </div>
                </div>
            )}

            {!isLoading && !propertiesError && (
                <div className="px-4 md:px-8 pb-10 max-w-[1440px] mx-auto flex gap-6 items-start">
                    <FilterSidebar
                        filters={filters}
                        onFiltersChange={setFilters}
                        isOpen={isMobileSidebarOpen}
                        onClose={() => setIsMobileSidebarOpen(false)}
                        currency={currency}
                    />

                    <div className="flex-1 min-w-0 flex flex-col gap-4">
                        {activeFilters.length > 0 && (
                            <div className="flex items-center gap-2 overflow-x-auto pb-0.5 -mx-1 px-1 scrollbar-hide">
                                {activeFilters.map((f) => (
                                    <FilterBadge
                                        key={f.id}
                                        label={f.label}
                                        onRemove={f.onRemove}
                                    />
                                ))}
                                {activeFilters.length > 1 && (
                                    <button
                                        onClick={() => setFilters(EMPTY_FILTERS)}
                                        className="shrink-0 text-xs font-semibold text-[#7A7A7A] hover:text-[#42211D] border border-dashed border-gray-300 rounded-full px-3 py-1 transition-colors cursor-pointer whitespace-nowrap"
                                    >
                                        Clear all
                                    </button>
                                )}
                            </div>
                        )}

                        <div className="flex gap-6">
                            <div
                                className={`flex-1 min-w-0 ${
                                    showMap ? "md:max-w-[340px] lg:max-w-[400px] xl:max-w-[440px]" : ""
                                }`}
                            >
                                {displayProperties.length === 0 ? (
                                    <div className="bg-white border border-gray-200 rounded-xl shadow-sm p-10 text-center">
                                        <div className="w-12 h-12 rounded-full bg-[#42211D]/10 flex items-center justify-center mx-auto mb-4">
                                            <svg className="w-6 h-6 text-[#42211D]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                                                      d="M21 21l-4.35-4.35M17 11A6 6 0 105 11a6 6 0 0012 0z" />
                                            </svg>
                                        </div>
                                        <h2 className="text-lg font-bold text-[#1A1A1A]">No properties found</h2>
                                        <p className="text-sm text-[#7A7A7A] mt-1.5">
                                            Try adjusting your search or clearing filters.
                                        </p>
                                        {activeFilterCount > 0 && (
                                            <button
                                                onClick={() => setFilters(EMPTY_FILTERS)}
                                                className="mt-4 text-sm font-semibold text-[#42211D] hover:underline cursor-pointer"
                                            >
                                                Clear all filters
                                            </button>
                                        )}
                                    </div>
                                ) : showMap && visibleProperties.length === 0 ? (
                                    <div className="bg-white border border-gray-200 rounded-xl shadow-sm p-10 text-center">
                                        <h2 className="text-lg font-bold text-[#1A1A1A]">No properties in this area</h2>
                                        <p className="text-sm text-[#7A7A7A] mt-1.5">Pan or zoom out on the map.</p>
                                    </div>
                                ) : (
                                    <div
                                        className={`${
                                            showMap
                                                ? "flex flex-col gap-4 overflow-y-auto pr-1"
                                                : "grid gap-5 grid-cols-1 sm:grid-cols-2 xl:grid-cols-3"
                                        }`}
                                        style={showMap ? { maxHeight: "calc(100vh - 240px)" } : undefined}
                                    >
                                        {(showMap ? visibleProperties : displayProperties).map((p) => {
                                            const isSelected = selectedProperty?.id === p.id;
                                            return (
                                                <div
                                                    key={p.id}
                                                    id={`property-card-${p.id}`}
                                                    onMouseEnter={() => showMap && setSelectedProperty(p)}
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
                                                            className={`bg-white border rounded-xl p-4 hover:shadow-md transition-all duration-300 hover:-translate-y-0.5 ${
                                                                isSelected
                                                                    ? "border-[#42211D] ring-2 ring-inset ring-[#42211D]/20 shadow-md"
                                                                    : "border-gray-200 shadow-sm"
                                                            }`}
                                                        >
                                                            <div className="relative aspect-[4/3] w-full overflow-hidden rounded-lg bg-gray-100">
                                                                <img
                                                                    src={p.imageUrl}
                                                                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                                                                    alt={p.title}
                                                                />
                                                            </div>
                                                            <div className="flex items-baseline justify-between mt-3">
                                                                <div className="min-w-0 flex-1">
                                                                    <h2 className="text-base font-bold text-[#1A1A1A] tracking-tight group-hover:text-[#42211D] transition-colors line-clamp-1">
                                                                        {p.title}
                                                                    </h2>
                                                                    <p className="text-xs text-[#7A7A7A] mt-0.5 font-medium">
                                                                        {p.city}
                                                                    </p>
                                                                    {p.amenities && p.amenities.length > 0 && (
                                                                        <div className="flex flex-wrap gap-1 mt-1.5">
                                                                            {p.amenities.slice(0, 3).map((a, idx) => (
                                                                                <span key={idx} className="text-[10px] bg-[#f0eded] text-[#42211D] px-1.5 py-0.5 rounded font-medium line-clamp-1 break-all">
                                                                                    {a}
                                                                                </span>
                                                                            ))}
                                                                            {p.amenities.length > 3 && (
                                                                                <span className="text-[10px] bg-[#f0eded] text-[#42211D] px-1.5 py-0.5 rounded font-medium">
                                                                                    +{p.amenities.length - 3}
                                                                                </span>
                                                                            )}
                                                                        </div>
                                                                    )}
                                                                </div>
                                                                <div className="text-right shrink-0 ml-4">
                                                                    <span className="text-[9px] text-[#7A7A7A] block font-bold uppercase tracking-wider leading-none mb-0.5">
                                                                        From
                                                                    </span>
                                                                    <span className="font-black text-base text-[#42211D]">
                                                                        {propertyPrices[p.id] !== undefined ? (
                                                                            `${Math.round(propertyPrices[p.id])} ${currency}`
                                                                        ) : (
                                                                            <span className="text-xs font-normal text-[#7A7A7A]">…</span>
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
                                    className="hidden md:block flex-1 sticky top-6"
                                    style={{ height: "calc(100vh - 200px)" }}
                                >
                                    <div className="w-full h-full rounded-2xl overflow-hidden border border-gray-200 shadow-sm">
                                        <PropertyMap
                                            properties={displayProperties}
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
                    </div>
                </div>
            )}

            {!isLoading && !propertiesError && displayProperties.length > 0 && (
                <button
                    onClick={toggleMap}
                    className="fixed bottom-6 left-1/2 -translate-x-1/2 z-40 bg-[#42211D] hover:bg-[#5c2e29] text-white px-6 py-3 rounded-full shadow-lg font-bold flex items-center gap-2 md:hidden transition-all duration-200 cursor-pointer"
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
