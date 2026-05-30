import { Star, MapPin } from 'lucide-react';
import { Link } from 'react-router-dom';

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
}

interface TopPropertiesProps {
    properties: Property[];
}

export default function TopPropertiesSection({ properties = [] }: TopPropertiesProps) {
  const displayProperties = properties && properties.length > 0 
    ? properties.slice(0, 4)
    : [];

  if (displayProperties.length === 0) {
      return null;
  }

  return (
    <section className="bg-card py-32 px-4 md:px-8 lg:px-16 border-t border-[#DACDCA]/30">
      <div className="max-w-7xl mx-auto">
        <div className="flex flex-col md:flex-row justify-between items-end mb-16">
          <div>
            <span className="block text-sm font-bold uppercase tracking-widest mb-3" style={{ color: 'rgb(var(--color-burgundy))' }}>Top Book Now</span>
            <h2 className="text-3xl md:text-5xl font-bold text-title">Featured Properties</h2>
          </div>
          <Link to="/catalog" className="mt-6 md:mt-0 text-title font-semibold hover:text-[rgb(var(--color-burgundy))] transition-colors text-lg">
            View All Properties &rarr;
          </Link>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
          {displayProperties.map((property) => (
            <Link to={`/property/${property.id}`} key={property.id} className="group cursor-pointer block">

              <div className="relative aspect-[4/3] rounded-3xl overflow-hidden mb-6 shadow-md">
                <img 
                  src={property.imageUrl || 'https://images.unsplash.com/photo-1510798831971-661eb04b3739?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80'} 
                  alt={property.title || property.name || "Property image"} 
                  className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700"
                />
                

                <div className="absolute top-4 right-4 bg-white/95 backdrop-blur-md px-3 py-1.5 rounded-full flex items-center gap-1.5 shadow-lg">
                  <Star size={16} className="text-yellow-500 fill-yellow-500" />
                  <span className="text-sm font-bold text-[#1A1A1A]">{property.rating || '4.8'}</span>
                </div>
              </div>


              <div className="flex justify-between items-start gap-4 px-2">
                <div className="flex-1">
                  <h3 className="font-bold text-title text-xl mb-2 group-hover:text-[rgb(var(--color-burgundy))] transition-colors line-clamp-1">
                    {property.title || property.name || 'Cozy Accommodation'}
                  </h3>
                  <div className="flex items-center gap-1.5 text-details font-medium">
                    <MapPin size={16} />
                    <span className="text-base">{property.location || property.city || 'Poland'}</span>
                  </div>
                </div>
                <div className="text-right whitespace-nowrap">
                  <span className="font-black text-xl text-title">{property.price || property.pricePerNight || 250} PLN</span>
                  <span className="text-sm text-details block font-medium">/night</span>
                </div>
              </div>
            </Link>
          ))}
        </div>
      </div>
    </section>
  );
}
