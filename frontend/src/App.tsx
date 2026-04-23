import './App.css'
import {Routes, Route} from "react-router-dom"
import Navbar from "./components/Navbar"
import RegisterForm from "./components/RegisterForm"
import LoginForm from "./components/LoginForm"
import AdminReservationList from "./components/AdminReservationList"
import ProtectedRoute from "./components/ProtectedRoute"

function App() {
    return (
        <>
            <Navbar/>
            <Routes>
                <Route path="/" element={<h1 className="mb-6 text-3xl font-bold text-title text-center">Home</h1>}/>
                <Route path="/register" element={<RegisterForm/>}/>
                <Route path="/login" element={<LoginForm/>}/>
                <Route
                    path="/admin/reservations"
                    element={
                        <ProtectedRoute allowedRoles={['ADMIN', 'OWNER']}>
                            <AdminReservationList />
                        </ProtectedRoute>
                    }
                />
            </Routes>
        </>
    )
}

export default App