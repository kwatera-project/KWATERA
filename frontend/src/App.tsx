import './App.css'
import {Routes, Route} from "react-router-dom"
import Navbar from "./components/Navbar"
import RegisterForm from "./components/RegisterForm"
import LoginForm from "./components/LoginForm.tsx";
import PropertyDetailsPage from "./pages/PropertyDetailsPage";
import PropertiesPage from "./pages/PropertiesPage.tsx";
import AdminReservationList from "./components/AdminReservationList"
import ProtectedRoute from "./components/ProtectedRoute"
import HomePage from "./pages/HomePage";
import ReservationDetailsPage from "./pages/ReservationDetailsPage";
import PaymentCancelPage from "./pages/PaymentCancelPage";

function App() {
    return (
        <>
            <Navbar/>
            <Routes>
                <Route path="/" element={<HomePage />} />
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
                <Route
                    path="/reservations/:id"
                    element={
                        <ProtectedRoute allowedRoles={[]}>
                            <ReservationDetailsPage />
                        </ProtectedRoute>
                    }
                />
                <Route path="/catalog" element={<PropertiesPage />} />
                <Route path="/property/:id" element={<PropertyDetailsPage />} />
                <Route path="/payment-cancel" element={<PaymentCancelPage />} />
            </Routes>
        </>
    )
}

export default App