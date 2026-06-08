import { Brain, Scan, Cpu, Database, Code, Building, CheckCircle2 } from 'lucide-react';

interface TeamMember {
    name: string;
    role: string;
    focus: string;
    github: string;
    linkedin: string;
    initials: string;
    gradient: string;
}

export default function AboutPage() {
    const teamMembers: TeamMember[] = [
        {
            name: "Alicja Świercz",
            role: "Lorem Ipsum Dolor",
            focus: "Lorem ipsum dolor sit amet, consectetur adipiscing",
            github: "",
            linkedin: "",
            initials: "AŚ",
            gradient: "from-amber-500 to-rose-500"
        },
        {
            name: "Łukasz Jęcek",
            role: "Lorem Ipsum Dolor",
            focus: "Lorem ipsum dolor sit amet, consectetur adipiscing",
            github: "",
            linkedin: "",
            initials: "ŁJ",
            gradient: "from-blue-500 to-indigo-600"
        },
        {
            name: "Zuzanna Adamczyk",
            role: "Lorem Ipsum Dolor",
            focus: "Lorem ipsum dolor sit amet, consectetur adipiscing",
            github: "",
            linkedin: "",
            initials: "ZA",
            gradient: "from-purple-500 to-pink-500"
        },
        {
            name: "Nadzeya Silchankava",
            role: "Lorem Ipsum Dolor",
            focus: "Lorem ipsum dolor sit amet, consectetur adipiscing",
            github: "",
            linkedin: "",
            initials: "NS",
            gradient: "from-teal-400 to-emerald-600"
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
            techs: ["Java 17", "Spring Boot", "Spring Security & JWT", "PostgreSQL", "REST APIs"]
        },
        {
            category: "AI & OCR Models",
            icon: <Cpu className="w-6 h-6 text-brand-primary" />,
            techs: ["Python", "YOLOv8 Object Detection", "Tesseract OCR", "Dynamic Pricing Engine"]
        }
    ];

    return (
        <div className="w-full bg-[#F7F7F7] font-sans min-h-screen pb-20">
            {/* Hero Section */}
            <div className="relative overflow-hidden bg-gradient-to-br from-[#42211D] to-[#2D1614] text-white py-24 px-4">
                <div className="absolute inset-0 opacity-10 bg-[radial-gradient(circle_at_top_right,_var(--color-brand-accent)_0%,_transparent_60%)]"></div>
                <div className="max-w-6xl mx-auto text-center relative z-10 space-y-6">
                    <span className="inline-block px-3 py-1 text-xs font-bold tracking-[0.2em] uppercase bg-white/10 text-white rounded-full border border-white/20">
                        Lorem Ipsum
                    </span>
                    <h1 className="text-4xl md:text-6xl font-extrabold tracking-tight leading-none">
                        Lorem Ipsum Dolor Sit Amet
                    </h1>
                    <p className="text-lg md:text-xl text-stone-300 max-w-3xl mx-auto font-light leading-relaxed">
                        Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
                    </p>
                </div>
            </div>

            {/* Minimalist Partnership Logo Bar */}
            <div className="w-full bg-white border-b border-stone-200/60 py-10 px-4">
                <div className="max-w-6xl mx-auto flex flex-col items-center text-center space-y-4">
                    <p className="text-xs uppercase tracking-[0.25em] text-stone-500 font-bold flex items-center gap-1.5 justify-center">
                        <Building className="w-3.5 h-3.5" /> Developed in Academic & Corporate Collaboration With
                    </p>
                    <div className="flex items-center justify-center gap-3.5 text-stone-400 hover:text-[#FFCC00] transition-colors duration-300 group py-1">
                        <svg className="w-7 h-7 text-stone-400 group-hover:text-amber-500 transition-colors duration-300" viewBox="0 0 24 24" fill="currentColor">
                            {/* Stylized Mobius strip loop representing Commerzbank logo */}
                            <path d="M12 2L2 9.5h3.5v7h4v-7h5v7h4v-7H22L12 2zm1 14.5v-3h-2v3h2z" />
                        </svg>
                        <span className="text-stone-700 group-hover:text-stone-900 transition-colors duration-300 font-black tracking-widest text-xl">
                            COMMERZBANK
                        </span>
                    </div>
                    <p className="text-xs text-stone-400 max-w-xl mx-auto font-light">
                        Integrating corporate standards, security paradigms, and enterprise scalability practices into the core architecture of the KWATERA platform.
                    </p>
                </div>
            </div>

            <div className="max-w-6xl mx-auto px-4 md:px-8 mt-16 space-y-20">
                
                {/* Standout Features Section */}
                <div className="space-y-10">
                    <div className="text-center space-y-2">
                        <h2 className="text-3xl font-extrabold text-[#1A1A1A] tracking-tight">Lorem Ipsum Dolor</h2>
                        <p className="text-stone-500 text-sm max-w-2xl mx-auto font-light">
                            Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor.
                        </p>
                    </div>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                        {/* Dynamic Pricing card */}
                        <div className="bg-white border border-[#DACDCA] rounded-2xl p-8 hover:shadow-lg transition-all duration-300 transform hover:-translate-y-1">
                            <div className="w-12 h-12 rounded-xl bg-[#42211D]/10 flex items-center justify-center mb-6">
                                <Brain className="w-6 h-6 text-[#42211D]" />
                            </div>
                            <h3 className="text-xl font-bold text-[#1A1A1A] mb-3">AI-Powered Dynamic Pricing</h3>
                            <p className="text-stone-600 text-sm leading-relaxed font-light">
                                Lorem ipsum dolor sit amet, consectetur adipiscing elit. Cras interdum, nunc vel tempor eleifend, urna lectus efficitur nibh, a cursus erat augue vitae dolor.
                            </p>
                            <ul className="mt-5 space-y-2.5">
                                <li className="flex items-center gap-2 text-xs text-stone-500">
                                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500 shrink-0" /> Lorem ipsum dolor sit amet
                                </li>
                                <li className="flex items-center gap-2 text-xs text-stone-500">
                                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500 shrink-0" /> Consectetur adipiscing elit
                                </li>
                            </ul>
                        </div>

                        {/* YOLO/OCR utility billing card */}
                        <div className="bg-white border border-[#DACDCA] rounded-2xl p-8 hover:shadow-lg transition-all duration-300 transform hover:-translate-y-1">
                            <div className="w-12 h-12 rounded-xl bg-[#42211D]/10 flex items-center justify-center mb-6">
                                <Scan className="w-6 h-6 text-[#42211D]" />
                            </div>
                            <h3 className="text-xl font-bold text-[#1A1A1A] mb-3">YOLO & OCR Utility Billing</h3>
                            <p className="text-stone-600 text-sm leading-relaxed font-light">
                                Lorem ipsum dolor sit amet, consectetur adipiscing elit. Cras interdum, nunc vel tempor eleifend, urna lectus efficitur nibh, a cursus erat augue vitae dolor.
                            </p>
                            <ul className="mt-5 space-y-2.5">
                                <li className="flex items-center gap-2 text-xs text-stone-500">
                                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500 shrink-0" /> Lorem ipsum dolor sit amet
                                </li>
                                <li className="flex items-center gap-2 text-xs text-stone-500">
                                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500 shrink-0" /> Consectetur adipiscing elit
                                </li>
                            </ul>
                        </div>
                    </div>
                </div>

                {/* Tech Stack Section */}
                <div className="space-y-10">
                    <div className="text-center space-y-2">
                        <h2 className="text-3xl font-extrabold text-[#1A1A1A] tracking-tight">Our Technology Stack</h2>
                        <p className="text-stone-500 text-sm max-w-2xl mx-auto font-light">
                            Enterprise-grade technologies driving the KWATERA system.
                        </p>
                    </div>
                    
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                        {techStack.map((stack) => (
                            <div key={stack.category} className="bg-white border border-[#DACDCA] rounded-2xl p-6 shadow-sm hover:shadow-md transition-all duration-300">
                                <div className="flex items-center gap-3 border-b border-stone-100 pb-4 mb-4">
                                    {stack.icon}
                                    <h3 className="font-bold text-lg text-[#1A1A1A]">{stack.category}</h3>
                                </div>
                                <div className="flex flex-wrap gap-2">
                                    {stack.techs.map((tech) => (
                                        <span key={tech} className="px-3 py-1 bg-stone-100 border border-stone-200 text-stone-700 text-xs font-semibold rounded-full">
                                            {tech}
                                        </span>
                                    ))}
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Team Info Section */}
                <div className="space-y-10">
                    <div className="text-center max-w-3xl mx-auto space-y-3">
                        <h2 className="text-3xl font-extrabold text-[#1A1A1A] tracking-tight">Lorem Ipsum</h2>
                        <p className="text-stone-500 text-sm md:text-base leading-relaxed font-light">
                            Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation.
                        </p>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                        {teamMembers.map((member) => (
                            <div key={member.name} className="bg-white border border-[#DACDCA] rounded-2xl overflow-hidden shadow-sm hover:shadow-lg transition-all duration-300 flex flex-col justify-between group">
                                <div>
                                    {/* Header colored splash */}
                                    <div className={`h-20 bg-gradient-to-r ${member.gradient} relative flex items-center justify-center`}>
                                        <div className="absolute -bottom-8 w-16 h-16 rounded-full border-4 border-white bg-stone-100 shadow-md flex items-center justify-center font-extrabold text-stone-800 text-lg">
                                            {member.initials}
                                        </div>
                                    </div>
                                    <div className="pt-10 pb-4 px-4 text-center space-y-2">
                                        <h4 className="font-bold text-base text-[#1A1A1A] group-hover:text-[#42211D] transition-colors">
                                            {member.name}
                                        </h4>
                                        <div className="space-y-1">
                                            <p className="text-[13px] text-[#42211D] font-bold leading-tight">
                                                {member.role}
                                            </p>
                                            <p className="text-[11px] text-stone-400 italic leading-snug">
                                                {member.focus}
                                            </p>
                                        </div>
                                    </div>
                                </div>
                                <div className="pb-6 px-4 text-center space-y-3">
                                    <div className="text-[10px] text-stone-400 bg-stone-50 py-1.5 px-3 rounded-lg inline-block border border-stone-100 font-medium">
                                        Lorem Ipsum Dolor Sit Amet
                                    </div>
                                    <div className="flex justify-center gap-3 pt-1">
                                        <a 
                                            href={member.github} 
                                            target="_blank" 
                                            rel="noopener noreferrer"
                                            className="p-2 rounded-full border border-stone-200 text-stone-500 hover:text-black hover:border-black transition-all bg-white hover:bg-stone-50"
                                            aria-label={`${member.name} Github`}
                                        >
                                            <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                                <path d="M15 22v-4a4.8 4.8 0 0 0-1-3.5c3 0 6-2 6-5.5.08-1.25-.27-2.48-1-3.5.28-1.15.28-2.35 0-3.5 0 0-1 0-3 1.5-2.64-.5-5.36-.5-8 0C6 2 5 2 5 2c-.3 1.15-.3 2.35 0 3.5A5.403 5.403 0 0 0 4 9c0 3.5 3 5.5 6 5.5-.39.49-.68 1.05-.85 1.65-.17.6-.22 1.23-.15 1.85v4" />
                                                <path d="M9 18c-4.51 2-5-2-7-2" />
                                            </svg>
                                        </a>
                                        <a 
                                            href={member.linkedin} 
                                            target="_blank" 
                                            rel="noopener noreferrer"
                                            className="p-2 rounded-full border border-stone-200 text-stone-500 hover:text-blue-600 hover:border-blue-600 transition-all bg-white hover:bg-stone-50"
                                            aria-label={`${member.name} LinkedIn`}
                                        >
                                            <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                                <path d="M16 8a6 6 0 0 1 6 6v7h-4v-7a2 2 0 0 0-2-2 2 2 0 0 0-2 2v7h-4v-7a6 6 0 0 1 6-6z" />
                                                <rect width="4" height="12" x="2" y="9" />
                                                <circle cx="4" cy="4" r="2" />
                                            </svg>
                                        </a>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

            </div>
        </div>
    );
}
