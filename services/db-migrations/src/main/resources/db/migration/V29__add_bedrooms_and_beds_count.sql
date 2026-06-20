UPDATE units
SET bedrooms = v.bedrooms,
    beds      = v.beds
    FROM (VALUES
    -- Cottages / cabins
    ('Room 1',                    1, 1), -- capacity 2
    ('Family Suite',              2, 3), -- capacity 5
    ('Entire Forest Cabin',       3, 3), -- capacity 6
    ('Entire Mountain Cabin',     4, 4), -- capacity 8
    ('Entire Nature Cabin',       4, 4), -- capacity 8
    ('Attic Room',                1, 1), -- capacity 2
    ('Entire Lakeside Cottage',   3, 3), -- capacity 6

    -- Apartments (first units)
    ('Studio 1',                  1, 1), -- capacity 2
    ('Apartment Deluxe',          2, 2), -- capacity 4
    ('City Center Apartment',     2, 2), -- capacity 4
    ('Historic Center Apartment', 2, 2), -- capacity 4
    ('Odra View Apartment',       1, 2), -- capacity 3
    ('Beachside Apartment',       2, 2), -- capacity 4
    ('Executive Apartment',       2, 2), -- capacity 4
    ('Premium City Apartment',     2, 2), -- capacity 4
    ('Riverside Deluxe Apartment', 1, 2), -- capacity 3
    ('Executive Plus Apartment',   2, 2), -- capacity 4
    ('Seaside Luxury Apartment',   2, 2), -- capacity 4
    ('Old Town Comfort Apartment', 2, 2)  -- capacity 4
) AS v(name, bedrooms, beds)
WHERE units.name = v.name;