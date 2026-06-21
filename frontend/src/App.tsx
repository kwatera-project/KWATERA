import { lazy, Suspense } from "react";
import './App.css'
import { Toaster } from "react-hot-toast";
import {Routes, Route, useLocation} from "react-router-dom"
import Navbar from "./components/Navbar"
import ProtectedRoute from "./components/ProtectedRoute"
import {CurrencyProvider} from "./contexts/CurrencyContext";
import DemoModeBanner from "./components/DemoModeBanner";
import Footer from "./components/landing/Footer";

const FULL_SCREEN_ROUTES = ["/login", "/register", "/payment-cancel", "/forgot-password", "/reset-password"];

const HomePage = lazy(() => import("./pages/HomePage"));
const AboutPage = lazy(() => import("./pages/AboutPage"));
const RegisterForm = lazy(() => import("./components/RegisterForm"));
const LoginForm = lazy(() => import("./components/LoginForm.tsx"));
const ForgotPasswordPage = lazy(() => import("./pages/ForgotPasswordPage"));
const ResetPasswordPage = lazy(() => import("./pages/ResetPasswordPage"));
const DashboardPage = lazy(() => import("./pages/DashboardPage"));
const AdminSystemEventsPage = lazy(() => import("./pages/AdminSystemEventsPage"));
const AdminUsersOverviewPage = lazy(() => import("./pages/AdminUsersOverviewPage"));
const AdminReservationList = lazy(() => import("./components/AdminReservationList"));
const OccupancyCalendarPage = lazy(() => import("./pages/OccupancyCalendarPage"));
const MyReservationsPage = lazy(() => import("./pages/MyReservationsPage"));
const ReservationDetailsPage = lazy(() => import("./pages/ReservationDetailsPage"));
const ProfilePage = lazy(() => import("./pages/ProfilePage"));
const PropertiesPage = lazy(() => import("./pages/PropertiesPage.tsx"));
const PropertyDetailsPage = lazy(() => import("./pages/PropertyDetailsPage"));
const CheckoutPage = lazy(() => import("./pages/CheckoutPage"));
const PaymentCancelPage = lazy(() => import("./pages/PaymentCancelPage"));
const SettlementDetailsPage = lazy(() => import("./pages/SettlementDetailsPage"));
const MeterReadingsPage = lazy(() => import("./pages/MeterReadingsPage"));
const AdminMeterReadingsPage = lazy(() => import("./pages/AdminMeterReadingsPage"));
const OwnerPropertiesPage = lazy(() => import("./pages/OwnerPropertiesPage.tsx"));
const OwnerPropertyUnitsPage = lazy(() => import("./pages/OwnerPropertyUnitsPage.tsx"));
const EditPropertyPage = lazy(() => import("./pages/EditPropertyPage.tsx"));
const CreatePropertyPage = lazy(() => import("./pages/CreatePropertyPage.tsx"));
const CreateUnitPage = lazy(() => import("./pages/CreateUnitPage.tsx"));
const EditUnitPage = lazy(() => import("./pages/EditUnitPage.tsx"));
const EditPropertyImages = lazy(() => import("./pages/EditPropertyImages.tsx"));
const EditUnitImages = lazy(() => import("./pages/EditUnitImages.tsx"));

function LoadingFallback() {
    return (
        <div className="flex flex-col items-center justify-center min-h-[60vh] w-full text-[#1A1A1A]">
            <div className="relative flex items-center justify-center">
                <div className="absolute w-14 h-14 rounded-full border-4 border-[#DACDCA]/40 animate-pulse"></div>
                <div className="w-10 h-10 rounded-full border-4 border-t-[#42211D] border-r-transparent border-b-transparent border-l-transparent animate-spin"></div>
            </div>
            <p className="mt-4 text-xs font-bold uppercase tracking-widest text-[#7A7A7A] animate-pulse">
                Loading Kwatera...
            </p>
        </div>
    );
}

function App() {
    const location = useLocation();
    const showFooter = !FULL_SCREEN_ROUTES.some((path) => location.pathname.includes(path));

    return (
        <CurrencyProvider>
            <Toaster
                position="top-center"
                containerStyle={{
                    top: "50%",
                    left: "50%",
                    transform: "translate(-50%, -50%)",
                    zIndex: 999999
                }}
            />
            <div className="min-h-screen flex flex-col bg-card">
                <DemoModeBanner/>
                <Navbar/>
                <main className="flex-1">
                    <Suspense fallback={<LoadingFallback />}>
                        <Routes>
                            <Route path="/" element={<HomePage/>}/>
                            <Route path="/about" element={<AboutPage/>}/>
                            <Route path="/register" element={<RegisterForm/>}/>
                            <Route path="/login" element={<LoginForm/>}/>
                            <Route path="/forgot-password" element={<ForgotPasswordPage/>}/>
                            <Route path="/reset-password" element={<ResetPasswordPage/>}/>
                            <Route
                                path="/admin/dashboard"
                                element={
                                    <ProtectedRoute allowedRoles={['ROLE_ADMIN', 'ROLE_OWNER']}>
                                        <DashboardPage/>
                                    </ProtectedRoute>
                                }
                            />
                            <Route
                                path="/admin/logs"
                                element={
                                    <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
                                        <div className="p-4 sm:p-8 max-w-7xl mx-auto min-h-screen text-[#1A1A1A]">
                                            <AdminSystemEventsPage/>
                                        </div>
                                    </ProtectedRoute>
                                }
                            />
                            <Route
                                path="/admin/users"
                                element={
                                    <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
                                        <AdminUsersOverviewPage/>
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
                                element={
                                    <ProtectedRoute allowedRoles={['ROLE_OWNER']}>
                                        <CreatePropertyPage />
                                    </ProtectedRoute>
                                }
                            />
                            <Route
                                path="/owner/properties/:propertyId/edit"
                                element={
                                    <ProtectedRoute allowedRoles={['ROLE_OWNER']}>
                                        <EditPropertyPage />
                                    </ProtectedRoute>
                                }
                            />
                            <Route
                                path="/owner/properties/:propertyId/units/new"
                                element={
                                    <ProtectedRoute allowedRoles={['ROLE_OWNER']}>
                                        <CreateUnitPage />
                                    </ProtectedRoute>
                                }
                            />
                            <Route
                                path="/owner/properties/:propertyId/units/:unitId/edit"
                                element={
                                    <ProtectedRoute allowedRoles={['ROLE_OWNER']}>
                                        <EditUnitPage />
                                    </ProtectedRoute>
                                }
                            />
                            <Route
                                path="/owner/properties/:propertyId/images"
                                element={
                                    <ProtectedRoute allowedRoles={['ROLE_OWNER']}>
                                        <EditPropertyImages />
                                    </ProtectedRoute>
                                }
                            />
                            <Route
                                path="/owner/properties/:propertyId/units/:unitId/images"
                                element={
                                    <ProtectedRoute allowedRoles={['ROLE_OWNER']}>
                                        <EditUnitImages />
                                    </ProtectedRoute>
                                }
                            />
                        </Routes>
                    </Suspense>
                </main>
                {showFooter && <Footer/>}
            </div>
        </CurrencyProvider>
    )
}

export default App