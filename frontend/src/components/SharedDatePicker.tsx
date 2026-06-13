import { useState, useRef, useEffect } from "react";
import type { ComponentProps } from "react";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";

interface SharedDatePickerProps {
    selected: Date | null;
    onChange: (date: Date | null) => void;
    selectsStart?: boolean;
    selectsEnd?: boolean;
    startDate?: Date | null;
    endDate?: Date | null;
    minDate?: Date | null;
    placeholderText?: string;
    className?: string;
    dateFormat?: string;
    datepickerRef?: React.RefObject<DatePicker | null>;
    wrapperClassName?: string;
    popperPlacement?: ComponentProps<typeof DatePicker>["popperPlacement"];
    allowPastDates?: boolean;
}

const MONTHS = [
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
];

export interface CustomCalendarHeaderProps {
    date: Date;
    changeYear: (year: number) => void;
    changeMonth: (month: number) => void;
    decreaseMonth: () => void;
    increaseMonth: () => void;
    prevMonthButtonDisabled: boolean;
    nextMonthButtonDisabled: boolean;
}

export const CustomCalendarHeader = ({
    date,
    changeYear,
    changeMonth,
    decreaseMonth,
    increaseMonth,
    prevMonthButtonDisabled,
    nextMonthButtonDisabled,
}: CustomCalendarHeaderProps) => {
    const [isOpen, setIsOpen] = useState(false);
    const [pickerYear, setPickerYear] = useState(date.getFullYear());
    const dropdownRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
                setIsOpen(false);
            }
        };
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    const handleToggle = () => {
        setPickerYear(date.getFullYear());
        setIsOpen(!isOpen);
    };

    const selectMonth = (monthIndex: number) => {
        changeYear(pickerYear);
        changeMonth(monthIndex);
        setIsOpen(false);
    };

    return (
        <div 
            className="flex items-center justify-between px-3 py-2 bg-white relative border-b border-gray-100"
            style={{ backgroundColor: "#FFFFFF" }}
        >
            <button
                type="button"
                onClick={decreaseMonth}
                disabled={prevMonthButtonDisabled}
                className="p-1 hover:bg-stone-100 rounded-full text-stone-800 transition-colors disabled:opacity-30 font-bold px-2 cursor-pointer"
            >
                &lt;
            </button>

            <div className="relative" ref={dropdownRef}>
                <button
                    type="button"
                    onClick={handleToggle}
                    className="text-stone-800 text-[15px] font-bold capitalize hover:bg-stone-100 px-3 py-1 rounded-md transition-colors cursor-pointer"
                >
                    {date.toLocaleString('en-US', { month: 'long' })} {date.getFullYear()}
                </button>

                {isOpen && (
                    <div 
                        className="absolute top-full left-1/2 -translate-x-1/2 mt-2 w-64 bg-white border border-stone-200 rounded-lg shadow-xl z-50 p-3"
                        style={{ backgroundColor: "#FFFFFF" }}
                    >
                        <div className="flex items-center justify-between mb-4 bg-white" style={{ backgroundColor: "#FFFFFF" }}>
                            <button
                                type="button"
                                onClick={() => setPickerYear(pickerYear - 1)}
                                className="p-1.5 hover:bg-stone-100 rounded-md text-sm font-bold text-stone-800 transition-colors cursor-pointer"
                            >
                                &lt;
                            </button>
                            <span className="font-bold text-stone-800 text-[15px]">{pickerYear}</span>
                            <button
                                type="button"
                                onClick={() => setPickerYear(pickerYear + 1)}
                                className="p-1.5 hover:bg-stone-100 rounded-md text-sm font-bold text-stone-800 transition-colors cursor-pointer"
                            >
                                &gt;
                            </button>
                        </div>

                        <div className="grid grid-cols-3 gap-2 bg-white" style={{ backgroundColor: "#FFFFFF" }}>
                            {MONTHS.map((month, index) => {
                                const isSelected = pickerYear === date.getFullYear() && index === date.getMonth();
                                return (
                                    <button
                                        key={month}
                                        type="button"
                                        onClick={() => selectMonth(index)}
                                        className={`py-2 text-sm font-semibold rounded-md transition-colors cursor-pointer ${
                                            isSelected
                                                ? "bg-stone-800 text-white"
                                                : "hover:bg-stone-100 text-stone-800"
                                        }`}
                                        style={!isSelected ? { backgroundColor: "#FFFFFF" } : undefined}
                                    >
                                        {month}
                                    </button>
                                );
                            })}
                        </div>
                    </div>
                )}
            </div>

            <button
                type="button"
                onClick={increaseMonth}
                disabled={nextMonthButtonDisabled}
                className="p-1 hover:bg-stone-100 rounded-full text-stone-800 transition-colors disabled:opacity-30 font-bold px-2 cursor-pointer"
            >
                &gt;
            </button>
        </div>
    );
};

export default function SharedDatePicker({
    selected,
    onChange,
    selectsStart,
    selectsEnd,
    startDate,
    endDate,
    minDate,
    placeholderText,
    className,
    dateFormat,
    datepickerRef,
    wrapperClassName,
    popperPlacement,
    allowPastDates = false
}: SharedDatePickerProps) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const resolvedMinDate = allowPastDates
        ? (minDate ?? undefined)
        : (minDate ? (minDate.getTime() < today.getTime() ? today : minDate) : today);

    return (
        <DatePicker
                ref={datepickerRef}
                selected={selected ?? undefined}
                onChange={onChange}
                selectsStart={selectsStart}
                selectsEnd={selectsEnd}
                startDate={startDate ?? undefined}
                endDate={endDate ?? undefined}
                minDate={resolvedMinDate}
                className={className || "bg-white text-sm font-bold text-[#1A1A1A] outline-none cursor-pointer w-24 text-center border-b border-transparent hover:border-gray-300 focus:border-stone-500 transition-colors pb-1"}
                dateFormat={dateFormat || "yyyy-MM-dd"}
                placeholderText={placeholderText}
                renderCustomHeader={(props) => <CustomCalendarHeader {...props} />}
                calendarClassName="bg-white border border-gray-200 shadow-xl rounded-lg custom-datepicker-has-header"
                popperClassName="z-[9999]"
                calendarStartDay={1}
                wrapperClassName={wrapperClassName}
                popperPlacement={popperPlacement}
                popperProps={{
                    strategy: "fixed"
                }}
            />
    );
}
