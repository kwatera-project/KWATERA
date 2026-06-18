import '../App.css'
import { GATEWAY_BASE_URL, IS_DEMO_MODE } from '../api/apiConfig'
import { useState } from "react"
import { useNavigate, Link } from "react-router-dom"
import { useTranslation } from "react-i18next"
import DemoRoleSelector from "./DemoRoleSelector"

export default function RegisterForm() {
    const [formData, setFormData] = useState({
        username: '',
        email: '',
        password: '',
        role: '',
        firstName: '',
        lastName: ''
    })

    const navigate = useNavigate()
    const [message, setMessage] = useState('')
    const { t } = useTranslation();

    if (IS_DEMO_MODE) {
        return (
            <div className="min-h-screen w-full flex items-center justify-center bg-white p-6">
                <div className="w-full max-w-md bg-white border border-gray-100 rounded-2xl shadow-xl p-8 sm:p-10 space-y-6">
                    <div className="text-center space-y-2">
                        <h1 className="text-2xl font-bold text-gray-900">Demo login</h1>
                        <p className="text-sm text-gray-500">Choose a role without creating an account.</p>
                    </div>
                    <DemoRoleSelector />
                    <Link to="/" className="block text-center text-sm text-[#42211D] font-bold hover:underline">
                        Back to landing page
                    </Link>
                </div>
            </div>
        );
    }

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        try {
            const response = await fetch(`${GATEWAY_BASE_URL}/api/auth/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(formData),
            })
            if (response.ok) {
                const loginRes = await fetch(`${GATEWAY_BASE_URL}/api/auth/login`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email: formData.email, password: formData.password })
                })
                const loginData = await loginRes.json()
                localStorage.setItem("token", loginData.token)
                navigate("/")
            } else {
                const errorData = await response.json().catch(() => ({ error: t('register.registrationFailed') }))
                setMessage(`✘ ${errorData.error || t('register.registrationFailed')}`)
            }
        } catch {
            setMessage(`✘ ${t('login.serverError')}`)
        }
    }

    return (
        <div className="h-screen w-full flex flex-col bg-white">
            <header className="flex-none bg-white border-b border-gray-100 px-8 py-9 flex items-center justify-between z-50 shadow-sm">
            </header>

            <div className="flex-1 w-full grid grid-cols-1 lg:grid-cols-12 overflow-hidden">
                <div className="lg:col-span-5 flex flex-col justify-center items-center overflow-y-auto p-4">
                    <div className="w-full max-w-md bg-white border border-gray-100 rounded-2xl shadow-xl p-8 sm:p-10 space-y-6">
                        <div className="text-center space-y-2">
                            <h1 className="text-2xl font-bold text-gray-900">{t('register.title')}</h1>
                            <p className="text-sm text-gray-500"> {t('register.subtitle')}</p>
                        </div>

                        <form onSubmit={handleSubmit} className="space-y-4">
                            <div className="space-y-1.5">
                                <label className="block text-sm font-medium text-gray-700"> {t('register.registerAs')}</label>
                                <div className="grid grid-cols-2 gap-4">
                                    <label className={`flex items-center justify-center p-3 rounded-lg border cursor-pointer transition ${formData.role === 'OWNER' ? 'border-[#42211D] bg-orange-50 font-semibold' : 'border-gray-300 hover:bg-gray-50'}`}>
                                        <input type="radio" value="OWNER" checked={formData.role === 'OWNER'} onChange={e => setFormData({...formData, role: e.target.value})} className="mr-2" />
                                        {t('register.owner')}
                                    </label>
                                    <label className={`flex items-center justify-center p-3 rounded-lg border cursor-pointer transition ${formData.role === 'GUEST' ? 'border-[#42211D] bg-orange-50 font-semibold' : 'border-gray-300 hover:bg-gray-50'}`}>
                                        <input type="radio" value="GUEST" checked={formData.role === 'GUEST'} onChange={e => setFormData({...formData, role: e.target.value})} className="mr-2" />
                                        {t('register.guest')}
                                    </label>
                                </div>
                            </div>

                            <div className="flex flex-col sm:flex-row gap-4">
                                <div className="w-full sm:w-1/2 space-y-1.5">
                                    <label className="block text-sm font-medium text-gray-700">{t('register.firstName')}</label>
                                    <input type="text" className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#42211D] outline-none" onChange={e => setFormData({...formData, firstName: e.target.value})} required />
                                </div>
                                <div className="w-full sm:w-1/2 space-y-1.5">
                                    <label className="block text-sm font-medium text-gray-700">{t('register.lastName')}</label>
                                    <input type="text" className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#42211D] outline-none" onChange={e => setFormData({...formData, lastName: e.target.value})} required />
                                </div>
                            </div>

                            <div className="space-y-1.5">
                                <label className="block text-sm font-medium text-gray-700">{t('register.username')}</label>
                                <input type="text" className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#42211D] outline-none" onChange={e => setFormData({...formData, username: e.target.value})} required />
                            </div>

                            <div className="space-y-1.5">
                                <label className="block text-sm font-medium text-gray-700">{t('login.email')}</label>
                                <input type="email" className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#42211D] outline-none" onChange={e => setFormData({...formData, email: e.target.value})} required />
                            </div>

                            <div className="space-y-1.5">
                                <label className="block text-sm font-medium text-gray-700">{t('login.password')}</label>
                                <input type="password" className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#42211D] outline-none" onChange={e => setFormData({...formData, password: e.target.value})} required />
                            </div>

                            <button type="submit" className="w-full py-3 bg-[#42211D] text-white font-bold rounded-lg hover:opacity-90 transition">
                                {t('register.submit')}
                            </button>
                        </form>

                        <p className="text-center text-sm text-gray-600">
                            {t('register.hasAccount')} <Link to="/login" className="text-[#42211D] font-bold hover:underline">{t('login.title')}</Link>
                        </p>

                        {message && (
                            <div className={`p-4 rounded-lg text-sm font-medium ${message.startsWith('✔') ? 'bg-emerald-50 text-emerald-700' : 'bg-red-50 text-red-700'}`}>
                                {message}
                            </div>
                        )}
                    </div>
                </div>

                <div className="hidden lg:block lg:col-span-7 relative overflow-hidden">
                    <img src="https://images.pexels.com/photos/5358783/pexels-photo-5358783.jpeg" className="w-full h-full object-cover" alt="Villa" />
                </div>
            </div>
        </div>
    )
}