import { useState, useEffect, useMemo } from 'react';
import { MapPin } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useCurrency } from '../../contexts/CurrencyContext';
import { getUnits } from '../../api/propertyApi';
import type { Unit } from '../../types/property';

interface Property {
    id: string;
    title?: string;
    name?: string;
    location?: string;
    city?: string;
    price?: number;
    pricePerNight?: number;
    rating?: number;
    imageUrl: string;
    reviewsCount?: number;
}

interface TopPropertiesProps {
    properties: Property[];
}

const FALLBACK_RATES: Record<string, number> = {
  PLN: 1.0,
  EUR: 4.3,
  USD: 4.0
};

export default function TopPropertiesSection({ properties = [] }: TopPropertiesProps) {
  const displayProperties = useMemo(() => {
    return properties && properties.length > 0 
      ? properties.slice(0, 4)
      : [];
  }, [properties]);

  const { currency } = useCurrency();
  const [prices, setPrices] = useState<Record<string, { price: number; displayCurrency: string }>>({});

  useEffect(() => {
    if (!displayProperties || displayProperties.length === 0) return;

    displayProperties.forEach((property) => {
      getUnits(property.id, currency)
        .then((units: Unit[]) => {
          if (units && units.length > 0) {
            // Find the minimum price or use the first unit's price
            const unit = units[0];
            const priceVal = unit.convertedPricePerNight && currency !== 'PLN'
              ? unit.convertedPricePerNight
              : unit.pricePerNight;
            const displayCurr = unit.currencyInfo?.displayCurrency || currency;
            setPrices(prev => ({
              ...prev,
              [property.id]: { price: priceVal, displayCurrency: displayCurr }
            }));
          }
        })
        .catch((err) => {
          console.error("Error fetching units for property " + property.id, err);
        });
    });
  }, [displayProperties, currency]);

  if (displayProperties.length === 0) {
      return null;
  }

  return (
    <section className="bg-white py-24 px-4 md:px-8 lg:px-16 border-t border-[#DACDCA]/30">
      <div className="max-w-7xl mx-auto">
        <div className="flex flex-col md:flex-row justify-between items-end mb-16">
          <div className="flex flex-col items-start text-left">
            <span className="block text-xs md:text-sm font-bold uppercase tracking-widest mb-2.5" style={{ color: 'rgb(var(--color-burgundy))' }}>TOP BOOK NOW</span>
            <h2 className="text-3xl md:text-5xl font-bold text-title">Featured Properties</h2>
          </div>
          <Link to="/catalog" className="mt-6 md:mt-0 text-title font-semibold hover:text-[rgb(var(--color-burgundy))] transition-colors text-lg">
            View All Properties &rarr;
          </Link>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
          {displayProperties.map((property) => {
            const priceInfo = prices[property.id];
            const rawPrice = property.price || property.pricePerNight || 250;
            const displayPrice = priceInfo 
              ? priceInfo.price 
              : (rawPrice / (FALLBACK_RATES[currency] || 1.0));
            const displayCurr = priceInfo
              ? priceInfo.displayCurrency
              : currency;

            return (
              <Link 
                to={`/property/${property.id}`} 
                key={property.id} 
                className="group block bg-white rounded-[32px] p-3 border border-gray-100/50 shadow-md transition-all duration-300 hover:-translate-y-1.5 hover:shadow-xl cursor-pointer"
              >

                <div className="relative aspect-[4/3] rounded-2xl overflow-hidden mb-5 shadow-sm">
                  <img 
                    src={property.imageUrl || 'https://images.unsplash.com/photo-1510798831971-661eb04b3739?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80'} 
                    alt={property.title || property.name || "Property image"} 
                    className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700"
                  />
                </div>

                <div className="flex justify-between items-start gap-3 px-1.5">
                  <div className="flex-1">
                    <h3 className="font-bold text-title text-lg mb-1.5 group-hover:text-[rgb(var(--color-burgundy))] transition-colors line-clamp-1">
                      {property.title || property.name || 'Cozy Accommodation'}
                    </h3>
                    <div className="flex items-center gap-1.5 text-details font-medium">
                      <MapPin size={15} />
                      <span className="text-sm">{property.location || property.city || 'Poland'}</span>
                    </div>
                  </div>
                  <div className="text-right whitespace-nowrap">
                    <span className="font-black text-lg text-[rgb(var(--color-burgundy))]">
                      {Math.round(displayPrice)}{' '}
                      <span className="font-bold text-[10px] ml-0.5 text-[rgb(var(--color-burgundy))]/85">{displayCurr}</span>
                    </span>
                    <span className="text-[10px] text-details block font-semibold">/night</span>
                  </div>
                </div>
              </Link>
            );
          })}
        </div>

        <div className="flex justify-center gap-2 mt-12">
          <span className="w-8 h-2 rounded-full bg-[rgb(var(--color-burgundy))] transition-all duration-300" />
          <span className="w-2 h-2 rounded-full bg-details/30 hover:bg-details/60 cursor-pointer transition-colors" />
          <span className="w-2 h-2 rounded-full bg-details/30 hover:bg-details/60 cursor-pointer transition-colors" />
        </div>
      </div>
    </section>
  );
}
