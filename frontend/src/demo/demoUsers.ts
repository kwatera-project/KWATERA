import { createDemoToken, DEMO_ROLE_STORAGE_KEY, DEMO_TOKEN_STORAGE_KEY, type DemoRole } from "./demoConfig";

export type DemoUser = {
    id: string;
    roleKey: DemoRole;
    role: "ROLE_GUEST" | "ROLE_OWNER" | "ROLE_ADMIN";
    label: string;
    username: string;
    firstName: string;
    lastName: string;
    email: string;
};

export const demoUsers: Record<DemoRole, DemoUser> = {
    guest: {
        id: "demo-user-guest",
        roleKey: "guest",
        role: "ROLE_GUEST",
        label: "Wejdź jako Gość",
        username: "demo.gosc",
        firstName: "Anna",
        lastName: "Nowak",
        email: "guest.demo@kwatera.local",
    },
    owner: {
        id: "demo-user-owner",
        roleKey: "owner",
        role: "ROLE_OWNER",
        label: "Wejdź jako Owner",
        username: "demo.owner",
        firstName: "Marek",
        lastName: "Zieliński",
        email: "owner.demo@kwatera.local",
    },
    admin: {
        id: "demo-user-admin",
        roleKey: "admin",
        role: "ROLE_ADMIN",
        label: "Wejdź jako Admin",
        username: "demo.admin",
        firstName: "Katarzyna",
        lastName: "Wiśniewska",
        email: "admin.demo@kwatera.local",
    },
};

export function getDemoUser(role: DemoRole | null = getSelectedDemoRole()) {
    return role ? demoUsers[role] : demoUsers.guest;
}

export function getSelectedDemoRole(): DemoRole | null {
    const role = localStorage.getItem(DEMO_ROLE_STORAGE_KEY);
    return role === "guest" || role === "owner" || role === "admin" ? role : null;
}

export function signInAsDemoRole(role: DemoRole) {
    const user = demoUsers[role];
    const token = createDemoToken({
        sub: user.email,
        userId: user.id,
        role: user.role,
        firstName: user.firstName,
        lastName: user.lastName,
        demoRole: role,
    });

    localStorage.setItem(DEMO_TOKEN_STORAGE_KEY, token);
    localStorage.setItem(DEMO_ROLE_STORAGE_KEY, role);
    window.dispatchEvent(new Event("kwatera-demo-auth"));
}
