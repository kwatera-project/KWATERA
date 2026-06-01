import { useState, useRef, useEffect } from "react";
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
                className="p-1 hover:bg-gray-100 rounded-full text-[#1A1A1A] transition-colors disabled:opacity-30 font-bold px-2"
            >
                &lt;
            </button>

            <div className="relative" ref={dropdownRef}>
                <button
                    type="button"
                    onClick={handleToggle}
                    className="text-[#1A1A1A] text-[15px] font-bold capitalize hover:bg-gray-100 px-3 py-1 rounded-md transition-colors"
                >
                    {date.toLocaleString('en-US', { month: 'long' })} {date.getFullYear()}
                </button>

                {isOpen && (
                    <div 
                        className="absolute top-full left-1/2 -translate-x-1/2 mt-2 w-64 bg-white border border-gray-200 rounded-lg shadow-xl z-50 p-3"
                        style={{ backgroundColor: "#FFFFFF" }}
                    >
                        <div className="flex items-center justify-between mb-4 bg-white" style={{ backgroundColor: "#FFFFFF" }}>
                            <button
                                type="button"
                                onClick={() => setPickerYear(pickerYear - 1)}
                                className="p-1.5 hover:bg-gray-100 rounded-md text-sm font-bold text-[#1A1A1A] transition-colors"
                            >
                                &lt;
                            </button>
                            <span className="font-bold text-[#1A1A1A] text-[15px]">{pickerYear}</span>
                            <button
                                type="button"
                                onClick={() => setPickerYear(pickerYear + 1)}
                                className="p-1.5 hover:bg-gray-100 rounded-md text-sm font-bold text-[#1A1A1A] transition-colors"
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
                                        className={`py-2 text-sm font-semibold rounded-md transition-colors ${
                                            isSelected
                                                ? "bg-[#1A1A1A] text-white"
                                                : "hover:bg-gray-100 text-[#1A1A1A]"
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
                className="p-1 hover:bg-gray-100 rounded-full text-[#1A1A1A] transition-colors disabled:opacity-30 font-bold px-2"
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
    datepickerRef
}: SharedDatePickerProps) {
    return (
        <DatePicker
            ref={datepickerRef}
            selected={selected ?? undefined}
            onChange={onChange}
            selectsStart={selectsStart}
            selectsEnd={selectsEnd}
            startDate={startDate ?? undefined}
            endDate={endDate ?? undefined}
            minDate={minDate ?? undefined}
            className={className || "bg-white text-sm font-bold text-[#1A1A1A] outline-none cursor-pointer w-24 text-center border-b border-transparent hover:border-gray-300 focus:border-blue-500 transition-colors pb-1"}
            dateFormat={dateFormat || "yyyy-MM-dd"}
            placeholderText={placeholderText}
            renderCustomHeader={(props) => <CustomCalendarHeader {...props} />}
            calendarClassName="bg-white border border-gray-200 shadow-xl rounded-lg custom-datepicker-has-header"
            popperClassName="z-[9999]"
            calendarStartDay={1}
        />
    );
}