-- Issue #196: deterministic presentation data for dashboards, occupancy, billing and OCR.
--
-- The seed covers 2025-06-01 through 2026-12-31. Dates are intentionally fixed:
-- this migration must produce the same business history regardless of when Flyway runs.
--
-- Regional assumptions:
--   * seaside and lake units are concentrated in June-September, especially July/August;
--   * mountain units have both summer and winter peaks;
--   * city units receive shorter, steadier weekend and mid-week stays.
--
-- Existing reservations from earlier migrations are treated as occupied even when cancelled.
-- This keeps the occupancy calendar visually clean and avoids relying on generated UUIDs.
--
-- MediaReadingService considers WATER consumption normal when it is between 20% and
-- 300% of capacity * stay_nights * 0.1 m3. This seed uses 85%-115% of that expected
-- value for every non-cancelled stay, keeping the normal path safely auto-approved.

CREATE TEMP TABLE seed_196_reservations ON COMMIT DROP AS
WITH inventory(property_title, unit_name, market, unit_order) AS (
    VALUES
        ('Sunset Apartments',          'Studio 1',                    'CITY',      1),
        ('Sunset Apartments',          'Apartment Deluxe',            'CITY',      2),
        ('Mountain View Lodge',        'Room 1',                      'MOUNTAIN',  3),
        ('Mountain View Lodge',        'Family Suite',                'MOUNTAIN',  4),
        ('Forest Retreat',             'Entire Forest Cabin',         'MOUNTAIN',  5),
        ('Mountain Escape',            'Entire Mountain Cabin',       'MOUNTAIN',  6),
        ('Evergreen Cabin',            'Entire Nature Cabin',         'MOUNTAIN',  7),
        ('Spruce Hideaway',            'Attic Room',                  'MOUNTAIN',  8),
        ('Masurian Forest Lodge',      'Entire Lakeside Cottage',     'LAKE',      9),
        ('Gdansk City Apartment',      'City Center Apartment',       'SEASIDE',  10),
        ('Gdansk City Apartment',      'Premium City Apartment',      'SEASIDE',  11),
        ('Krakow Old Town Apartment',  'Historic Center Apartment',  'CITY',     12),
        ('Krakow Old Town Apartment',  'Old Town Comfort Apartment',  'CITY',     13),
        ('Riverside Apartment',        'Odra View Apartment',         'CITY',     14),
        ('Riverside Apartment',        'Riverside Deluxe Apartment',  'CITY',     15),
        ('Seaside Apartment',          'Beachside Apartment',         'SEASIDE',  16),
        ('Seaside Apartment',          'Seaside Luxury Apartment',    'SEASIDE',  17),
        ('Business Class Apartment',   'Executive Apartment',         'CITY',     18),
        ('Business Class Apartment',   'Executive Plus Apartment',    'CITY',     19)
),
schedule(market, slot_no, base_start, nights, price_multiplier) AS (
    VALUES
        -- Baltic coast: dense summer turns, shoulder-season city breaks.
        ('SEASIDE',  1, DATE '2025-06-20',  7, 1.18::numeric),
        ('SEASIDE',  2, DATE '2025-07-03', 10, 1.35::numeric),
        ('SEASIDE',  3, DATE '2025-07-20',  9, 1.35::numeric),
        ('SEASIDE',  4, DATE '2025-08-05', 10, 1.35::numeric),
        ('SEASIDE',  5, DATE '2025-08-21',  8, 1.32::numeric),
        ('SEASIDE',  6, DATE '2025-09-05',  6, 1.18::numeric),
        ('SEASIDE',  7, DATE '2025-11-14',  3, 0.88::numeric),
        ('SEASIDE',  8, DATE '2026-05-22',  4, 1.05::numeric),
        ('SEASIDE',  9, DATE '2026-06-19',  8, 1.18::numeric),
        ('SEASIDE', 10, DATE '2026-07-08', 12, 1.35::numeric),
        ('SEASIDE', 11, DATE '2026-08-03', 12, 1.35::numeric),
        ('SEASIDE', 12, DATE '2026-09-02',  7, 1.18::numeric),

        -- Masurian lake: highly seasonal, with long summer family stays.
        ('LAKE',  1, DATE '2025-06-14',  7, 1.15::numeric),
        ('LAKE',  2, DATE '2025-07-02', 10, 1.30::numeric),
        ('LAKE',  3, DATE '2025-07-18', 11, 1.30::numeric),
        ('LAKE',  4, DATE '2025-08-05', 12, 1.30::numeric),
        ('LAKE',  5, DATE '2025-08-22',  8, 1.28::numeric),
        ('LAKE',  6, DATE '2025-09-06',  7, 1.15::numeric),
        ('LAKE',  7, DATE '2026-05-29',  6, 1.05::numeric),
        ('LAKE',  8, DATE '2026-06-18',  9, 1.15::numeric),
        ('LAKE',  9, DATE '2026-07-08', 12, 1.30::numeric),
        ('LAKE', 10, DATE '2026-07-25', 10, 1.30::numeric),
        ('LAKE', 11, DATE '2026-08-12', 12, 1.30::numeric),
        ('LAKE', 12, DATE '2026-09-05',  7, 1.15::numeric),

        -- Mountains: summer hiking plus Christmas, New Year and winter holidays.
        ('MOUNTAIN',  1, DATE '2025-07-05',  8, 1.18::numeric),
        ('MOUNTAIN',  2, DATE '2025-08-02', 10, 1.18::numeric),
        ('MOUNTAIN',  3, DATE '2025-12-19',  8, 1.28::numeric),
        ('MOUNTAIN',  4, DATE '2026-01-09',  7, 1.28::numeric),
        ('MOUNTAIN',  5, DATE '2026-02-07',  7, 1.28::numeric),
        ('MOUNTAIN',  6, DATE '2026-04-17',  4, 0.95::numeric),
        ('MOUNTAIN',  7, DATE '2026-07-04',  9, 1.18::numeric),
        ('MOUNTAIN',  8, DATE '2026-08-10',  8, 1.18::numeric),
        ('MOUNTAIN',  9, DATE '2026-10-09',  4, 0.95::numeric),
        ('MOUNTAIN', 10, DATE '2026-11-20',  4, 1.05::numeric),
        ('MOUNTAIN', 11, DATE '2026-12-18',  8, 1.28::numeric),

        -- Cities: short weekends and occasional business-like mid-week stays.
        ('CITY',  1, DATE '2025-06-12', 3, 1.03::numeric),
        ('CITY',  2, DATE '2025-08-08', 3, 1.08::numeric),
        ('CITY',  3, DATE '2025-10-06', 3, 1.00::numeric),
        ('CITY',  4, DATE '2025-12-05', 3, 1.08::numeric),
        ('CITY',  5, DATE '2026-02-13', 3, 1.00::numeric),
        ('CITY',  6, DATE '2026-04-20', 3, 1.00::numeric),
        ('CITY',  7, DATE '2026-06-05', 3, 1.03::numeric),
        ('CITY',  8, DATE '2026-08-14', 4, 1.08::numeric),
        ('CITY',  9, DATE '2026-09-21', 3, 1.03::numeric),
        ('CITY', 10, DATE '2026-11-06', 3, 1.00::numeric),
        ('CITY', 11, DATE '2026-12-11', 4, 1.08::numeric)
),
guest_pool AS (
    SELECT id,
           username,
           email,
           row_number() OVER (ORDER BY username) AS guest_no,
           count(*) OVER ()                     AS guest_count
    FROM users
    WHERE role = 'GUEST'
),
resolved_inventory AS (
    SELECT i.*,
           p.id       AS property_id,
           p.owner_id,
           p.city,
           u.id       AS unit_id,
           u.price_per_night,
           u.capacity
    FROM inventory i
    JOIN properties p ON p.title = i.property_title
    JOIN units u ON u.property_id = p.id AND u.name = i.unit_name
),
dated AS (
    SELECT ri.*,
           s.slot_no,
           s.nights,
           s.price_multiplier,
           s.base_start + ((ri.unit_order - 1) % 4) AS start_date
    FROM resolved_inventory ri
    JOIN schedule s ON s.market = ri.market
),
classified AS (
    SELECT d.*,
           d.start_date + d.nights AS end_date,
           CASE
               WHEN d.start_date + d.nights <= DATE '2026-06-20'
                   THEN CASE WHEN (d.slot_no + d.unit_order) % 9 = 0
                             THEN 'CANCELLED' ELSE 'COMPLETED' END
               WHEN d.start_date <= DATE '2026-06-20'
                   THEN 'CONFIRMED'
               WHEN (d.slot_no + d.unit_order) % 10 = 0
                   THEN 'CANCELLED'
               WHEN (d.slot_no + d.unit_order) % 6 = 0
                   THEN 'PENDING'
               ELSE 'CONFIRMED'
           END AS status
    FROM dated d
),
priced AS (
    SELECT c.*,
           round(c.price_per_night * c.price_multiplier / 5.00) * 5.00
               AS price_per_night_snapshot
    FROM classified c
),
with_guest AS (
    SELECT p.*,
           gp.id       AS guest_id,
           gp.username AS guest_username,
           gp.email    AS guest_email
    FROM priced p
    JOIN guest_pool gp
      ON gp.guest_no = ((p.unit_order + p.slot_no - 2) % gp.guest_count) + 1
),
with_utility AS (
    SELECT wg.*,
           utility.price_per_unit AS water_unit_price,
           CASE
               -- Cancelled reservations represent pre-stay cancellations: no occupancy,
               -- no meter movement and a financially closed zero settlement.
               WHEN wg.status = 'CANCELLED' THEN 0.00::numeric
               ELSE round(
                   wg.capacity
                   * wg.nights
                   * 0.10
                   * (0.85 + ((wg.slot_no + wg.unit_order) % 7) * 0.05),
                   2
               )
           END AS water_consumption
    FROM with_guest wg
    LEFT JOIN LATERAL (
        SELECT usi.price_per_unit
        FROM unit_settlement_items usi
        WHERE usi.unit_id = wg.unit_id
          AND usi.settlement_item_type = 'WATER'
          AND usi.billing_type = 'PER_USAGE'
        ORDER BY usi.created_at, usi.id
        LIMIT 1
    ) utility ON TRUE
),
non_conflicting AS (
    SELECT wu.*
    FROM with_utility wu
    WHERE wu.start_date >= DATE '2025-06-01'
      AND wu.end_date <= DATE '2026-12-31'
      AND NOT EXISTS (
          SELECT 1
          FROM reservations existing
          WHERE existing.unit_id = wu.unit_id
            AND existing.start_date < wu.end_date
            AND existing.end_date > wu.start_date
      )
)
SELECT md5(
           'issue-196|reservation|' || property_title || '|' || unit_name || '|' || start_date
       )::uuid AS reservation_id,
       property_id,
       owner_id,
       property_title,
       city,
       unit_id,
       unit_name,
       capacity,
       unit_order,
       market,
       slot_no,
       guest_id,
       guest_username,
       guest_email,
       start_date,
       end_date,
       nights,
       status,
       price_per_night_snapshot::numeric(10, 2),
       (nights * price_per_night_snapshot)::numeric(10, 2) AS accommodation_amount,
       water_consumption::numeric(12, 6),
       water_unit_price::numeric(12, 2),
       CASE
           WHEN status = 'PENDING' THEN (start_date - 1) + TIME '09:30'
           ELSE (start_date - (21 + ((slot_no + unit_order) % 28))) + TIME '10:00'
       END AS created_at,
       CASE market
           WHEN 'SEASIDE' THEN 'Arrival after 17:00; please share parking and beach access details.'
           WHEN 'LAKE' THEN 'Family holiday; we would appreciate information about kayaks and the pier.'
           WHEN 'MOUNTAIN' THEN 'We are planning hiking trips and would like a quiet evening check-in.'
           ELSE 'Short city stay; a quiet workspace and self check-in would be helpful.'
       END AS guest_message
FROM non_conflicting;

DO $$
DECLARE
    seeded_count integer;
    missing_tariffs integer;
    invalid_water_rows integer;
BEGIN
    SELECT count(*) INTO seeded_count FROM seed_196_reservations;
    IF seeded_count NOT BETWEEN 150 AND 220 THEN
        RAISE EXCEPTION
            'Issue #196 seed expected 150-220 non-conflicting reservations, got %',
            seeded_count;
    END IF;

    SELECT count(*) INTO missing_tariffs
    FROM seed_196_reservations
    WHERE water_unit_price IS NULL;

    IF missing_tariffs > 0 THEN
        RAISE EXCEPTION
            'Issue #196 seed expected a WATER tariff for every reservation, missing %',
            missing_tariffs;
    END IF;

    SELECT count(*) INTO invalid_water_rows
    FROM seed_196_reservations
    WHERE (status = 'CANCELLED' AND water_consumption <> 0)
       OR (status <> 'CANCELLED' AND (
              water_consumption IS NULL
              OR water_consumption < capacity * nights * 0.10 * 0.20
              OR water_consumption > capacity * nights * 0.10 * 3.00
          ));

    IF invalid_water_rows > 0 THEN
        RAISE EXCEPTION
            'Issue #196 seed generated % WATER rows outside MediaReadingService approval rules',
            invalid_water_rows;
    END IF;
END $$;

INSERT INTO reservations (
    id,
    user_id,
    unit_id,
    start_date,
    end_date,
    status,
    created_at,
    updated_at,
    price_per_night_snapshot,
    total_price,
    payment_currency,
    payment_exchange_rate,
    guest_email,
    guest_message
)
SELECT reservation_id,
       guest_id,
       unit_id,
       start_date,
       end_date,
       status,
       created_at,
       CASE
           WHEN status = 'COMPLETED' THEN end_date + TIME '11:00'
           WHEN status = 'CANCELLED' THEN created_at + INTERVAL '2 days'
           ELSE created_at + INTERVAL '15 minutes'
       END,
       price_per_night_snapshot,
       accommodation_amount,
       'PLN',
       1.0000,
       guest_email,
       guest_message
FROM seed_196_reservations;

-- Status histories make the admin reservation details reflect a plausible lifecycle.
INSERT INTO reservation_status_history (
    id, reservation_id, old_status, new_status, changed_by, changed_at
)
SELECT md5('issue-196|history|pending|' || reservation_id)::uuid,
       reservation_id,
       NULL,
       'PENDING',
       guest_id,
       created_at
FROM seed_196_reservations

UNION ALL

SELECT md5('issue-196|history|confirmed|' || reservation_id)::uuid,
       reservation_id,
       'PENDING',
       'CONFIRMED',
       owner_id,
       created_at + INTERVAL '10 minutes'
FROM seed_196_reservations
WHERE status IN ('CONFIRMED', 'COMPLETED')

UNION ALL

SELECT md5('issue-196|history|completed|' || reservation_id)::uuid,
       reservation_id,
       'CONFIRMED',
       'COMPLETED',
       owner_id,
       end_date + TIME '11:00'
FROM seed_196_reservations
WHERE status = 'COMPLETED'

UNION ALL

SELECT md5('issue-196|history|cancelled|' || reservation_id)::uuid,
       reservation_id,
       'PENDING',
       'CANCELLED',
       guest_id,
       created_at + INTERVAL '2 days'
FROM seed_196_reservations
WHERE status = 'CANCELLED';

-- Financial facts are staged once so settlements, items and transactions reconcile exactly.
CREATE TEMP TABLE seed_196_financials ON COMMIT DROP AS
SELECT sr.*,
       md5('issue-196|settlement|' || sr.reservation_id)::uuid AS settlement_id,
       CASE sr.status
           WHEN 'COMPLETED' THEN 'PAID'
           WHEN 'CONFIRMED' THEN 'PARTIALLY_PAID'
           WHEN 'PENDING'   THEN 'DRAFT'
           ELSE 'CANCELLED'
       END AS settlement_status,
       CASE WHEN sr.status = 'CONFIRMED'
            THEN round(sr.accommodation_amount * 0.20, 2)
            ELSE 0.00::numeric END::numeric(12, 2) AS deposit_amount,
       -- DRAFT readings are captured for the media/OCR flow but are not billed until
       -- the reservation is confirmed. Cancelled pre-stay reservations remain zeroed.
       CASE WHEN sr.status IN ('COMPLETED', 'CONFIRMED')
            THEN round(sr.water_consumption * sr.water_unit_price, 2)
            ELSE 0.00::numeric END::numeric(12, 2) AS utilities_amount
FROM seed_196_reservations sr;

ALTER TABLE seed_196_financials
    ADD COLUMN total_amount numeric(12, 2),
    ADD COLUMN amount_paid numeric(12, 2),
    ADD COLUMN balance_due numeric(12, 2);

UPDATE seed_196_financials
SET total_amount = CASE
        WHEN status = 'CANCELLED' THEN 0.00
        ELSE accommodation_amount + utilities_amount + deposit_amount
    END,
    amount_paid = CASE
        WHEN status = 'COMPLETED'
            THEN accommodation_amount + utilities_amount + deposit_amount
        WHEN status = 'CONFIRMED'
            THEN deposit_amount
        ELSE 0.00
    END,
    balance_due = CASE
        WHEN status = 'COMPLETED' THEN 0.00
        WHEN status = 'CONFIRMED' THEN accommodation_amount + utilities_amount
        WHEN status = 'PENDING'   THEN accommodation_amount
        ELSE 0.00
    END;

INSERT INTO settlements (
    id,
    reservation_id,
    status,
    accommodation_amount,
    utilities_amount,
    deposit_amount,
    discount_amount,
    total_amount,
    amount_paid,
    balance_due,
    issued_at,
    paid_at,
    created_at,
    updated_at,
    invoice_requested
)
SELECT settlement_id,
       reservation_id,
       settlement_status,
       CASE WHEN status = 'CANCELLED' THEN 0.00 ELSE accommodation_amount END,
       utilities_amount,
       deposit_amount,
       0.00,
       total_amount,
       amount_paid,
       balance_due,
       CASE WHEN settlement_status = 'DRAFT' THEN NULL ELSE created_at END,
       CASE
           WHEN settlement_status = 'PAID' THEN end_date + TIME '14:00'
           ELSE NULL
       END,
       created_at,
       CASE WHEN status = 'COMPLETED' THEN end_date + TIME '14:00' ELSE created_at END,
       false
FROM seed_196_financials;

INSERT INTO settlement_items (
    id, settlement_id, type, description, quantity, unit_price, amount, created_at
)
SELECT md5('issue-196|item|accommodation|' || reservation_id)::uuid,
       settlement_id,
       'ACCOMMODATION',
       'Accommodation for ' || nights || ' night(s) at the seasonal booked rate',
       nights,
       price_per_night_snapshot,
       accommodation_amount,
       created_at
FROM seed_196_financials
WHERE settlement_status IN ('PAID', 'PARTIALLY_PAID')

UNION ALL

SELECT md5('issue-196|item|deposit|' || reservation_id)::uuid,
       settlement_id,
       'DEPOSIT',
       'Refundable booking deposit (20% of accommodation)',
       1,
       deposit_amount,
       deposit_amount,
       created_at
FROM seed_196_financials
WHERE deposit_amount > 0

UNION ALL

SELECT md5('issue-196|item|water|' || reservation_id)::uuid,
       settlement_id,
       'WATER',
       'Metered water consumption at check-out',
       water_consumption,
       water_unit_price,
       utilities_amount,
       end_date + TIME '11:00'
FROM seed_196_financials
WHERE settlement_status IN ('PAID', 'PARTIALLY_PAID');

-- Successful transaction totals equal amount_paid. Failed attempts never affect amount_paid.
INSERT INTO payment_transactions (
    id,
    settlement_id,
    unit_id,
    status,
    type,
    description,
    quantity,
    unit_price,
    amount,
    stripe_session_id,
    stripe_event_id,
    failure_reason,
    created_at
)
SELECT md5('issue-196|payment|accommodation|' || reservation_id)::uuid,
       settlement_id,
       unit_id,
       'SUCCESS',
       'ACCOMMODATION',
       'Accommodation payment',
       nights,
       price_per_night_snapshot,
       accommodation_amount,
       'cs_seed196_' || replace(reservation_id::text, '-', '') || '_stay',
       'evt_seed196_' || replace(reservation_id::text, '-', '') || '_stay',
       NULL,
       created_at + INTERVAL '1 day'
FROM seed_196_financials
WHERE status = 'COMPLETED'

UNION ALL

SELECT md5('issue-196|payment|water|' || reservation_id)::uuid,
       settlement_id,
       unit_id,
       'SUCCESS',
       'WATER',
       'Final metered water charge',
       water_consumption,
       water_unit_price,
       utilities_amount,
       'cs_seed196_' || replace(reservation_id::text, '-', '') || '_water',
       'evt_seed196_' || replace(reservation_id::text, '-', '') || '_water',
       NULL,
       end_date + TIME '14:00'
FROM seed_196_financials
WHERE status = 'COMPLETED'

UNION ALL

SELECT md5('issue-196|payment|deposit|' || reservation_id)::uuid,
       settlement_id,
       unit_id,
       'SUCCESS',
       'DEPOSIT',
       'Booking deposit',
       1,
       deposit_amount,
       deposit_amount,
       'cs_seed196_' || replace(reservation_id::text, '-', '') || '_deposit',
       'evt_seed196_' || replace(reservation_id::text, '-', '') || '_deposit',
       NULL,
       created_at + INTERVAL '1 hour'
FROM seed_196_financials
WHERE status = 'CONFIRMED'

UNION ALL

SELECT md5('issue-196|payment|failed|' || reservation_id)::uuid,
       settlement_id,
       unit_id,
       'FAILED',
       'ACCOMMODATION',
       'Unsuccessful accommodation payment attempt',
       nights,
       price_per_night_snapshot,
       accommodation_amount,
       'cs_seed196_' || replace(reservation_id::text, '-', '') || '_failed',
       'evt_seed196_' || replace(reservation_id::text, '-', '') || '_failed',
       CASE WHEN status = 'CANCELLED'
            THEN 'Payment session cancelled'
            ELSE 'Card authorization declined'
       END,
       created_at + INTERVAL '2 hours'
FROM seed_196_financials
WHERE status IN ('PENDING', 'CANCELLED')
  AND (slot_no + unit_order) % 2 = 0;

-- Every non-cancelled reservation gets the full WATER meter/OCR flow. Cancelled rows
-- are intentionally pre-stay cancellations with zero consumption and no meter event.
CREATE TEMP TABLE seed_196_readings ON COMMIT DROP AS
SELECT sf.*,
       md5('issue-196|reading|water|' || reservation_id)::uuid AS media_reading_id,
       (120 + unit_order * 17 + slot_no * 2)::numeric(12, 6) AS initial_reading,
       (120 + unit_order * 17 + slot_no * 2 + water_consumption)::numeric(12, 6)
           AS final_reading,
       (0.91 + ((slot_no + unit_order) % 7) * 0.01)::numeric(12, 6)
           AS initial_confidence,
       (0.90 + ((slot_no + unit_order + 3) % 8) * 0.01)::numeric(12, 6)
           AS final_confidence
FROM seed_196_financials sf
WHERE status <> 'CANCELLED';

INSERT INTO media_readings (
    id,
    settlement_id,
    utility_type,
    initial_reading,
    initial_confidence_score,
    final_reading,
    final_confidence_score,
    unit_price,
    initial_reading_status,
    final_reading_status,
    initial_reading_source,
    final_reading_source,
    created_at,
    updated_at
)
SELECT media_reading_id,
       settlement_id,
       'WATER',
       initial_reading,
       initial_confidence,
       final_reading,
       final_confidence,
       water_unit_price,
       'AUTO_APPROVED',
       'AUTO_APPROVED',
       'OCR',
       'OCR',
       start_date + TIME '15:00',
       end_date + TIME '11:00'
FROM seed_196_readings;

INSERT INTO media_reading_upload_attempts (
    id,
    media_reading_id,
    meter_image,
    ocr_value,
    confidence_score,
    status,
    reading_type,
    attempted_at
)
SELECT md5('issue-196|attempt|initial-approved|' || reservation_id)::uuid,
       media_reading_id,
       NULL::bytea,
       initial_reading::text,
       initial_confidence::numeric(5, 4),
       'AUTO_APPROVED',
       'INITIAL',
       start_date + TIME '15:00'
FROM seed_196_readings

UNION ALL

SELECT md5('issue-196|attempt|final-approved|' || reservation_id)::uuid,
       media_reading_id,
       NULL::bytea,
       final_reading::text,
       final_confidence::numeric(5, 4),
       'AUTO_APPROVED',
       'FINAL',
       end_date + TIME '11:00'
FROM seed_196_readings

UNION ALL

SELECT md5('issue-196|attempt|initial-reupload|' || reservation_id)::uuid,
       media_reading_id,
       NULL::bytea,
       initial_reading::text,
       0.6200,
       'REQUEST_REUPLOAD',
       'INITIAL',
       start_date + TIME '14:50'
FROM seed_196_readings
WHERE (slot_no + unit_order) % 5 = 0;
