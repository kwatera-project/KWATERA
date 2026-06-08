import { Brain, Scan, Calendar, LayoutDashboard, Server, Database, Code } from 'lucide-react';
import Footer from '../components/landing/Footer';

interface TeamMember {
    name: string;
    role: string;
    bio: string;
    github: string;
    linkedin: string;
    initials: string;
    bgColor: string;
}

export default function AboutPage() {
    const teamMembers: TeamMember[] = [
        {
            name: "Zuzanna Adamczyk",
            role: "AI/OCR & Catalog Engineer",
            bio: "Engineered the core OCR module for automated utility readings and developed the property catalog. Bridging artificial intelligence with practical, automated billing solutions.",
            github: "https://github.com/ZuzannaAdamczyk",
            linkedin: "https://www.linkedin.com/in/zuzanna-adamczyk-26a56928a/",
            initials: "ZA",
            bgColor: "bg-[#42211D]/10 text-[#42211D]"
        },
        {
            name: "Łukasz Jęcek",
            role: "Tech Lead & System Architect",
            bio: "Orchestrated the microservices architecture, CI/CD pipelines, and system integration. Established robust coding standards to ensure platform stability and scalability.",
            github: "https://github.com/lukaszjecek",
            linkedin: "https://www.linkedin.com/in/lukasz-jecek",
            initials: "ŁJ",
            bgColor: "bg-[#42211D]/10 text-[#42211D]"
        },
        {
            name: "Nadzeya Silchankava",
            role: "Backend Domain & Payments Engineer",
            bio: "Architected the foundational backend domain, focusing on billing microservices and Stripe payment integration, enabling a full-cycle rental administration.",
            github: "https://github.com/sinadzeya",
            linkedin: "https://www.linkedin.com/in/nadzeya-silchankava/",
            initials: "NS",
            bgColor: "bg-[#42211D]/10 text-[#42211D]"
        },
        {
            name: "Alicja Świercz",
            role: "Reservation Flow & UX Engineer",
            bio: "Spearheaded the end-to-end reservation lifecycle and crafted the frontend user experience. Designed intuitive dashboards and secure checkout flows, ensuring a seamless journey for all users.",
            github: "https://github.com/alicjaswiers",
            linkedin: "https://www.linkedin.com/in/alicjaswiers/",
            initials: "AŚ",
            bgColor: "bg-[#42211D]/10 text-[#42211D]"
        }
    ];

    const techStack = [
        {
            category: "Frontend",
            icon: <Code className="w-6 h-6 text-[#42211D]" />,
            techs: ["React 19", "TypeScript", "TailwindCSS v4", "Vite", "React Router 6"]
        },
        {
            category: "Backend Services",
            icon: <Database className="w-6 h-6 text-[#42211D]" />,
            techs: ["Java 25", "Spring Boot", "Spring Security & JWT", "PostgreSQL", "REST APIs"]
        },
        {
            category: "AI & OCR Models",
            icon: <Brain className="w-6 h-6 text-[#42211D]" />,
            techs: ["Python", "YOLOv8 Object Detection", "Tesseract OCR", "Dynamic Pricing Engine"]
        }
    ];

    return (
        <div className="w-full bg-stone-50 font-sans min-h-screen -mt-8 text-stone-850 antialiased selection:bg-[#42211D]/10 selection:text-[#42211D] flex flex-col justify-between">
            
            <div className="w-full flex-grow">
                
                {/* 1. Hero Section (Centered & Spacious) */}
                <div className="bg-white border-b border-[#DACDCA] py-24 px-6 text-center">
                    <div className="max-w-4xl mx-auto space-y-8">
                        <span className="inline-flex items-center gap-1.5 px-3 py-1 text-xs font-bold tracking-[0.2em] uppercase text-[#42211D] bg-[#42211D]/5 rounded-full">
                            About Kwatera
                        </span>
                        <h1 className="text-4xl md:text-6xl font-extrabold text-stone-900 tracking-tight leading-tight">
                            KWATERA: Next-Generation Vacation Rental Management
                        </h1>
                        <p className="text-lg md:text-xl text-stone-550 max-w-3xl mx-auto font-light leading-relaxed">
                            An all-in-one platform integrating intelligent utility billing, dynamic AI pricing, and seamless reservation flows to empower property owners and delight guests.
                        </p>

                        {/* Minimalist Corporate Patronage Banner with Highlighted Logo */}
                        <div className="pt-10 border-t border-stone-100 flex flex-col items-center justify-center space-y-4">
                            <span className="text-[11px] font-bold tracking-[0.25em] text-stone-400 uppercase">
                                Developed in academic & corporate collaboration with
                            </span>
                            <div className="flex items-center justify-center p-2 hover:scale-102 transition-transform duration-300">
                                <img 
                                    src="/commerzbank_logo.png" 
                                    alt="Commerzbank Logo" 
                                    className="h-16 md:h-20 object-contain mix-blend-multiply" 
                                />
                            </div>
                        </div>
                    </div>
                </div>

                {/* 1.1 Project Overview & Repository Context */}
                <div className="max-w-6xl mx-auto px-6 md:px-8 mt-16">
                    <div className="bg-white border-l-4 border-[#42211D] border-y border-r border-[#DACDCA] rounded-r-2xl p-8 md:p-10 shadow-sm space-y-6 text-left">
                        <div className="space-y-2">
                            <h2 className="text-xl md:text-2xl font-bold text-stone-900 tracking-tight">Project Context</h2>
                            <p className="text-xs md:text-sm text-[#42211D] font-bold tracking-wide">
                                Kompleksowy Webowy Asystent Terminarza, Energii, Rezerwacji i Administracji
                            </p>
                        </div>
                        <div className="text-stone-600 text-base leading-relaxed font-light space-y-4">
                            <p>
                                <strong>KWATERA</strong> is a web-based system for managing holiday accommodation bookings, availability, utility settlements, payments, reporting, and administration.
                            </p>
                            <p>
                                As a semester project, KWATERA focuses on building a realistic, production-ready accommodation management platform rather than a simple CRUD application. The Stage 3 repository implements actual business workflows, role-based controls, Stripe transaction processing, YOLOv8-assisted display boundary extraction, and AI models for nightly rate optimization.
                            </p>
                        </div>
                    </div>
                </div>

                <div className="max-w-6xl mx-auto px-6 md:px-8 space-y-24 mt-20">

                    {/* 2. The Architecture Section */}
                    <div className="space-y-8 bg-white border border-[#DACDCA] rounded-3xl p-8 md:p-12 shadow-sm">
                        <div className="max-w-3xl mx-auto text-center space-y-4">
                            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-stone-100 text-stone-600 text-xs font-semibold">
                                System Architecture
                            </span>
                            <h2 className="text-2xl md:text-3xl font-extrabold text-stone-900 tracking-tight">
                                Bridging innovation and hospitality
                            </h2>
                            <p className="text-stone-500 text-base leading-relaxed font-light">
                                Our system automates the rental lifecycle — from OCR-based utility readings to AI-driven dynamic pricing — all orchestrated within a robust microservices architecture.
                            </p>
                        </div>

                        {/* Clean Vector Architecture Diagram */}
                        <div className="w-full bg-stone-50/80 border border-[#DACDCA]/50 rounded-2xl p-6 md:p-10 flex flex-col items-center overflow-x-auto">
                            <div className="min-w-[700px] w-full max-w-4xl flex flex-col items-center py-4 relative">
                                
                                {/* React Frontend Node */}
                                <div className="z-10 bg-white border border-[#DACDCA] rounded-xl px-5 py-3 shadow-sm flex items-center gap-2.5 hover:border-[#42211D]/40 transition-colors">
                                    <Code className="w-5 h-5 text-[#42211D]" />
                                    <div className="text-left">
                                        <h4 className="text-sm font-bold text-stone-900 leading-none">React Client App</h4>
                                        <span className="text-[10px] text-stone-400">Vite • Tailwind CSS</span>
                                    </div>
                                </div>

                                {/* Down Arrow Line */}
                                <div className="h-8 w-px border-l-2 border-dashed border-stone-300 my-1"></div>

                                {/* API Gateway Node */}
                                <div className="z-10 bg-white border border-[#DACDCA] rounded-xl px-6 py-2.5 shadow-sm flex items-center gap-2.5 hover:border-[#42211D]/40 transition-colors">
                                    <Server className="w-4 h-4 text-[#42211D]" />
                                    <div className="text-left">
                                        <h4 className="text-sm font-bold text-stone-900 leading-none">API Gateway</h4>
                                        <span className="text-[9px] text-stone-400">Security & Routing</span>
                                    </div>
                                </div>

                                {/* Separator Line (Dashed vertical spacer below API Gateway to raise the Gateway above lines) */}
                                <div className="h-6 w-px border-l-2 border-dashed border-stone-300 my-1"></div>

                                {/* Branch Lines Container */}
                                <div className="w-full flex justify-between relative px-12 md:px-24">
                                    <div className="absolute top-0 left-12 md:left-24 right-12 md:right-24 h-4 border-t-2 border-dashed border-stone-300"></div>
                                    <div className="h-8 w-px border-l-2 border-dashed border-stone-300"></div>
                                    <div className="h-8 w-px border-l-2 border-dashed border-stone-300"></div>
                                    <div className="h-8 w-px border-l-2 border-dashed border-stone-300"></div>
                                </div>

                                {/* Bottom Microservices Row */}
                                <div className="w-full flex justify-between gap-4 px-2">
                                    
                                    {/* Microservice 1: Core Service */}
                                    <div className="flex-1 bg-white border border-[#DACDCA] rounded-xl p-4 shadow-sm flex flex-col items-center text-center space-y-2 hover:border-[#42211D]/30 transition-colors">
                                        <div className="p-1.5 rounded-lg bg-stone-100">
                                            <Database className="w-4 h-4 text-[#42211D]" />
                                        </div>
                                        <div>
                                            <h5 className="text-xs font-bold text-stone-900 leading-tight">Core & Property Service</h5>
                                            <p className="text-[9px] text-stone-400">Java / Spring Boot</p>
                                        </div>
                                    </div>

                                    {/* Microservice 2: AI Pricing Service */}
                                    <div className="flex-1 bg-white border border-[#DACDCA] rounded-xl p-4 shadow-sm flex flex-col items-center text-center space-y-2 hover:border-[#42211D]/30 transition-colors">
                                        <div className="p-1.5 rounded-lg bg-stone-100">
                                            <Brain className="w-4 h-4 text-[#42211D]" />
                                        </div>
                                        <div>
                                            <h5 className="text-xs font-bold text-stone-900 leading-tight">AI Dynamic Pricing</h5>
                                            <p className="text-[9px] text-stone-400">Python / ML Engine</p>
                                        </div>
                                    </div>

                                    {/* Microservice 3: OCR Billing Service */}
                                    <div className="flex-1 bg-white border border-[#DACDCA] rounded-xl p-4 shadow-sm flex flex-col items-center text-center space-y-2 hover:border-[#42211D]/30 transition-colors">
                                        <div className="p-1.5 rounded-lg bg-stone-100">
                                            <Scan className="w-4 h-4 text-[#42211D]" />
                                        </div>
                                        <div>
                                            <h5 className="text-xs font-bold text-stone-900 leading-tight">OCR Utility Billing</h5>
                                            <p className="text-[9px] text-stone-400">Python / YOLOv8</p>
                                        </div>
                                    </div>

                                </div>
                            </div>
                        </div>
                    </div>

                    {/* 3. Features & Tech Stack Section */}
                    <div className="space-y-10">
                        <div className="text-center space-y-2">
                            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-[#42211D]/5 text-[#42211D] text-xs font-semibold">
                                Core Features
                            </span>
                            <h2 className="text-3xl font-extrabold text-stone-900 tracking-tight">
                                Platform Features & Tech Stack
                            </h2>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                            
                            {/* Feature Card 1 */}
                            <div className="bg-white border border-[#DACDCA] rounded-2xl p-6 md:p-8 hover:shadow-md hover:-translate-y-0.5 transition-all duration-300">
                                <div className="w-10 h-10 rounded-xl bg-[#42211D]/5 flex items-center justify-center mb-5">
                                    <Brain className="w-5 h-5 text-[#42211D]" />
                                </div>
                                <h3 className="text-xl font-bold text-stone-900 mb-2">AI-Powered Dynamic Pricing</h3>
                                <p className="text-stone-500 text-base leading-relaxed font-light">
                                    Maximize revenue with our intelligent pricing engine. By analyzing market trends and property data, the system automatically adjusts daily rates for optimal profitability.
                                </p>
                            </div>

                            {/* Feature Card 2 */}
                            <div className="bg-white border border-[#DACDCA] rounded-2xl p-6 md:p-8 hover:shadow-md hover:-translate-y-0.5 transition-all duration-300">
                                <div className="w-10 h-10 rounded-xl bg-[#42211D]/5 flex items-center justify-center mb-5">
                                    <Scan className="w-5 h-5 text-[#42211D]" />
                                </div>
                                <h3 className="text-xl font-bold text-stone-900 mb-2">YOLO & OCR Utility Billing</h3>
                                <p className="text-stone-500 text-base leading-relaxed font-light">
                                    Eliminate manual meter readings. Our advanced OCR module utilizes image processing to detect and extract utility data from photos, streamlining your billing.
                                </p>
                            </div>

                            {/* Feature Card 3 */}
                            <div className="bg-white border border-[#DACDCA] rounded-2xl p-6 md:p-8 hover:shadow-md hover:-translate-y-0.5 transition-all duration-300">
                                <div className="w-10 h-10 rounded-xl bg-[#42211D]/5 flex items-center justify-center mb-5">
                                    <Calendar className="w-5 h-5 text-[#42211D]" />
                                </div>
                                <h3 className="text-xl font-bold text-stone-900 mb-2">Seamless Reservation Flow</h3>
                                <p className="text-stone-500 text-base leading-relaxed font-light">
                                    Deliver a frictionless booking experience. From browsing our property catalog to secure Stripe-integrated checkouts, the entire guest journey is optimized for conversion.
                                </p>
                            </div>

                            {/* Feature Card 4 */}
                            <div className="bg-white border border-[#DACDCA] rounded-2xl p-6 md:p-8 hover:shadow-md hover:-translate-y-0.5 transition-all duration-300">
                                <div className="w-10 h-10 rounded-xl bg-[#42211D]/5 flex items-center justify-center mb-5">
                                    <LayoutDashboard className="w-5 h-5 text-[#42211D]" />
                                </div>
                                <h3 className="text-xl font-bold text-stone-900 mb-2">Comprehensive Owner Dashboard</h3>
                                <p className="text-stone-500 text-base leading-relaxed font-light">
                                    Take full control. Manage units, track real-time occupancy, and monitor financial settlements from a centralized, professional command center.
                                </p>
                            </div>

                        </div>
                    </div>

                    {/* Tech Stack Sub-section */}
                    <div className="space-y-10 pt-4">
                        <div className="text-center space-y-2">
                            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-[#42211D]/5 text-[#42211D] text-xs font-semibold">
                                Core Technologies
                            </span>
                            <h2 className="text-xl md:text-2xl font-bold text-stone-900 tracking-tight">
                                System Technology Stack
                            </h2>
                        </div>
                        
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                            {techStack.map((stack) => (
                                <div key={stack.category} className="bg-white border border-[#DACDCA] rounded-2xl p-6 shadow-sm hover:shadow-md transition-all duration-300">
                                    <div className="flex items-center gap-3 border-b border-stone-100 pb-4 mb-4">
                                        {stack.icon}
                                        <h3 className="font-bold text-base text-stone-900">{stack.category}</h3>
                                    </div>
                                    <div className="flex flex-wrap gap-2">
                                        {stack.techs.map((tech) => (
                                            <span key={tech} className="px-3 py-1 bg-stone-100 border border-stone-200 text-stone-600 text-xs md:text-sm font-medium rounded-md">
                                                {tech}
                                            </span>
                                        ))}
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* 4. The Team Section */}
                    <div className="space-y-12 pb-16">
                        <div className="text-center space-y-2">
                            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-[#42211D]/5 text-[#42211D] text-xs font-semibold">
                                Project Creators
                            </span>
                            <h2 className="text-2xl md:text-3xl font-extrabold text-stone-900 tracking-tight">
                                Meet the Creators
                            </h2>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                            {teamMembers.map((member) => (
                                <div key={member.name} className="bg-white border border-[#DACDCA] rounded-2xl p-6 flex flex-col justify-between hover:shadow-md transition-all duration-305 hover:-translate-y-0.5">
                                    <div className="space-y-4">
                                        {/* Styled Avatar Placeholder */}
                                        <div className="flex justify-center">
                                            <div className={`w-16 h-16 rounded-full ${member.bgColor} flex items-center justify-center font-bold text-lg border border-[#DACDCA] shadow-inner`}>
                                                {member.initials}
                                            </div>
                                        </div>
                                        <div className="text-center space-y-2">
                                            <h4 className="font-bold text-[#42211D] text-lg leading-none">{member.name}</h4>
                                            
                                            {/* Symmetrical vertical alignment spaces */}
                                            <div className="h-12 flex flex-col justify-center">
                                                <p className="text-xs md:text-sm font-extrabold text-stone-850 uppercase tracking-wider leading-tight">
                                                    {member.role}
                                                </p>
                                            </div>
                                            <div className="h-8 flex items-center justify-center">
                                                <p className="text-[10px] md:text-xs text-stone-400 font-semibold leading-normal">
                                                    FTIMS • IOAD Specialization | Lodz University of Technology
                                                </p>
                                            </div>
                                        </div>
                                        
                                        {/* Symmetrical bio descriptions */}
                                        <div className="h-32 flex items-start justify-center overflow-hidden">
                                            <p className="text-stone-500 text-xs md:text-sm font-light text-center leading-relaxed">
                                                {member.bio}
                                            </p>
                                        </div>
                                    </div>
                                    
                                    <div className="flex justify-center gap-2.5 pt-4 border-t border-stone-100 mt-4">
                                        <a 
                                            href={member.github} 
                                            target="_blank" 
                                            rel="noopener noreferrer"
                                            className="p-1.5 rounded-full border border-stone-200 text-stone-400 hover:text-stone-900 hover:border-stone-900 transition-colors"
                                            aria-label={`${member.name} GitHub`}
                                        >
                                            <svg className="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                                <path d="M15 22v-4a4.8 4.8 0 0 0-1-3.5c3 0 6-2 6-5.5.08-1.25-.27-2.48-1-3.5.28-1.15.28-2.35 0-3.5 0 0-1 0-3 1.5-2.64-.5-5.36-.5-8 0C6 2 5 2 5 2c-.3 1.15-.3 2.35 0 3.5A5.403 5.403 0 0 0 4 9c0 3.5 3 5.5 6 5.5-.39.49-.68 1.05-.85 1.65-.17.6-.22 1.23-.15 1.85v4" />
                                                <path d="M9 18c-4.51 2-5-2-7-2" />
                                            </svg>
                                        </a>
                                        <a 
                                            href={member.linkedin} 
                                            target="_blank" 
                                            rel="noopener noreferrer"
                                            className="p-1.5 rounded-full border border-stone-200 text-stone-400 hover:text-blue-600 hover:border-blue-600 transition-colors"
                                            aria-label={`${member.name} LinkedIn`}
                                        >
                                            <svg className="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                                <path d="M16 8a6 6 0 0 1 6 6v7h-4v-7a2 2 0 0 0-2-2 2 2 0 0 0-2 2v7h-4v-7a6 6 0 0 1 6-6z" />
                                                <rect width="4" height="12" x="2" y="9" />
                                                <circle cx="4" cy="4" r="2" />
                                            </svg>
                                        </a>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>

                </div>
            </div>

            {/* Footer component */}
            <Footer />
        </div>
    );
}
