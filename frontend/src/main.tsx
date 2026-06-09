import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import App from './App.tsx'
import { APP_BASE_PATH } from './api/apiConfig.ts'

createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <BrowserRouter basename={APP_BASE_PATH}>
            <App />
        </BrowserRouter>
    </StrictMode>,
)
