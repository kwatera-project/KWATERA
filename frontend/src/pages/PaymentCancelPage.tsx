import {useTranslation} from "react-i18next"
export default function PaymentCancelPage() {
    const {t} = useTranslation();

    return (
        <div className="min-h-screen flex flex-col items-center justify-center p-8 text-[#1A1A1A]">
            <div className="bg-white border border-[#DACDCA] rounded-xl shadow-sm p-10 text-center max-w-md w-full space-y-6 hover:shadow-md transition-all duration-300">
                <div className="w-16 h-16 bg-red-50 border border-red-100 rounded-full flex items-center justify-center mx-auto shadow-sm">
                    <svg className="w-8 h-8 text-red-500" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                    </svg>
                </div>
                
                <div className="space-y-2">
                    <h1 className="text-3xl font-bold text-[#1A1A1A] tracking-tight">
                        {t('paymentCancel.title')}
                    </h1>
                    <p className="text-sm text-[#7A7A7A] font-medium leading-relaxed">
                        {t('paymentCancel.subtitle')}
                    </p>
                </div>

                <a
                    href="/"
                    className="inline-block w-full px-6 py-3 bg-[#42211D] text-white font-bold hover:bg-[#2a1412] text-sm rounded-lg transition-colors border border-[#DACDCA] shadow-sm text-center cursor-pointer"
                >
                    {t('paymentCancel.backHome')}
                </a>
            </div>
        </div>
    );
}