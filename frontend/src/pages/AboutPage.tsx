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
    image?: string;
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
            bgColor: "bg-brand-accent text-brand-primary",
            image: "https://media.licdn.com/dms/image/v2/D4D03AQG-Nzan5fxIqw/profile-displayphoto-scale_400_400/B4DZypIThDH4Ag-/0/1772364056913?e=1782345600&v=beta&t=dJv1F7PmYzt7IprARrV9NEoCF1wkc61bViDR03qoOvw"
        },
        {
            name: "Łukasz Jęcek",
            role: "Tech Lead & System Architect",
            bio: "Orchestrated the microservices architecture, CI/CD pipelines, and system integration. Established robust coding standards to ensure platform stability and scalability.",
            github: "https://github.com/lukaszjecek",
            linkedin: "https://www.linkedin.com/in/lukasz-jecek",
            initials: "ŁJ",
            bgColor: "bg-brand-accent text-brand-primary",
            image: "https://media.licdn.com/dms/image/v2/D4D03AQEPl_ZjzTMrLQ/profile-displayphoto-scale_400_400/B4DZ1GO2n_IYAg-/0/1774999796059?e=1782345600&v=beta&t=3reSXKEcy20lX6dwpn2-rIF-JkXbUkgjRjJxqnErcFc"
        },
        {
            name: "Nadzeya Silchankava",
            role: "Backend Domain & Payments Engineer",
            bio: "Architected the foundational backend domain, focusing on billing microservices and Stripe payment integration, enabling a full-cycle rental administration.",
            github: "https://github.com/sinadzeya",
            linkedin: "https://www.linkedin.com/in/nadzeya-silchankava/",
            initials: "NS",
            bgColor: "bg-brand-accent text-brand-primary",
            image: "https://media.licdn.com/dms/image/v2/D4D03AQE_aChgY43qbQ/profile-displayphoto-scale_400_400/B4DZ0ptqeLI0Ag-/0/1774521351351?e=1782345600&v=beta&t=yoD_W1SRPieWYGMuWUGMFiBmgvMtRTXnZAWd2qlsCMI"
        },
        {
            name: "Alicja Świercz",
            role: "Reservation Flow & UX Engineer",
            bio: "Spearheaded the end-to-end reservation lifecycle and crafted the frontend user experience. Designed intuitive dashboards and secure checkout flows, ensuring a seamless journey for all users.",
            github: "https://github.com/alicjaswiers",
            linkedin: "https://www.linkedin.com/in/alicjaswiers/",
            initials: "AŚ",
            bgColor: "bg-brand-accent text-brand-primary",
            image: "https://media.licdn.com/dms/image/v2/D4D03AQF3mQWeHKZC0Q/profile-displayphoto-scale_400_400/B4DZ6o31UOH4Ag-/0/1780949670275?e=1782345600&v=beta&t=EwpL05Qu1k0ab5Uf5Ws1koAIyYo-wgEIZw2YtL9d2G0"
        }
    ];

    const techStack = [
        {
            category: "Frontend",
            icon: <Code className="w-6 h-6 text-brand-primary" />,
            techs: ["React 19", "TypeScript", "TailwindCSS v4", "Vite", "React Router 6"]
        },
        {
            category: "Backend Services",
            icon: <Database className="w-6 h-6 text-brand-primary" />,
            techs: ["Java 25", "Spring Boot", "Spring Security & JWT", "PostgreSQL", "REST APIs"]
        },
        {
            category: "AI & OCR Models",
            icon: <Brain className="w-6 h-6 text-brand-primary" />,
            techs: ["Python", "YOLOv8 Object Detection", "Tesseract OCR", "Dynamic Pricing Engine"]
        }
    ];

    return (
        <div className="w-full bg-white font-sans min-h-screen -mt-8 text-brand-main antialiased selection:bg-brand-primary/10 selection:text-brand-primary flex flex-col justify-between">
            
            <div className="w-full flex-grow">

                <div className="bg-white border-b border-brand-accent py-24 px-6 text-center">
                    <div className="max-w-4xl mx-auto space-y-8">
                        <span className="inline-flex items-center gap-1.5 px-3 py-1 text-xs font-bold tracking-[0.2em] uppercase text-brand-primary bg-brand-primary/5 rounded-full">
                            About Kwatera
                        </span>
                        <h1 className="text-4xl md:text-6xl font-extrabold text-brand-main tracking-tight leading-tight">
                            KWATERA: Next-Generation Vacation Rental Management
                        </h1>
                        <p className="text-lg md:text-xl text-brand-muted max-w-3xl mx-auto font-light leading-relaxed">
                            An all-in-one platform integrating intelligent utility billing, dynamic AI pricing, and seamless reservation flows to empower property owners and delight guests.
                        </p>

                        <div className="pt-10 border-t border-brand-accent/40 flex flex-col items-center justify-center space-y-4">
                            <span className="text-[11px] font-bold tracking-[0.25em] text-brand-muted uppercase">
                                Developed in academic & corporate collaboration with
                            </span>
                            <a 
                                href="https://lodz.commerzbank.pl" 
                                target="_blank" 
                                rel="noopener noreferrer"
                                className="flex items-center justify-center p-2 hover:scale-102 transition-transform duration-300"
                            >
                                <img 
                                    src="/commerzbank_logo.png" 
                                    alt="Commerzbank Logo" 
                                    className="h-16 md:h-20 object-contain mix-blend-multiply" 
                                />
                            </a>
                        </div>
                    </div>
                </div>

                <div className="max-w-6xl mx-auto px-6 md:px-8 mt-16">
                    <div className="bg-white border-l-4 border-brand-primary border-y border-r border-brand-accent rounded-r-2xl p-8 md:p-10 shadow-sm space-y-6 text-left">
                        <div className="space-y-2">
                            <h2 className="text-xl md:text-2xl font-bold text-brand-main tracking-tight">Project Context</h2>
                            <p className="text-xs md:text-sm text-brand-primary font-bold tracking-wide">
                                Kompleksowy Webowy Asystent Terminarza, Energii, Rezerwacji i Administracji
                            </p>
                        </div>
                        <div className="text-brand-muted text-base leading-relaxed font-light space-y-4">
                            <p>
                                <strong>KWATERA</strong> is a web-based system for managing holiday accommodation bookings, availability, utility settlements, payments, reporting, and administration.
                            </p>
                            <p>
                                As a semester project, KWATERA focuses on building a realistic, production-ready accommodation management platform rather than a simple CRUD application.
                            </p>
                        </div>
                    </div>
                </div>

                <div className="bg-brand-bg py-20 border-y border-brand-accent/40 mt-20">
                    <div className="max-w-6xl mx-auto px-6 md:px-8 space-y-24">

                        <div className="space-y-8 bg-white border border-brand-accent rounded-3xl p-8 md:p-12 shadow-sm">
                            <div className="max-w-3xl mx-auto text-center space-y-4">
                                <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-brand-bg text-brand-muted text-xs font-semibold">
                                    System Architecture
                                </span>
                                <h2 className="text-2xl md:text-3xl font-extrabold text-brand-main tracking-tight">
                                    Bridging innovation and hospitality
                                </h2>
                                <p className="text-brand-muted text-base leading-relaxed font-light">
                                    Our system automates the rental lifecycle - from OCR-based utility readings to AI-driven dynamic pricing - all orchestrated within a robust microservices architecture.
                                </p>
                            </div>

                            <div className="w-full bg-brand-bg/80 border border-brand-accent/50 rounded-2xl p-6 md:p-10 flex flex-col items-center overflow-x-auto">
                                <div className="min-w-[700px] w-full max-w-4xl flex flex-col items-center py-4 relative">

                                    <div className="z-10 bg-white border border-brand-accent rounded-xl px-5 py-3 shadow-sm flex items-center gap-2.5 hover:border-brand-primary/40 transition-colors">
                                        <Code className="w-5 h-5 text-brand-primary" />
                                        <div className="text-left">
                                            <h4 className="text-sm font-bold text-brand-main leading-none">React Client App</h4>
                                            <span className="text-[10px] text-brand-muted">Vite • Tailwind CSS</span>
                                        </div>
                                    </div>

                                    <div className="h-8 w-px border-l-2 border-dashed border-brand-accent my-1"></div>

                                    <div className="z-10 bg-white border border-brand-accent rounded-xl px-6 py-2.5 shadow-sm flex items-center gap-2.5 hover:border-brand-primary/40 transition-colors">
                                        <Server className="w-4 h-4 text-brand-primary" />
                                        <div className="text-left">
                                            <h4 className="text-sm font-bold text-brand-main leading-none">API Gateway</h4>
                                            <span className="text-[9px] text-brand-muted">Security & Routing</span>
                                        </div>
                                    </div>

                                    <div className="h-6 w-px border-l-2 border-dashed border-brand-accent my-1"></div>

                                    <div className="w-full flex justify-between relative px-12 md:px-24">
                                        <div className="absolute top-0 left-12 md:left-24 right-12 md:right-24 h-4 border-t-2 border-dashed border-brand-accent"></div>
                                        <div className="h-8 w-px border-l-2 border-dashed border-brand-accent"></div>
                                        <div className="h-8 w-px border-l-2 border-dashed border-brand-accent"></div>
                                        <div className="h-8 w-px border-l-2 border-dashed border-brand-accent"></div>
                                    </div>

                                    <div className="w-full flex justify-between gap-4 px-2">

                                        <div className="flex-1 bg-white border border-brand-accent rounded-xl p-4 shadow-sm flex flex-col items-center text-center space-y-2 hover:border-brand-primary/30 transition-colors">
                                            <div className="p-1.5 rounded-lg bg-brand-bg">
                                                <Database className="w-4 h-4 text-brand-primary" />
                                            </div>
                                            <div>
                                                <h5 className="text-xs font-bold text-brand-main leading-tight">Core & Property Service</h5>
                                                <p className="text-[9px] text-brand-muted">Java / Spring Boot</p>
                                            </div>
                                        </div>

                                        <div className="flex-1 bg-white border border-brand-accent rounded-xl p-4 shadow-sm flex flex-col items-center text-center space-y-2 hover:border-brand-primary/30 transition-colors">
                                            <div className="p-1.5 rounded-lg bg-brand-bg">
                                                <Brain className="w-4 h-4 text-brand-primary" />
                                            </div>
                                            <div>
                                                <h5 className="text-xs font-bold text-brand-main leading-tight">AI Dynamic Pricing</h5>
                                                <p className="text-[9px] text-brand-muted">Python / ML Engine</p>
                                            </div>
                                        </div>

                                        <div className="flex-1 bg-white border border-brand-accent rounded-xl p-4 shadow-sm flex flex-col items-center text-center space-y-2 hover:border-brand-primary/30 transition-colors">
                                            <div className="p-1.5 rounded-lg bg-brand-bg">
                                                <Scan className="w-4 h-4 text-brand-primary" />
                                            </div>
                                            <div>
                                                <h5 className="text-xs font-bold text-brand-main leading-tight">OCR Utility Billing</h5>
                                                <p className="text-[9px] text-brand-muted">Python / YOLOv8</p>
                                            </div>
                                        </div>

                                    </div>
                                </div>
                            </div>
                        </div>

                        <div className="space-y-10">
                            <div className="text-center space-y-2">
                                <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-white text-brand-primary border border-brand-accent/50 text-xs font-semibold">
                                    Core Features
                                </span>
                                <h2 className="text-3xl font-extrabold text-brand-main tracking-tight">
                                    Platform Features & Tech Stack
                                </h2>
                            </div>

                            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">

                                <div className="bg-white border border-brand-accent rounded-2xl p-6 md:p-8 hover:shadow-md hover:-translate-y-0.5 transition-all duration-300">
                                    <div className="w-10 h-10 rounded-xl bg-brand-accent/30 flex items-center justify-center mb-5">
                                        <Brain className="w-5 h-5 text-brand-primary" />
                                    </div>
                                    <h3 className="text-xl font-bold text-brand-main mb-2">AI-Powered Dynamic Pricing</h3>
                                    <p className="text-brand-muted text-base leading-relaxed font-light">
                                        Maximize revenue with our intelligent pricing engine. By analyzing market trends and property data, the system automatically adjusts daily rates for optimal profitability.
                                    </p>
                                </div>

                                <div className="bg-white border border-brand-accent rounded-2xl p-6 md:p-8 hover:shadow-md hover:-translate-y-0.5 transition-all duration-300">
                                    <div className="w-10 h-10 rounded-xl bg-brand-accent/30 flex items-center justify-center mb-5">
                                        <Scan className="w-5 h-5 text-brand-primary" />
                                    </div>
                                    <h3 className="text-xl font-bold text-brand-main mb-2">YOLO & OCR Utility Billing</h3>
                                    <p className="text-brand-muted text-base leading-relaxed font-light">
                                        Eliminate manual meter readings. Our advanced OCR module utilizes image processing to detect and extract utility data from photos, streamlining your billing.
                                    </p>
                                </div>

                                <div className="bg-white border border-brand-accent rounded-2xl p-6 md:p-8 hover:shadow-md hover:-translate-y-0.5 transition-all duration-300">
                                    <div className="w-10 h-10 rounded-xl bg-brand-accent/30 flex items-center justify-center mb-5">
                                        <Calendar className="w-5 h-5 text-brand-primary" />
                                    </div>
                                    <h3 className="text-xl font-bold text-brand-main mb-2">Seamless Reservation Flow</h3>
                                    <p className="text-brand-muted text-base leading-relaxed font-light">
                                        Deliver a frictionless booking experience. From browsing our property catalog to secure Stripe-integrated checkouts, the entire guest journey is optimized for conversion.
                                    </p>
                                </div>

                                <div className="bg-white border border-brand-accent rounded-2xl p-6 md:p-8 hover:shadow-md hover:-translate-y-0.5 transition-all duration-300">
                                    <div className="w-10 h-10 rounded-xl bg-brand-accent/30 flex items-center justify-center mb-5">
                                        <LayoutDashboard className="w-5 h-5 text-brand-primary" />
                                    </div>
                                    <h3 className="text-xl font-bold text-brand-main mb-2">Comprehensive Owner Dashboard</h3>
                                    <p className="text-brand-muted text-base leading-relaxed font-light">
                                        Take full control. Manage units, track real-time occupancy, and monitor financial settlements from a centralized, professional command center.
                                    </p>
                                </div>

                            </div>
                        </div>

                        <div className="space-y-10 pt-4">
                            <div className="text-center space-y-2">
                                <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-white text-brand-primary border border-brand-accent/50 text-xs font-semibold">
                                    Core Technologies
                                </span>
                                <h2 className="text-xl md:text-2xl font-bold text-brand-main tracking-tight">
                                    System Technology Stack
                                </h2>
                            </div>
                            
                            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                                {techStack.map((stack) => (
                                    <div key={stack.category} className="bg-white border border-brand-accent rounded-2xl p-6 shadow-sm hover:shadow-md transition-all duration-300">
                                        <div className="flex items-center gap-3 border-b border-brand-accent/30 pb-4 mb-4">
                                            {stack.icon}
                                            <h3 className="font-bold text-base text-brand-main">{stack.category}</h3>
                                        </div>
                                        <div className="flex flex-wrap gap-2">
                                            {stack.techs.map((tech) => (
                                                <span key={tech} className="px-3 py-1 bg-brand-bg border border-brand-accent/40 text-brand-muted text-xs md:text-sm font-medium rounded-md">
                                                    {tech}
                                                </span>
                                            ))}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>

                    </div>
                </div>

                <div className="max-w-6xl mx-auto px-6 md:px-8 py-24 space-y-12">
                    <div className="text-center space-y-2">
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-brand-primary/5 text-brand-primary text-xs font-semibold">
                            Project Creators
                        </span>
                        <h2 className="text-2xl md:text-3xl font-extrabold text-brand-main tracking-tight">
                            Meet the Creators
                        </h2>
                        <p className="text-brand-muted text-sm md:text-base leading-relaxed font-light max-w-3xl mx-auto">
                            KWATERA was conceptualized and developed as part of our academic degree program in 
                            <span className="font-semibold text-brand-main"> Applied Computer Science</span> at the 
                            <span className="font-semibold text-brand-main"> Faculty of Technical Physics, Information Technology and Applied Mathematics (FTIMS)</span>, 
                            <span className="font-semibold text-brand-main"> Lodz University of Technology</span>.
                        </p>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                        {teamMembers.map((member) => (
                            <div key={member.name} className="bg-white border border-brand-accent rounded-2xl p-6 flex flex-col justify-between hover:shadow-md transition-all duration-305 hover:-translate-y-0.5">
                                <div className="space-y-4">
                                    <div className="flex justify-center">
                                        {member.image ? (
                                            <div className="w-24 h-24 rounded-full overflow-hidden border border-brand-accent shadow-inner">
                                                <img 
                                                    src={member.image} 
                                                    alt={member.name} 
                                                    className="w-full h-full object-cover scale-105"
                                                />
                                            </div>
                                        ) : (
                                            <div className={`w-24 h-24 rounded-full ${member.bgColor} flex items-center justify-center font-bold text-xl border border-brand-accent shadow-inner`}>
                                                {member.initials}
                                            </div>
                                        )}
                                    </div>
                                    <div className="text-center space-y-2">
                                        <h4 className="font-bold text-brand-primary text-lg leading-none">{member.name}</h4>

                                        <div className="h-12 flex flex-col justify-center">
                                            <p className="text-xs md:text-sm font-extrabold text-brand-main uppercase tracking-wider leading-tight">
                                                {member.role}
                                            </p>
                                        </div>
                                        <div className="h-6 flex items-center justify-center">
                                            <p className="text-[10px] md:text-xs text-brand-muted font-semibold leading-normal">
                                                FTIMS | Lodz University of Technology
                                            </p>
                                        </div>
                                    </div>

                                    <p className="text-brand-muted text-xs md:text-sm font-light text-center leading-relaxed">
                                        {member.bio}
                                    </p>
                                </div>
                                
                                <div className="flex justify-center gap-2.5 pt-4 border-t border-brand-accent/30 mt-4">
                                    <a 
                                        href={member.github} 
                                        target="_blank" 
                                        rel="noopener noreferrer"
                                        className="p-1.5 rounded-full border border-brand-accent text-brand-muted hover:text-brand-main hover:border-brand-primary transition-colors"
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
                                        className="p-1.5 rounded-full border border-brand-accent text-brand-muted hover:text-blue-600 hover:border-brand-primary transition-colors"
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
            <Footer />
        </div>
    );
}
