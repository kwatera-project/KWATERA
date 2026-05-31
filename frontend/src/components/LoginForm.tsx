import '../App.css'
import { GATEWAY_BASE_URL } from '../api/apiConfig'
import { useState } from "react"
import { useNavigate, Link } from "react-router-dom"

export default function LoginForm() {
    const [formData, setFormData] = useState({ email: '', password: '' })
    const navigate = useNavigate()
    const [message, setMessage] = useState('')

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        try {
            const response = await fetch(`${GATEWAY_BASE_URL}/api/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(formData),
            })
            if (response.ok) {
                const data = await response.json()
                localStorage.setItem("token", data.token)
                navigate("/")
            } else {
                const errorData = await response.json().catch(() => ({ error: 'Login failed' }))
                setMessage(`✘ ${errorData.error || 'Login failed'}`)
            }
        } catch (_error) {
            setMessage('✘ Error connecting to server')
        }
    }

    return (
        <div className="h-screen w-full flex flex-col bg-white">
            <header className="flex-none bg-white border-b border-gray-100 px-8 py-10 flex items-center justify-between z-50 shadow-sm">
            </header>

            <div className="flex-1 w-full grid grid-cols-1 lg:grid-cols-12 overflow-hidden">
                <div className="lg:col-span-5 flex flex-col justify-center items-center overflow-y-auto p-4">
                    <div className="w-full max-w-md bg-white border border-gray-100 rounded-2xl shadow-xl p-8 sm:p-10 space-y-6">
                        <div className="text-center space-y-2">
                            <h1 className="text-2xl font-bold text-gray-900">Sign In</h1>
                            <p className="text-sm text-gray-500">Welcome back! Please enter your details to access your account.</p>
                        </div>

                        <form onSubmit={handleSubmit} className="space-y-4">
                            <div className="space-y-1.5">
                                <label className="block text-sm font-medium text-gray-700">Email Address</label>
                                <input
                                    type="email"
                                    placeholder="name@example.com"
                                    className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#42211D] focus:border-[#42211D] outline-none transition"
                                    onChange={e => setFormData({ ...formData, email: e.target.value })}
                                    required
                                />
                            </div>

                            <div className="space-y-1.5">
                                <label className="block text-sm font-medium text-gray-700">Password</label>
                                <input
                                    type="password"
                                    placeholder="••••••••"
                                    className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#42211D] focus:border-[#42211D] outline-none transition"
                                    onChange={e => setFormData({ ...formData, password: e.target.value })}
                                    required
                                />
                            </div>

                            <button
                                type="submit"
                                className="w-full py-3 bg-[#42211D] text-white font-bold rounded-lg hover:opacity-90 transition shadow-sm"
                            >
                                Sign In
                            </button>
                        </form>

                        <p className="text-center text-sm text-gray-600">
                            Don't have an account?{" "}
                            <Link to="/register" className="text-[#42211D] font-bold hover:underline">
                                Sign Up
                            </Link>
                        </p>

                        {message && (
                            <div className={`p-4 rounded-lg text-sm font-medium ${message.startsWith('✔') ? 'bg-emerald-50 text-emerald-700' : 'bg-red-50 text-red-700'}`}>
                                {message}
                            </div>
                        )}
                    </div>
                </div>

                <div className="hidden lg:block lg:col-span-7 relative overflow-hidden">
                    <img
                        src="https://images.pexels.com/photos/5364965/pexels-photo-5364965.jpeg"
                        alt="Interior"
                        className="w-full h-full object-cover"
                    />
                </div>
            </div>
        </div>
    )
}