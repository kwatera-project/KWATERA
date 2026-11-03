import {useNavigate} from "react-router-dom"

export function useLogout() {
    const navigate = useNavigate()

    return () => {
        localStorage.removeItem("token")
        window.dispatchEvent(new Event("kwatera-auth"))
        navigate("/login")
    }
}



