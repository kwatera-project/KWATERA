import './App.css'
import {Routes, Route} from "react-router-dom"
import Navbar from "./components/Navbar"
import RegisterForm from "./components/RegisterForm"
import LoginForm from "./components/LoginForm.tsx";
import PropertiesPage from "./pages/PropertiesPage"
import PropertyDetailsPage from "./pages/PropertyDetailsPage";

function App() {
    return (
        <>
            <Navbar/>

            <Routes>
                <Route path="/" element={<PropertiesPage />}/>
                <Route path="/register" element={<RegisterForm/>}/>
                <Route path="/login" element={<LoginForm/>}/>
                <Route path="/property/:id" element={<PropertyDetailsPage />} />
            </Routes>
        </>
    )
}

export default App
