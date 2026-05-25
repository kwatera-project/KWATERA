import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getMyReservations } from '../api/reservationApi';
import type {GuestReservation} from '../types/reservation';
import { useCurrency } from '../contexts/CurrencyContext';

export default function MyReservationsPage() {
    const [reservations, setReservations] = useState<GuestReservation[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const { currency } = useCurrency();

    useEffect(() => {
        getMyReservations(currency)
            .then(data => {
                setReservations(data);
                setLoading(false);
            })
            .catch(err => {
                setError(err.message);
                setLoading(false);
            });
    }, [currency]);

    if (loading) return <div className="p-8 text-center text-gray-500">Loading your reservations...</div>;
    if (error) return <div className="p-8 text-center text-red-600">Error: {error}</div>;

    return (
        <div className="p-8 max-w-6xl mx-auto">
            <h1 className="text-3xl font-bold mb-6 text-title">My Reservations</h1>

            {reservations.length === 0 ? (
                <div className="bg-white p-8 rounded-lg shadow-sm border border-gray-200 text-center">
                    <p className="text-gray-500 mb-4">You don't have any reservations yet.</p>
                    <Link to="/catalog" className="text-blue-600 hover:underline">
                        Browse Catalog
                    </Link>
                </div>
            ) : (
                <div className="grid gap-4">
                    {reservations.map(res => (
                        <div key={res.id} className="bg-white p-6 rounded-lg shadow-sm border border-gray-200 flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
                            <div>
                                <div className="text-sm text-gray-500 mb-1">Reservation ID: <span className="font-mono">{res.id}</span></div>
                                <div className="font-medium mb-2">Unit ID: <span className="font-mono font-normal text-sm">{res.unitId}</span></div>
                                <div className="text-sm text-gray-600">
                                    {res.startDate} to {res.endDate}
                                </div>
                                {res.convertedTotalPrice && res.currencyInfo && (
                                    <div className="text-sm font-semibold mt-2">
                                        Total Price: {res.convertedTotalPrice.toFixed(2)} {res.currencyInfo.displayCurrency}
                                    </div>
                                )}
                            </div>

                            <div className="flex flex-col items-end gap-4 w-full md:w-auto">
                                <span className={`px-2 py-1 rounded text-sm font-bold ${
                                    res.status === 'CONFIRMED' ? 'bg-green-100 text-green-800' :
                                        res.status === 'PENDING' ? 'bg-yellow-100 text-yellow-800' :
                                            res.status === 'COMPLETED' ? 'bg-blue-100 text-blue-800' :
                                                'bg-gray-100 text-gray-800'
                                }`}>
                                    {res.status}
                                </span>

                                <div className="flex gap-4">
                                    <Link
                                        to={`/settlements/${res.id}`}
                                        className="text-blue-600 hover:underline text-sm"
                                    >
                                        View Bill
                                    </Link>
                                    <Link
                                        to={`/reservations/${res.id}`}
                                        className="text-blue-600 hover:underline text-sm"
                                    >
                                        View Details
                                    </Link>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}