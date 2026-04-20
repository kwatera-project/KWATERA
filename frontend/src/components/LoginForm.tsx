import '../App.css'
import {useState} from "react"
import {useNavigate, Link} from "react-router-dom"

export default function LoginForm() {
    const [formData, setFormData] = useState({
        email: '',
        password: ''
    })

    const navigate = useNavigate()

    const [message, setMessage] = useState('')

    const handleSubmit = async (e: { preventDefault: () => void; }) => {
        e.preventDefault()

        try {
            const response = await fetch('http://localhost:8081/api/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(formData),
            })

            if (response.ok) {
                const data = await response.json()
                localStorage.setItem("token", data.token)

                setMessage('✔ Login successfully!')
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
        } catch (error) {
            setMessage('✘ Error connecting to API server')
        }
    }

    return (
        <div className="flex h-screen items-center justify-center bg-main">
            <div className="w-full max-w-md rounded-xl bg-card p-8 shadow-lg">
                <h1 className="mb-6 text-3xl font-bold text-title text-center">
                    Login
                </h1>
                <p className="mt-1 mb-1 text-center text-sm">
                    <Link to="/register" className="text-[rgb(var(--color-burgundy))] hover:underline">
                        Don't have an account?
                    </Link>
                </p>
                <form onSubmit={handleSubmit} className="space-y-4">
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
                        Login
                    </button>
                </form>

                {message && (
                    <p className="mt-4 text-center font-medium text-details">{message}</p>
                )}
            </div>
        </div>
    )
}