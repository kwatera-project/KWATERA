import './App.css'
import {Routes, Route} from "react-router-dom"
import Navbar from "./components/Navbar"
import RegisterForm from "./components/RegisterForm"
import LoginForm from "./components/LoginForm.tsx";
import PropertyDetailsPage from "./pages/PropertyDetailsPage";
import PropertiesPage from "./pages/PropertiesPage.tsx";
import CheckoutPage from "./pages/CheckoutPage";
import AdminReservationList from "./components/AdminReservationList"
import ProtectedRoute from "./components/ProtectedRoute"
import HomePage from "./pages/HomePage";
import ReservationDetailsPage from "./pages/ReservationDetailsPage";
import MyReservationsPage from "./pages/MyReservationsPage";
import PaymentCancelPage from "./pages/PaymentCancelPage";
import SettlementDetailsPage from "./pages/SettlementDetailsPage";
import OccupancyCalendarPage from "./pages/OccupancyCalendarPage";
import ProfilePage from "./pages/ProfilePage";
import DashboardPage from "./pages/DashboardPage";
import {CurrencyProvider} from "./contexts/CurrencyContext";
import MeterReadingsPage from "./pages/MeterReadingsPage";
import AdminMeterReadingsPage from "./pages/AdminMeterReadingsPage";
import OwnerPropertiesPage from "./pages/OwnerPropertiesPage.tsx";
import OwnerPropertyUnitsPage from "./pages/OwnerPropertyUnitsPage.tsx";
import EditPropertyPage from "./pages/EditPropertyPage.tsx";
import CreatePropertyPage from "./pages/CreatePropertyPage.tsx";
import CreateUnitPage from "./pages/CreateUnitPage.tsx";
import EditUnitPage from "./pages/EditUnitPage.tsx";
import EditPropertyImages from "./pages/EditPropertyImages.tsx";
import EditUnitImages from "./pages/EditUnitImages.tsx";
import AboutPage from "./pages/AboutPage";

function App() {
    return (
        <CurrencyProvider>
            <Navbar/>
            <Routes>
                <Route path="/" element={<HomePage/>}/>
                <Route path="/about" element={<AboutPage/>}/>
                <Route path="/register" element={<RegisterForm/>}/>
                <Route path="/login" element={<LoginForm/>}/>
                <Route
                    path="/admin/dashboard"
                    element={
                        <ProtectedRoute allowedRoles={['ROLE_ADMIN', 'ROLE_OWNER']}>
                            <DashboardPage/>
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/admin/reservations"
                    element={
                        <ProtectedRoute allowedRoles={['ROLE_ADMIN', 'ROLE_OWNER']}>
                            <AdminReservationList/>
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/admin/occupancy"
                    element={
                        <ProtectedRoute allowedRoles={['ROLE_ADMIN', 'ROLE_OWNER']}>
                            <OccupancyCalendarPage/>
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/my-reservations"
                    element={
                        <ProtectedRoute allowedRoles={['ROLE_GUEST']}>
                            <MyReservationsPage/>
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/reservations/:id"
                    element={
                        <ProtectedRoute allowedRoles={[]}>
                            <ReservationDetailsPage/>
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/profile"
                    element={
                        <ProtectedRoute>
                            <ProfilePage/>
                        </ProtectedRoute>
                    }
                />
                <Route path="/properties" element={<PropertiesPage/>}/>
                <Route path="/catalog" element={<PropertiesPage/>}/>
                <Route path="/property/:id" element={<PropertyDetailsPage/>}/>
                <Route
                    path="/checkout"
                    element={
                        <ProtectedRoute>
                            <CheckoutPage/>
                        </ProtectedRoute>
                    }
                />
                <Route path="/payment-cancel" element={<PaymentCancelPage/>}/>
                <Route
                    path="/settlements/:id"
                    element={
                        <ProtectedRoute allowedRoles={[]}>
                            <SettlementDetailsPage/>
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/settlements/:settlementId/meter-readings"
                    element={
                        <ProtectedRoute allowedRoles={['ROLE_GUEST']}>
                            <MeterReadingsPage/>
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/admin/settlements/:settlementId/meter-readings"
                    element={
                        <ProtectedRoute allowedRoles={['ROLE_ADMIN', 'ROLE_OWNER']}>
                            <AdminMeterReadingsPage/>
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/owner/properties"
                    element={
                        <ProtectedRoute allowedRoles={['ROLE_OWNER']}>
                            <OwnerPropertiesPage/>
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/owner/properties/:propertyId/units"
                    element={
                        <ProtectedRoute allowedRoles={['ROLE_OWNER']}>
                            <OwnerPropertyUnitsPage/>
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/owner/properties/new"
                    element={<CreatePropertyPage />}
                />
                <Route
                    path="/owner/properties/:propertyId/edit"
                    element={<EditPropertyPage />}
                />
                <Route
                    path="/owner/properties/:propertyId/units/new"
                    element={<CreateUnitPage />}
                />
                <Route
                    path="/owner/properties/:propertyId/units/:unitId/edit"
                    element={<EditUnitPage />}
                />
                <Route
                    path="/owner/properties/:propertyId/images"
                    element={<EditPropertyImages />}
                />
                <Route
                    path="/owner/properties/:propertyId/units/:unitId/images"
                    element={<EditUnitImages />}
                />
            </Routes>
        </CurrencyProvider>
    )
}

export default App
