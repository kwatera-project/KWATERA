import { useCallback, useEffect, useState, useRef } from "react";
import { MapContainer, TileLayer, Marker, Popup, useMap, useMapEvents } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { getUnits } from "../api/propertyApi";
import type { Property, Unit } from "../types/property";
import { useCurrency } from "../contexts/CurrencyContext";
import { Link } from "react-router-dom";
import {useTranslation} from "react-i18next"

const createCustomMarkerIcon = () => {
  return L.divIcon({
    html: `
      <div class="relative flex items-center justify-center">
        <svg class="w-9 h-9 text-[#42211D] filter drop-shadow-md" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
        </svg>
        <div class="absolute w-2.5 h-2.5 bg-white rounded-full top-[10px]"></div>
      </div>
    `,
    className: "custom-leaflet-marker",
    iconSize: [36, 36],
    iconAnchor: [18, 36],
    popupAnchor: [0, -36],
  });
};

interface PropertyPopupContentProps {
  property: Property;
  propertyDetailsSearch?: string;
}

function PropertyPopupContent({ property, propertyDetailsSearch = "" }: PropertyPopupContentProps) {
  const [minPrice, setMinPrice] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const { currency } = useCurrency();
  const {t} = useTranslation();


    useEffect(() => {
    let isMounted = true;
    getUnits(property.id, currency)
      .then((units: Unit[]) => {
        if (isMounted && units && units.length > 0) {
          const prices = units
            .map((u) => (u.convertedPricePerNight && currency !== "PLN" ? u.convertedPricePerNight : u.pricePerNight))
            .filter((p) => p !== undefined && p !== null);
          if (prices.length > 0) {
            setMinPrice(Math.min(...prices));
          }
        }
      })
      .catch((err) => {
        console.error("Failed to fetch units for popup:", err);
      })
      .finally(() => {
        if (isMounted) {
          setLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [property.id, currency]);

  return (
    <Link to={`/property/${property.id}${propertyDetailsSearch}`} className="group block no-underline text-[#1A1A1A]">
      <div className="w-48 bg-white text-[#1A1A1A]">
        <div className="relative h-28 w-full overflow-hidden rounded-t-lg bg-gray-100">
          {property.imageUrl ? (
            <img
              src={property.imageUrl}
              alt={property.title}
              className="h-full w-full object-cover group-hover:scale-105 transition-transform duration-500"
            />
          ) : (
            <div className="flex h-full w-full items-center justify-center bg-gray-200 text-[#7A7A7A] text-xs">
                {t('propertyMap.noImage')}
            </div>
          )}
        </div>
        <div className="p-3">
          <h4 className="font-bold text-sm text-[#1A1A1A] line-clamp-1 mb-0.5 group-hover:text-[#42211D] transition-colors">
            {property.title}
          </h4>
          <p className="text-[11px] text-[#7A7A7A] font-medium mb-2">{property.city}</p>
          <div className="flex items-baseline justify-between border-t border-gray-100 pt-2 mt-1">
            <span className="text-[10px] text-[#7A7A7A] uppercase font-bold tracking-wider">{t('propertyMap.from')}</span>
            <span className="font-black text-sm text-[#42211D]">
              {loading ? (
                <span className="animate-pulse">...</span>
              ) : minPrice !== null ? (
                `${Math.round(minPrice)} ${currency}`
              ) : (
                "N/A"
              )}
              <span className="text-[10px] text-[#7A7A7A] font-normal ml-0.5">{t('propertyMap.perNight')}</span>
            </span>
          </div>
        </div>
      </div>
    </Link>
  );
}

interface PropertyMarkerProps {
  property: Property;
  onSelect: (property: Property) => void;
  onDeselect: () => void;
  propertyDetailsSearch: string;
  shouldOpenPopup: boolean;
  onPopupOpened: () => void;
}

function PropertyMarker({
  property,
  onSelect,
  onDeselect,
  propertyDetailsSearch,
  shouldOpenPopup,
  onPopupOpened,
}: PropertyMarkerProps) {
  const markerRef = useRef<L.Marker | null>(null);

  useEffect(() => {
    if (shouldOpenPopup && markerRef.current) {
      markerRef.current.openPopup();
      onPopupOpened();
    }
  }, [shouldOpenPopup, onPopupOpened]);

  return (
    <Marker
      ref={markerRef}
      position={[Number(property.latitude), Number(property.longitude)]}
      icon={createCustomMarkerIcon()}
      eventHandlers={{
        click: () => {
          onSelect(property);
        },
        popupclose: () => {
          onDeselect();
        },
      }}
    >
      <Popup className="custom-popup">
        <PropertyPopupContent property={property} propertyDetailsSearch={propertyDetailsSearch} />
      </Popup>
    </Marker>
  );
}

interface MapEventsHandlerProps {
  onBoundsChange?: (bounds: { minLat: number; maxLat: number; minLng: number; maxLng: number }) => void;
}

function MapEventsHandler({ onBoundsChange }: MapEventsHandlerProps) {
  const map = useMapEvents({
    moveend: () => handleBoundsUpdate(),
    zoomend: () => handleBoundsUpdate(),
  });

  const handleBoundsUpdate = useCallback(() => {
    if (!onBoundsChange) return;
    const bounds = map.getBounds();
    const southWest = bounds.getSouthWest();
    const northEast = bounds.getNorthEast();
    onBoundsChange({
      minLat: southWest.lat,
      maxLat: northEast.lat,
      minLng: southWest.lng,
      maxLng: northEast.lng,
    });
  }, [map, onBoundsChange]);

  useEffect(() => {
    const timer = setTimeout(() => {
      handleBoundsUpdate();
    }, 100);
    return () => clearTimeout(timer);
  }, [handleBoundsUpdate]);

  return null;
}

interface FitBoundsProps {
  properties: Property[];
}

function FitBounds({ properties }: FitBoundsProps) {
  const map = useMap();

  useEffect(() => {
    if (properties.length === 0) return;

    const points = properties
      .filter(
        (p) =>
          p.latitude !== undefined &&
          p.longitude !== undefined &&
          p.latitude !== null &&
          p.longitude !== null
      )
      .map((p) => [Number(p.latitude), Number(p.longitude)] as [number, number]);

    if (points.length > 0) {
      const bounds = L.latLngBounds(points);
      map.fitBounds(bounds, { padding: [50, 50], maxZoom: 14 });
    }
  }, [properties, map]);

  return null;
}

function InvalidateMapSize() {
  const map = useMap();
  useEffect(() => {
    const timer = setTimeout(() => {
      map.invalidateSize();
    }, 200);
    return () => clearTimeout(timer);
  }, [map]);
  return null;
}

interface PropertyMapProps {
  properties: Property[];
  onBoundsChange?: (bounds: { minLat: number; maxLat: number; minLng: number; maxLng: number }) => void;
  onPropertySelect?: (property: Property | null) => void;
  propertyDetailsSearch?: string;
  selectedProperty?: Property | null;
  openPopupPropertyId?: string | null;
  setOpenPopupPropertyId?: (id: string | null) => void;
}

export default function PropertyMap({
  properties,
  onBoundsChange,
  onPropertySelect,
  propertyDetailsSearch = "",
  selectedProperty,
  openPopupPropertyId,
  setOpenPopupPropertyId,
}: PropertyMapProps) {
  const defaultCenter: [number, number] = [52.2297, 21.0122];
  const defaultZoom = 6;

  return (
    <MapContainer
      center={defaultCenter}
      zoom={defaultZoom}
      className="w-full h-full z-0"
      scrollWheelZoom={true}
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'
        url="https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png"
      />
      <InvalidateMapSize />
      <MapEventsHandler onBoundsChange={onBoundsChange} />
      <FitBounds properties={properties} />

      {properties
        .filter(
          (p) =>
            p.latitude !== undefined &&
            p.longitude !== undefined &&
            p.latitude !== null &&
            p.longitude !== null
        )
        .map((property) => (
          <PropertyMarker
            key={property.id}
            property={property}
            onSelect={(p) => onPropertySelect?.(p)}
            onDeselect={() => {
              if (onPropertySelect && selectedProperty?.id === property.id) {
                onPropertySelect(null);
              }
            }}
            propertyDetailsSearch={propertyDetailsSearch}
            shouldOpenPopup={openPopupPropertyId === property.id}
            onPopupOpened={() => setOpenPopupPropertyId?.(null)}
          />
        ))}
    </MapContainer>
  );
}
