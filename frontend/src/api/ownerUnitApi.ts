import { GATEWAY_BASE_URL } from "./apiConfig";

const API_URL = `${GATEWAY_BASE_URL}/api/owner`;

export async function getPropertyUnits(
    propertyId: string
) {

    const token = localStorage.getItem("token");

    const res = await fetch(
        `${API_URL}/properties/${propertyId}/units`,
        {
            headers: {
                "Authorization": `Bearer ${token}`,
            },
        }
    );

    if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
    }

    return res.json();
}

// export async function createUnit(
//     propertyId: string,
//     unit: CreateUnitRequest
// ) {
//     const res = await fetch(
//         `${API_URL}/properties/${propertyId}/units`,
//         {
//             method: "POST",
//             headers: {
//                 "Content-Type": "application/json",
//             },
//             credentials: "include",
//             body: JSON.stringify(unit),
//         }
//     );
//
//     return res.json();
// }
//
// export async function updateUnit(
//     unitId: string,
//     unit: UpdateUnitRequest
// ) {
//     const res = await fetch(
//         `${API_URL}/units/${unitId}`,
//         {
//             method: "PUT",
//             headers: {
//                 "Content-Type": "application/json",
//             },
//             credentials: "include",
//             body: JSON.stringify(unit),
//         }
//     );
//
//     return res.json();
// }
//
// export async function deleteUnit(unitId: string) {
//     await fetch(
//         `${API_URL}/units/${unitId}`,
//         {
//             method: "DELETE",
//             credentials: "include",
//         }
//     );
// }