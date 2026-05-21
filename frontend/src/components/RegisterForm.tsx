import '../App.css'
import { GATEWAY_BASE_URL } from '../api/apiConfig'
import {useState} from "react"
import {useNavigate} from "react-router-dom";

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

    const handleSubmit = async (e: { preventDefault: () => void; }) => {
        e.preventDefault()

        try {
            const response = await fetch(`${GATEWAY_BASE_URL}/api/auth/register`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(formData),
            })

            if (response.ok) {
                const loginRes = await fetch(`${GATEWAY_BASE_URL}/api/auth/login`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        email: formData.email,
                        password: formData.password
                    })
                })

                const loginData = await loginRes.json()
                localStorage.setItem("token", loginData.token)

                setMessage('✔ Registered & logged in!')
                navigate("/")
            } else {

                let errorMessage

                try {
                    const errorData = await response.json()
                    errorMessage = errorData.error
                } catch {
                    errorMessage = await response.text()
                }

                setMessage(`✘ ${errorMessage}`)
            }
        } catch (_error) {
            console.error("Auth Error:", _error)
            setMessage('✘ Error connecting to API server')
        }
    }

    return (
        <div className="flex h-screen items-center justify-center bg-main">
            <div className="w-full max-w-md rounded-xl bg-card p-8 shadow-lg">
                <h1 className="mb-6 text-3xl font-bold text-title text-center">
                     Registration
                </h1>
                <form onSubmit={handleSubmit} className="space-y-4">
                    <div className="flex gap-4">
                        <label className="flex items-center gap-2 cursor-pointer">
                            <input
                                type="radio"
                                value="OWNER"
                                checked={formData.role === 'OWNER'}
                                onChange={e => setFormData({...formData, role: e.target.value})}
                                className="accent-[rgb(var(--color-burgundy))]"
                            />
                            Owner
                        </label>

                        <label className="flex items-center gap-2 cursor-pointer">
                            <input
                                type="radio"
                                value="GUEST"
                                checked={formData.role === 'GUEST'}
                                onChange={e => setFormData({...formData, role: e.target.value})}
                                className="accent-[rgb(var(--color-burgundy))]"
                            />
                            Guest
                        </label>

                    </div>
                    <div className="flex gap-4">
                        <input
                            type="text"
                            placeholder="First Name"
                            className="w-1/2 p-2 border rounded"
                            onChange={e => setFormData({...formData, firstName: e.target.value})}
                            required
                        />
                        <input
                            type="text"
                            placeholder="Last Name"
                            className="w-1/2 p-2 border rounded"
                            onChange={e => setFormData({...formData, lastName: e.target.value})}
                            required
                        />
                    </div>
                    <input
                        type="text"
                        placeholder="Username"
                        className="w-full p-2 border rounded"
                        onChange={e => setFormData({...formData, username: e.target.value})}
                        required
                    />
                    <input
                        type="email"
                        placeholder="Email"
                        className="w-full p-2 border rounded"
                        onChange={e => setFormData({...formData, email: e.target.value})}
                        required
                    />
                    <input
                        type="password"
                        placeholder="Password"
                        className="w-full p-2 border rounded"
                        onChange={e => setFormData({...formData, password: e.target.value})}
                        required
                    />
                    <button
                        type="submit"
                        className="w-full bg-button text-white py-2 rounded font-bold bg-button-hover transition"
                    >
                        Register
                    </button>
                </form>

                {message && (
                    <p className="mt-4 text-center font-medium text-details">{message}</p>
                )}
            </div>
        </div>
    )
}