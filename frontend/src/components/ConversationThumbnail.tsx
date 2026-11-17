import { useEffect, useState } from "react";
import { House } from "lucide-react";
import { getProperty, getUnit } from "../api/propertyApi";

export default function ConversationThumbnail({ unitId }: { unitId: string }) {
    const [photos, setPhotos] = useState<string[]>([]);
    const [failed, setFailed] = useState<string[]>([]);

    useEffect(() => {
        let active = true;
        async function loadPhotos() {
            const unit = await getUnit(unitId).catch(() => null);
            if (!active || !unit) return;
            const property = unit.propertyId ? await getProperty(unit.propertyId).catch(() => null) : null;
            if (!active) return;
            setPhotos([property?.imageUrl, unit.imageUrl].filter(
                (url): url is string => typeof url === "string" && Boolean(url.trim()),
            ));
        }
        void loadPhotos();
        return () => { active = false; };
    }, [unitId]);

    const photo = photos.find(url => !failed.includes(url));
    return (
        <span className="grid size-14 shrink-0 place-items-center overflow-hidden rounded-xl border border-brand-accent bg-brand-bg text-brand-primary md:size-[4.5rem]" aria-hidden="true">
            {photo ? <img key={photo} src={photo} alt="" className="size-full object-cover" onError={() => setFailed(previous => [...previous, photo])} />
                : <House size={24} />}
        </span>
    );
}
