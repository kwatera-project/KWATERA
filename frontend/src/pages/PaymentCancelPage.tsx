export default function PaymentCancelPage() {
    return (
        <div className="min-h-screen flex flex-col items-center justify-center p-6">
            <h1 className="text-3xl font-bold text-red-600">
                Payment cancelled
            </h1>

            <p className="mt-4 text-gray-600">
                Your payment was cancelled. No charges were made.
            </p>

            <a
                href="/"
                className="mt-6 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
            >
                Back to homepage
            </a>
        </div>
    );
}