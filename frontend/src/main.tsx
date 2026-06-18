import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import './i18n'
import App from './App.tsx'
import { ROUTER_BASE_PATH } from './api/apiConfig.ts'

createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <BrowserRouter basename={ROUTER_BASE_PATH}>
            <App />
        </BrowserRouter>
    </StrictMode>,
)
