import { useNavigate } from "react-router-dom"

export function useLogout() {
    const navigate = useNavigate()

    return () => {
        localStorage.removeItem("token")
        navigate("/login")
    }
}



