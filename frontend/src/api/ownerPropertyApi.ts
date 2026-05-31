import { GATEWAY_BASE_URL } from "./apiConfig";

const API_URL = `${GATEWAY_BASE_URL}/api/owner`;

export async function getMyProperties() {
    const token = localStorage.getItem("token");

    const res = await fetch(
        `${API_URL}/properties`,
        {
            headers: {
                Authorization: `Bearer ${token}`,
            },
        }
    );

    if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
    }

    return res.json();
}

// export async function createProperty(property: CreatePropertyRequest) {
//     const res = await fetch(`${API_URL}/properties`, {
//         method: "POST",
//         headers: {
//             "Content-Type": "application/json",
//         },
//         credentials: "include",
//         body: JSON.stringify(property),
//     });
//
//     return res.json();
// }
//
// export async function updateProperty(
//     propertyId: string,
//     property: UpdatePropertyRequest
// ) {
//     const res = await fetch(
//         `${API_URL}/properties/${propertyId}`,
//         {
//             method: "PUT",
//             headers: {
//                 "Content-Type": "application/json",
//             },
//             credentials: "include",
//             body: JSON.stringify(property),
//         }
//     );
//
//     return res.json();
// }
//
// export async function deleteProperty(propertyId: string) {
//     await fetch(
//         `${API_URL}/properties/${propertyId}`,
//         {
//             method: "DELETE",
//             credentials: "include",
//         }
//     );
//}