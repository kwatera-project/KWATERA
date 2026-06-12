import { useEffect, useId, useRef, useState } from "react";
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
    const closeTimerRef = useRef<number | null>(null);
    const [isIconHovered, setIsIconHovered] = useState(false);
    const [isComponentHovered, setIsComponentHovered] = useState(false);
    const [isFocused, setIsFocused] = useState(false);
    const [isPinned, setIsPinned] = useState(false);
    const [isIconHoverSuppressed, setIsIconHoverSuppressed] = useState(false);
    const isOpen = isIconHovered || isComponentHovered || isFocused || isPinned;

    const clearCloseTimer = () => {
        if (closeTimerRef.current !== null) {
            window.clearTimeout(closeTimerRef.current);
            closeTimerRef.current = null;
        }
    };

    useEffect(() => {
        return () => {
            if (closeTimerRef.current !== null) {
                window.clearTimeout(closeTimerRef.current);
            }
        };
    }, []);

    const handleBlur = (event: FocusEvent<HTMLDivElement>) => {
        if (!event.currentTarget.contains(event.relatedTarget)) {
            setIsFocused(false);
            setIsPinned(false);
        }
    };

    const scheduleHoverClose = () => {
        clearCloseTimer();
        closeTimerRef.current = window.setTimeout(() => {
            setIsIconHovered(false);
            setIsComponentHovered(false);
            setIsIconHoverSuppressed(false);
        }, 200);
    };

    return (
        <div
            className="space-y-2 overflow-visible"
            onMouseEnter={() => {
                clearCloseTimer();
                if (isOpen) {
                    setIsComponentHovered(true);
                }
            }}
            onMouseLeave={scheduleHoverClose}
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
                        onFocus={() => setIsFocused(true)}
                        onMouseEnter={() => {
                            clearCloseTimer();
                            if (!isIconHoverSuppressed) {
                                setIsIconHovered(true);
                                setIsComponentHovered(true);
                            }
                        }}
                        onClick={() => {
                            if (isPinned) {
                                setIsPinned(false);
                                setIsFocused(false);
                                setIsIconHovered(false);
                                setIsComponentHovered(false);
                                setIsIconHoverSuppressed(true);
                            } else {
                                setIsPinned(true);
                                setIsComponentHovered(true);
                                setIsIconHoverSuppressed(false);
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
                    onMouseEnter={() => {
                        clearCloseTimer();
                        setIsComponentHovered(true);
                    }}
                    className={`w-full rounded-lg border px-3 py-2 text-xs font-medium leading-snug ${panelClassName}`}
                >
                    {text}
                </div>
            )}
        </div>
    );
}
