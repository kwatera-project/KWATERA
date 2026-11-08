WITH eligible_reservations AS (
    SELECT
        r.id AS reservation_id,
        r.user_id AS author_id,
        r.unit_id AS unit_id,
        r.end_date AS checkout_date
    FROM reservations r
        JOIN units u ON u.id = r.unit_id
        JOIN properties p ON p.id = u.property_id
    WHERE r.status = 'COMPLETED'
      AND r.user_id <> p.owner_id
      AND NOT EXISTS (
        SELECT 1
        FROM reviews rev
        WHERE rev.reservation_id = r.id
    )
),
    numbered_reservations AS (
     SELECT
         reservation_id,
         author_id,
         unit_id,
         checkout_date,
         ROW_NUMBER() OVER (ORDER BY reservation_id) as row_num
     FROM eligible_reservations
    )

INSERT INTO reviews (id, reservation_id, author_id, unit_id, rating, comment, created_at, updated_at)
SELECT
    gen_random_uuid(),
    reservation_id,
    author_id,
    unit_id,
    -- Varied ratings (mix of 5, 4, 3, 2, and 1 stars across 20 cases)
    CASE (row_num % 20)
        WHEN 0 THEN 5
        WHEN 1 THEN 4
        WHEN 2 THEN 5
        WHEN 3 THEN 2
        WHEN 4 THEN 3
        WHEN 5 THEN 1
        WHEN 6 THEN 5
        WHEN 7 THEN 4
        WHEN 8 THEN 3
        WHEN 9 THEN 2
        WHEN 10 THEN 5
        WHEN 11 THEN 4
        WHEN 12 THEN 3
        WHEN 13 THEN 1
        WHEN 14 THEN 5
        WHEN 15 THEN 2
        WHEN 16 THEN 4
        WHEN 17 THEN 3
        WHEN 18 THEN 1
        ELSE 5
        END AS rating,
    CASE (row_num % 20)
        -- 5 stars: High praise for cleanliness and seamless service
        WHEN 0 THEN 'Absolutely wonderful stay! The place was spotless, check-in was seamless, and the host was extremely helpful and responsive.'
        -- 4 stars: Mostly positive with a slight downside (street noise)
        WHEN 1 THEN 'Great location and very comfortable bed. The only minor drawback was some street noise early in the morning, but overall a great experience.'
        -- 5 stars: Well-equipped and great views
        WHEN 2 THEN 'Charming space with beautiful views and a fully equipped kitchen. Exceeded our expectations, we will definitely book again!'
        -- 2 stars: Cleanliness issues and maintenance complaints
        WHEN 3 THEN 'Quite disappointing. There was mold in the shower drain, and the air conditioner made a loud rattling noise all night. Would not recommend.'
        -- 3 stars: Average quality / unreliable Wi-Fi for remote work
        WHEN 4 THEN 'Decent value for the price, but the Wi-Fi kept dropping, which made working remotely very difficult. Cleanliness was just okay.'
        -- 1 star: Check-in failure and dirty premises
        WHEN 5 THEN 'Terrible experience. We received the wrong key lockbox code and waited over an hour outside in the cold. The apartment was not clean upon arrival.'
        -- 5 stars: Great for business trips / high-speed internet
        WHEN 6 THEN 'Perfect spot for a business trip. High-speed fiber internet, a spacious desk setup, and a quiet neighborhood.'
        -- 4 stars: Lovely unit, but far from city center
        WHEN 7 THEN 'The apartment itself is clean and modern, exactly like the photos. The only issue is the distance from the city center; expect longer transit times.'
        -- 3 stars: Missing basic supplies
        WHEN 8 THEN 'Fantastic location, but the place lacked basic amenities—only one roll of toilet paper provided and no dish towels in the kitchen.'
        -- 2 stars: Heating issues and slow host communication
        WHEN 9 THEN 'The room was freezing cold because the heating system barely worked. Reaching out to the host for support was frustratingly slow.'
        -- 5 stars: Pet-friendly setup
        WHEN 10 THEN 'So glad we found a truly pet-friendly spot! Our dog loved the enclosed back patio, and the hosts even left out a welcome bowl and treats.'
        -- 4 stars: Great pool access, slightly crowded
        WHEN 11 THEN 'The pool area is fantastic and kept very clean. It can get a bit crowded on weekend afternoons, but otherwise a wonderful vacation spot.'
        -- 3 stars: Uncomfortable bedding
        WHEN 12 THEN 'The apartment was cozy and well-located, but the mattress was far too firm and the pillows lacked support. Woke up with a sore back.'
        -- 1 star: Unresponsive host and pest issues
        WHEN 13 THEN 'Awful stay. We spotted bugs in the kitchen, and when we tried calling the emergency contact number, nobody answered or returned our messages.'
        -- 5 stars: Accommodating host & early check-in
        WHEN 14 THEN 'Host was an absolute lifesaver! Allowed us to check in three hours early after a long flight. Beautiful apartment and exceptional hospitality.'
        -- 2 stars: Parking nightmare
        WHEN 15 THEN 'The listing stated parking was included, but the designated space was tiny and blocked by construction bins the entire weekend.'
        -- 4 stars: Great for families
        WHEN 16 THEN 'Spacious place with plenty of room for our family of four. High chair and pack-n-play were provided as requested, which saved us a lot of hassle.'
        -- 3 stars: Thin walls / noise complaints
        WHEN 17 THEN 'The space itself is decorated nicely, but the walls are paper thin. We could hear neighbors talking and footsteps overhead late into the night.'
        -- 1 star: Plumbing failure / hot water out
        WHEN 18 THEN 'No hot water for our entire three-day stay. The host kept insisting it was being fixed, but nothing changed. Unacceptable standard.'
        -- 5 stars: Aesthetic design & cozy atmosphere
        ELSE 'Super cozy and beautifully styled! The warm lighting, plush linens, and curated book collection made it feel like a true home away from home.'
        END AS comment,

    checkout_date + ((12 + (row_num % 72)) || ' hours')::INTERVAL AS created_at,
    checkout_date + ((12 + (row_num % 72)) || ' hours')::INTERVAL AS updated_at

FROM numbered_reservations;


DO $$
DECLARE
v_failed_count INT;
BEGIN
    -- 1. Test: Verifying that there is at most one review for each reservation
SELECT COUNT(*) INTO v_failed_count
FROM (
         SELECT reservation_id
         FROM reviews
         GROUP BY reservation_id
         HAVING COUNT(*) > 1
     ) sub;

IF v_failed_count > 0 THEN
        RAISE EXCEPTION 'TEST ERROR: Reservations with more than one review found! Count: %', v_failed_count;
END IF;

    -- 2. Test: Verify that all reviews pertain exclusively to bookings with the status 'COMPLETED'
SELECT COUNT(*) INTO v_failed_count
FROM reviews rev
         JOIN reservations r ON rev.reservation_id = r.id
WHERE r.status <> 'COMPLETED';

IF v_failed_count > 0 THEN
        RAISE EXCEPTION 'TEST ERROR: Reviews found for reservations with a status other than COMPLETED! Count: %', v_failed_count;
END IF;

    -- 3. Test: Verifying that the author of the review (guest) is NOT the owner (owner)
SELECT COUNT(*) INTO v_failed_count
FROM reviews rev
         JOIN reservations r ON rev.reservation_id = r.id
         JOIN units u ON r.unit_id = u.id
         JOIN properties p ON u.property_id = p.id
WHERE rev.author_id = p.owner_id;

IF v_failed_count > 0 THEN
        RAISE EXCEPTION 'TEST ERROR: Reviews posted by the owner instead of a guest were found! Count: %', v_failed_count;
END IF;

    RAISE NOTICE 'Success: All conditions have been successfully verifiede!';
END $$;