import { useNavigate } from "react-router-dom";
import { IS_DEMO_MODE } from "../api/apiConfig";
import { demoUsers, signInAsDemoRole, type DemoUser } from "../demo/demoUsers";
import { useTranslation } from "react-i18next";

function roleTarget(user: DemoUser) {
    if (user.role === "ROLE_GUEST") return "/my-reservations";
    if (user.role === "ROLE_OWNER") return "/owner/properties";
    return "/admin/dashboard";
}

export default function DemoRoleSelector({ compact = false }: { compact?: boolean }) {
    const navigate = useNavigate();
    const { t } = useTranslation();

    if (!IS_DEMO_MODE) return null;

    return (
        <div className={compact ? "flex flex-wrap gap-2" : "space-y-3"}>
            {Object.values(demoUsers).map((user) => (
                <button
                    key={user.roleKey}
                    type="button"
                    onClick={() => {
                        signInAsDemoRole(user.roleKey);
                        navigate(roleTarget(user));
                    }}
                    className={
                        compact
                            ? "px-4 py-2 rounded-lg bg-white/95 text-[#42211D] border border-white/70 text-xs font-black shadow-sm hover:bg-white transition"
                            : "w-full px-4 py-3 rounded-lg bg-[#42211D] text-white font-black shadow-sm hover:bg-[#2a1412] transition"
                    }
                >
                    {t(`demo.roles.${user.roleKey}`, { defaultValue: user.label })}
                </button>
            ))}
        </div>
    );
}
