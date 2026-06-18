import React, { useState } from "react";
import { X } from "lucide-react";
import { useTranslation } from "react-i18next";

interface TagInputProps {
    tags: string[];
    onChange: (tags: string[]) => void;
    label?: string;
    placeholder?: string;
}

export default function TagInput({ tags, onChange, label, placeholder }: TagInputProps) {
    const [input, setInput] = useState("");
    const { t } = useTranslation();

    const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === "Enter" || e.key === ",") {
            e.preventDefault();
            const tagValue = input.trim();
            if (tagValue && !tags.includes(tagValue)) {
                onChange([...tags, tagValue]);
            }
            setInput("");
        }
    };

    const handleRemove = (tagToRemove: string) => {
        onChange(tags.filter(tag => tag !== tagToRemove));
    };

    return (
        <div className="space-y-1 w-full">
            {label && (
                <label className="text-xs font-bold text-[#7A7A7A] uppercase tracking-wider">
                    {label}
                </label>
            )}
            <div className="border border-[#DACDCA] rounded-lg p-2 bg-transparent focus-within:border-[#42211D] focus-within:ring-1 focus-within:ring-[#42211D] transition-all flex flex-wrap gap-2 items-center min-h-[48px]">
                {tags.map((tag, index) => (
                    <span
                        key={index}
                        className="inline-flex items-center gap-1.5 bg-[#42211D]/10 text-[#42211D] border border-[#42211D]/20 rounded-full px-3 py-1 text-xs font-semibold select-none"
                    >
                        {tag}
                        <button
                            type="button"
                            onClick={() => handleRemove(tag)}
                            className="text-[#42211D]/75 hover:text-[#42211D] rounded-full hover:bg-[#42211D]/20 p-0.5 transition-colors focus:outline-none"
                        >
                            <X className="w-3 h-3" />
                        </button>
                    </span>
                ))}
                <input
                    type="text"
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    onKeyDown={handleKeyDown}
                    placeholder={tags.length === 0 ? (placeholder ?? t('tagInput.placeholder')) : ""}
                    className="flex-1 bg-transparent border-none outline-none p-1 text-sm font-semibold text-[#1A1A1A] placeholder-[#7A7A7A]/70 min-w-[120px]"
                />
            </div>
            <p className="text-xxs text-[#7A7A7A] font-semibold italic mt-1">{t('tagInput.hint')}</p>
        </div>
    );
}
