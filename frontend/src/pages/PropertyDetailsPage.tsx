import { useEffect, useState } from "react";
import { getProperty, getUnits, getPropertyImages } from "../api/propertyApi";
import { useParams } from "react-router-dom";
import type { Unit } from "../types/property";

export default function PropertyDetailsPage() {

    const { id } = useParams();

    const [property, setProperty] = useState<any>(null);
    const [units, setUnits] = useState<Unit[]>([]);
    const [images, setImages] = useState<string[]>([]);
    const [mainImage, setMainImage] = useState("");

    useEffect(() => {
        if (id) {

            getProperty(id).then(function (data) {
                setProperty(data);
            });

            getUnits(id).then(function (data) {
                setUnits(data);
            });

            getPropertyImages(id).then(function (data) {
                setImages(data);

                if (data.length > 0) {
                    setMainImage(data[0]);
                }
            });
        }
    }, [id]);

    if (!property) {
        return <div className="p-6">Loading...</div>;
    }

    return (
        <div className="max-w-5xl mx-auto p-6">

            <img
                src={mainImage ? mainImage : property.imageUrl}
                className="w-full aspect-[16/9] object-cover rounded"
            />

            <div className="flex gap-2 mt-2">
                {images.map(function (img, i) {
                    return (
                        <img
                            key={i}
                            src={img}
                            onClick={function () {
                                setMainImage(img);
                            }}
                            className="w-20 aspect-square object-cover rounded cursor-pointer border"
                        />
                    );
                })}
            </div>

            <h1 className="text-3xl font-bold mt-4">{property.title}</h1>
            <p className="text-details">{property.location}</p>
            <h2 className="mt-6 text-xl font-bold">Units</h2>

            {units.map(function (u) {
                return (
                    <div
                        key={u.id}
                        className="bg-card rounded-xl mt-4 overflow-hidden"
                    >

                        {u.imageUrl && (
                            <img
                                src={u.imageUrl}
                                className="w-full aspect-[16/9] object-cover rounded"
                            />
                        )}

                        <h3 className="font-bold mt-2">{u.name}</h3>
                        <p className="text-details">{u.description}</p>

                        <p className="mt-2">{u.pricePerNight} zł</p>
                        <p>
                            {u.capacity} {u.capacity === 1 ? "person" : "people"}
                        </p>
                    </div>
                );
            })}

        </div>
    );
}