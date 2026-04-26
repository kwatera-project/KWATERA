import './App.css'
import {Routes, Route} from "react-router-dom"
import Navbar from "./components/Navbar"
import RegisterForm from "./components/RegisterForm"
import LoginForm from "./components/LoginForm.tsx";
import PropertyDetailsPage from "./pages/PropertyDetailsPage";
import PropertiesPage from "./pages/PropertiesPage.tsx";
import AdminReservationList from "./components/AdminReservationList"
import ProtectedRoute from "./components/ProtectedRoute"
import ReservationDetailsPage from "./pages/ReservationDetailsPage";

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
                        <ProtectedRoute allowedRoles={['ROLE_ADMIN', 'ROLE_OWNER']}>
                            <AdminReservationList />
                        </ProtectedRoute>
                    }
                />
                <Route path="/catalog" element={<PropertiesPage />} />
                <Route path="/property/:id" element={<PropertyDetailsPage />} />
                <Route
                    path="/reservations/:id"
                    element={
                        <ProtectedRoute allowedRoles={[]}>
                            <ReservationDetailsPage />
                        </ProtectedRoute>
                    }
                />
            </Routes>
        </>
    )
}

export default App