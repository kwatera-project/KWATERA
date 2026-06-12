import { useId, useState } from "react";
import type { FocusEvent } from "react";

type WaterRateTooltipProps = {
    text: string;
    value: string;
    label?: string;
    iconClassName?: string;
    panelClassName?: string;
    labelClassName?: string;
    valueClassName?: string;
};

export default function WaterRateTooltip({
    text,
    value,
    label = "Rate",
    iconClassName = "border-brand-accent text-brand-muted bg-white",
    panelClassName = "border-brand-accent bg-white text-brand-main shadow-sm",
    labelClassName = "text-brand-muted font-medium",
    valueClassName = "font-semibold text-brand-main",
}: WaterRateTooltipProps) {
    const tooltipId = useId();
    const [isHovered, setIsHovered] = useState(false);
    const [isFocused, setIsFocused] = useState(false);
    const [isPinned, setIsPinned] = useState(false);
    const [isHoverSuppressed, setIsHoverSuppressed] = useState(false);
    const isOpen = isHovered || isFocused || isPinned;

    const handleBlur = (event: FocusEvent<HTMLDivElement>) => {
        if (!event.currentTarget.contains(event.relatedTarget)) {
            setIsFocused(false);
            setIsPinned(false);
        }
    };

    return (
        <div
            className="space-y-2 overflow-visible"
            onMouseEnter={() => {
                if (!isHoverSuppressed) setIsHovered(true);
            }}
            onMouseLeave={() => {
                setIsHovered(false);
                setIsPinned(false);
                setIsHoverSuppressed(false);
            }}
            onFocus={() => setIsFocused(true)}
            onBlur={handleBlur}
        >
            <div className="grid grid-cols-[auto,minmax(0,1fr)] items-start gap-x-4 gap-y-1">
                <span className={`inline-flex items-center gap-1 min-w-0 ${labelClassName}`}>
                    {label}
                    <button
                        type="button"
                        aria-label="Water rate conversion"
                        aria-expanded={isOpen}
                        aria-controls={tooltipId}
                        onClick={() => {
                            if (isPinned) {
                                setIsPinned(false);
                                setIsFocused(false);
                                setIsHovered(false);
                                setIsHoverSuppressed(true);
                            } else {
                                setIsPinned(true);
                                setIsHoverSuppressed(false);
                            }
                        }}
                        className={`inline-flex h-4 w-4 shrink-0 items-center justify-center rounded-full border text-[10px] font-bold cursor-help focus:outline-none focus:ring-2 focus:ring-brand-primary/20 ${iconClassName}`}
                    >
                        ?
                    </button>
                </span>
                <span className={`min-w-0 whitespace-normal break-words text-right leading-snug ${valueClassName}`}>
                    {value}
                </span>
            </div>

            {isOpen && (
                <div
                    id={tooltipId}
                    role="tooltip"
                    className={`w-full rounded-lg border px-3 py-2 text-xs font-medium leading-snug ${panelClassName}`}
                >
                    {text}
                </div>
            )}
        </div>
    );
}
