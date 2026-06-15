INSERT INTO users (id, role, username, email, password, first_name, last_name)
VALUES
-- OWNERS
(gen_random_uuid(), 'OWNER', 'owner3', 'owner3@example.com',
 '$2a$12$Nn3JKEeeEFlGpJPGNCQ3RekQIauvI1nNxcPO7mGaO0qwnHODq5mo6', 'Piotr', 'Wiśniewski'),
(gen_random_uuid(), 'OWNER', 'owner4', 'owner4@example.com',
 '$2a$12$Nn3JKEeeEFlGpJPGNCQ3RekQIauvI1nNxcPO7mGaO0qwnHODq5mo6', 'Maria', 'Wójcik'),
(gen_random_uuid(), 'OWNER', 'owner5', 'owner5@example.com',
 '$2a$12$Nn3JKEeeEFlGpJPGNCQ3RekQIauvI1nNxcPO7mGaO0qwnHODq5mo6', 'Tomasz', 'Kamiński'),
(gen_random_uuid(), 'OWNER', 'owner6', 'owner6@example.com',
 '$2a$12$Nn3JKEeeEFlGpJPGNCQ3RekQIauvI1nNxcPO7mGaO0qwnHODq5mo6', 'Katarzyna', 'Lewandowska'),
(gen_random_uuid(), 'OWNER', 'owner7', 'owner7@example.com',
 '$2a$12$Nn3JKEeeEFlGpJPGNCQ3RekQIauvI1nNxcPO7mGaO0qwnHODq5mo6', 'Michał', 'Zieliński'),
(gen_random_uuid(), 'OWNER', 'owner8', 'owner8@example.com',
 '$2a$12$Nn3JKEeeEFlGpJPGNCQ3RekQIauvI1nNxcPO7mGaO0qwnHODq5mo6', 'Agnieszka', 'Szymańska'),
(gen_random_uuid(), 'OWNER', 'owner9', 'owner9@example.com',
 '$2a$12$Nn3JKEeeEFlGpJPGNCQ3RekQIauvI1nNxcPO7mGaO0qwnHODq5mo6', 'Paweł', 'Woźniak'),
(gen_random_uuid(), 'OWNER', 'owner10', 'owner10@example.com',
 '$2a$12$Nn3JKEeeEFlGpJPGNCQ3RekQIauvI1nNxcPO7mGaO0qwnHODq5mo6', 'Monika', 'Dąbrowska'),

-- GUESTS
(gen_random_uuid(), 'GUEST', 'guest3', 'guest3@example.com',
 '$2a$12$Nn3JKEeeEFlGpJPGNCQ3RekQIauvI1nNxcPO7mGaO0qwnHODq5mo6', 'Adam', 'Krawczyk'),
(gen_random_uuid(), 'GUEST', 'guest4', 'guest4@example.com',
 '$2a$12$Nn3JKEeeEFlGpJPGNCQ3RekQIauvI1nNxcPO7mGaO0qwnHODq5mo6', 'Ewa', 'Piotrowska'),
(gen_random_uuid(), 'GUEST', 'guest5', 'guest5@example.com',
 '$2a$12$Nn3JKEeeEFlGpJPGNCQ3RekQIauvI1nNxcPO7mGaO0qwnHODq5mo6', 'Karol', 'Grabowski'),
(gen_random_uuid(), 'GUEST', 'guest6', 'guest6@example.com',
 '$2a$12$Nn3JKEeeEFlGpJPGNCQ3RekQIauvI1nNxcPO7mGaO0qwnHODq5mo6', 'Joanna', 'Pawlak'),
(gen_random_uuid(), 'GUEST', 'guest7', 'guest7@example.com',
 '$2a$12$Nn3JKEeeEFlGpJPGNCQ3RekQIauvI1nNxcPO7mGaO0qwnHODq5mo6', 'Mateusz', 'Michalski'),
(gen_random_uuid(), 'GUEST', 'guest8', 'guest8@example.com',
 '$2a$12$Nn3JKEeeEFlGpJPGNCQ3RekQIauvI1nNxcPO7mGaO0qwnHODq5mo6', 'Natalia', 'Król'),
(gen_random_uuid(), 'GUEST', 'guest9', 'guest9@example.com',
 '$2a$12$Nn3JKEeeEFlGpJPGNCQ3RekQIauvI1nNxcPO7mGaO0qwnHODq5mo6', 'Łukasz', 'Wieczorek'),
(gen_random_uuid(), 'GUEST', 'guest10', 'guest10@example.com',
 '$2a$12$Nn3JKEeeEFlGpJPGNCQ3RekQIauvI1nNxcPO7mGaO0qwnHODq5mo6', 'Paulina', 'Mazur');

INSERT INTO properties (id, owner_id, title, description,
                        city, country, postal_code, street, street_number,
                        latitude, longitude, amenities)
SELECT gen_random_uuid(),
       u.id,
       p.title,
       p.description,
       p.city,
       'Poland',
       p.postal_code,
       p.street,
       p.street_number,
       p.latitude,
       p.longitude,
       p.amenities::jsonb
FROM users u
         JOIN (VALUES

                   -- Mountain / Forest Cabins

                   ('owner1', 'Forest Retreat',
                    'A cozy cabin surrounded by the forests of the Żywiec Beskids, perfect for a peaceful getaway and outdoor adventures.',
                    'Szczyrk', '43-370', 'Leśna', '12', 49.71770000, 19.03200000,
                    '["WiFi","Parking","Fireplace","BBQ"]'),

                   ('owner2', 'Mountain Escape',
                    'A charming mountain cabin offering breathtaking views of the Tatra Mountains and easy access to hiking trails.',
                    'Kościelisko', '34-511', 'Nędzy Kubińca', '45', 49.29020000, 19.88950000,
                    '["WiFi","Hot Tub","Parking","Fireplace"]'),

                   ('owner3', 'Evergreen Cabin',
                    'A traditional wooden cabin surrounded by evergreen forests, offering peace, privacy, and an authentic mountain atmosphere.',
                    'Wetlina', '38-608', 'Bieszczadzka', '7', 49.12790000, 22.46800000,
                    '["WiFi","BBQ","Terrace","Parking"]'),

                   ('owner4', 'Spruce Hideaway',
                    'A comfortable cabin located near the Karkonosze Mountains, ideal for families and nature lovers.',
                    'Karpacz', '58-540', 'Leśna', '18', 50.77670000, 15.75500000,
                    '["WiFi","Sauna","Parking","Fireplace"]'),

                   ('owner5', 'Masurian Forest Lodge',
                    'A lakeside cabin surrounded by the beautiful forests of Masuria, offering relaxation and water activities.',
                    'Mikołajki', '11-730', 'Jeziorna', '4', 53.80230000, 21.57010000,
                    '["WiFi","Kayaks","Parking","BBQ"]'),

                   -- Apartments

                   ('owner6', 'Gdansk City Apartment',
                    'A modern apartment located close to Gdansk Old Town, featuring stylish interiors and excellent city access.',
                    'Gdańsk', '80-830', 'Długa', '15', 54.34800000, 18.65300000,
                    '["WiFi","Elevator","Air Conditioning"]'),

                   ('owner7', 'Krakow Old Town Apartment',
                    'A stylish apartment in the heart of Krakow, within walking distance of historic landmarks and restaurants.',
                    'Kraków', '31-017', 'Floriańska', '22', 50.06470000, 19.94500000,
                    '["WiFi","Air Conditioning","Balcony"]'),

                   ('owner8', 'Riverside Apartment',
                    'A comfortable apartment overlooking the Odra River, offering modern amenities and a central location.',
                    'Wrocław', '50-001', 'Kotlarska', '8', 51.10790000, 17.03850000,
                    '["WiFi","Parking","Elevator"]'),

                   ('owner9', 'Seaside Apartment',
                    'A bright and spacious apartment located near the beach, perfect for a relaxing seaside vacation.',
                    'Sopot', '81-701', 'Bohaterów Monte Cassino', '33', 54.44180000, 18.56000000,
                    '["WiFi","Parking", "Elevator"]'),

                   ('owner10', 'Business Class Apartment',
                    'An elegant apartment designed for business travelers, offering premium comfort in the center of Warsaw.',
                    'Warszawa', '00-001', 'Marszałkowska', '100', 52.22970000, 21.01220000,
                    '["WiFi","Air Conditioning","Gym","Elevator"]')) AS p(
                                                                          username, title, description,
                                                                          city, postal_code, street, street_number,
                                                                          latitude, longitude, amenities
    )
              ON u.username = p.username;


INSERT INTO units (id,
                   property_id,
                   name,
                   description,
                   price_per_night,
                   capacity,
                   unit_type,
                   unit_number,
                   floor,
                   amenities)
SELECT gen_random_uuid(),
       p.id,
       u.name,
       u.description,
       u.price_per_night,
       u.capacity,
       u.unit_type,
       u.unit_number,
       u.floor,
       u.amenities::jsonb
FROM properties p
         JOIN (VALUES ('Forest Retreat',
                       'Entire Forest Cabin',
                       'A private forest cabin with a fireplace and outdoor barbecue area.',
                       420.00, 6, 'ENTIRE_COTTAGE', 'FR-1', 0,
                       '["WiFi","Fireplace","BBQ","Parking"]'),

                      ('Mountain Escape',
                       'Entire Mountain Cabin',
                       'A spacious mountain cabin with panoramic views of the Tatra Mountains.',
                       550.00, 8, 'ENTIRE_COTTAGE', 'ME-1', 0,
                       '["WiFi","Hot Tub","Fireplace","Parking"]'),

                      ('Evergreen Cabin',
                       'Entire Nature Cabin',
                       'A cozy private attic room in a wooden cabin surrounded by nature.',
                       600.00, 8, 'ENTIRE_COTTAGE', 'AT-1', 1,
                       '["WiFi","Shared Kitchen","Parking"]'),

                      ('Spruce Hideaway',
                       'Attic Room',
                       'A cozy private attic room in a wooden cabin surrounded by nature.',
                       180.00, 2, 'PRIVATE_ROOM_IN_HOME', 'SH-1', 0,
                       '["WiFi","Sauna","Fireplace","Parking"]'),

                      ('Masurian Forest Lodge',
                       'Entire Lakeside Cottage',
                       'A charming lakeside cottage with direct access to outdoor recreation.',
                       480.00, 6, 'ENTIRE_COTTAGE', 'MF-1', 0,
                       '["WiFi","Kayaks","BBQ","Parking"]'),

                      ('Gdansk City Apartment',
                       'City Center Apartment',
                       'Modern apartment located in the heart of Gdansk Old Town.',
                       350.00, 4, 'ENTIRE_APARTMENT', 'A101', 1,
                       '["WiFi","Air Conditioning","Elevator"]'),

                      ('Krakow Old Town Apartment',
                       'Historic Center Apartment',
                       'Elegant apartment within walking distance of Krakow’s main attractions.',
                       380.00, 4, 'ENTIRE_APARTMENT', 'A202', 2,
                       '["WiFi","Balcony","Air Conditioning"]'),

                      ('Riverside Apartment',
                       'Odra View Apartment',
                       'Comfortable apartment with views of the Odra River.',
                       340.00, 3, 'ENTIRE_APARTMENT', 'A303', 3,
                       '["WiFi","Elevator","Parking"]'),

                      ('Seaside Apartment',
                       'Beachside Apartment',
                       'Bright apartment located just minutes from the beach.',
                       450.00, 4, 'ENTIRE_APARTMENT', 'A404', 4,
                       '["WiFi","Balcony","Parking"]'),

                      ('Business Class Apartment',
                       'Executive Apartment',
                       'Premium apartment designed for business travelers in central Warsaw.',
                       520.00, 4, 'ENTIRE_APARTMENT', 'A505', 5,
                       '["WiFi","Air Conditioning","Gym","Elevator"]')) AS u(
                                                                             property_title,
                                                                             name,
                                                                             description,
                                                                             price_per_night,
                                                                             capacity,
                                                                             unit_type,
                                                                             unit_number,
                                                                             floor,
                                                                             amenities
    )
              ON p.title = u.property_title;

INSERT INTO units (id,
                   property_id,
                   name,
                   description,
                   price_per_night,
                   capacity,
                   unit_type,
                   unit_number,
                   floor,
                   amenities)

SELECT gen_random_uuid(),
       p.id,
       u.name,
       u.description,
       u.price_per_night,
       u.capacity,
       'ENTIRE_APARTMENT',
       u.unit_number,
       u.floor,
       u.amenities::jsonb
FROM properties p
         JOIN (VALUES

                   -- Gdansk City Apartment (SECOND UNIT)
                   ('Gdansk City Apartment',
                    'Premium City Apartment',
                    'A stylish and fully equipped premium apartment in Gdansk, ideal for longer stays and business travelers.',
                    420.00, 4, 'A102', 1,
                    '["WiFi","Air Conditioning","Elevator"]'),

                   -- Riverside Apartment (SECOND UNIT)
                   ('Riverside Apartment',
                    'Riverside Deluxe Apartment',
                    'Modern riverside apartment with enhanced comfort and upgraded interior design.',
                    390.00, 3, 'R201', 2,
                    '["WiFi","Elevator","Parking","River View"]'),

                   -- Business Class Apartment (SECOND UNIT)
                   ('Business Class Apartment',
                    'Executive Plus Apartment',
                    'High-end executive apartment with extra workspace and premium amenities.',
                    590.00, 4, 'B601', 6,
                    '["WiFi","Air Conditioning","Gym","Elevator"]'),

                   -- Seaside Apartment (SECOND UNIT)
                   ('Seaside Apartment',
                    'Seaside Luxury Apartment',
                    'Luxury seaside apartment with panoramic coastal views and modern interior.',
                    520.00, 4, 'S501', 5,
                    '["WiFi","Balcony","Parking","Sea View"]'),

                   -- Krakow Old Town Apartment (SECOND UNIT)
                   ('Krakow Old Town Apartment',
                    'Old Town Comfort Apartment',
                    'Comfortable apartment located in historic Krakow Old Town with upgraded amenities.',
                    430.00, 4, 'K301', 3,
                    '["WiFi","Balcony","Air Conditioning","Historic View"]')) AS u(
                                                                                   property_title,
                                                                                   name,
                                                                                   description,
                                                                                   price_per_night,
                                                                                   capacity,
                                                                                   unit_number,
                                                                                   floor,
                                                                                   amenities
    )
              ON p.title = u.property_title;

INSERT INTO property_images (id, property_id, url, is_main)
SELECT gen_random_uuid(), p.id, i.url, i.is_main
FROM properties p
         JOIN (VALUES ('Forest Retreat', 'https://images.pexels.com/photos/18628485/pexels-photo-18628485.jpeg', true),

                      ('Mountain Escape', 'https://images.pexels.com/photos/13181168/pexels-photo-13181168.jpeg', true),

                      ('Evergreen Cabin', 'https://images.pexels.com/photos/35124483/pexels-photo-35124483.jpeg', true),

                      ('Spruce Hideaway', 'https://images.pexels.com/photos/8001411/pexels-photo-8001411.jpeg', true),

                      ('Masurian Forest Lodge', 'https://images.pexels.com/photos/17804250/pexels-photo-17804250.jpeg',
                       true),

                      ('Business Class Apartment', 'https://images.pexels.com/photos/439391/pexels-photo-439391.jpeg',
                       true),

                      ('Seaside Apartment', 'https://images.pexels.com/photos/27075286/pexels-photo-27075286.jpeg',
                       true),

                      ('Krakow Old Town Apartment',
                       'https://images.pexels.com/photos/31656149/pexels-photo-31656149.jpeg', true),
                      ('Krakow Old Town Apartment',
                       'https://images.pexels.com/photos/31656153/pexels-photo-31656153.jpeg', false),

                      ('Riverside Apartment', 'https://images.pexels.com/photos/34360419/pexels-photo-34360419.jpeg',
                       true),

                      ('Gdansk City Apartment', 'https://images.pexels.com/photos/34360421/pexels-photo-34360421.jpeg',
                       true)) i(title, url, is_main)
              ON p.title = i.title;


INSERT INTO unit_images (id, unit_id, url, is_main)
SELECT gen_random_uuid(), u.id, i.url, i.is_main
FROM units u
         JOIN (VALUES

                   -- Forest Retreat
                   ('Entire Forest Cabin', 'https://images.pexels.com/photos/7746099/pexels-photo-7746099.jpeg', true),
                   ('Entire Forest Cabin', 'https://images.pexels.com/photos/7746103/pexels-photo-7746103.jpeg', false),
                   ('Entire Forest Cabin', 'https://images.pexels.com/photos/7746101/pexels-photo-7746101.jpeg', false),

                   -- Mountain Escape
                   ('Entire Mountain Cabin', 'https://images.pexels.com/photos/7746949/pexels-photo-7746949.jpeg',
                    true),
                   ('Entire Mountain Cabin', 'https://images.pexels.com/photos/7746941/pexels-photo-7746941.jpeg',
                    false),
                   ('Entire Mountain Cabin', 'https://images.pexels.com/photos/7746943/pexels-photo-7746943.jpeg',
                    false),

                   -- Evergreen Cabin
                   ('Entire Nature Cabin', 'https://images.pexels.com/photos/7746476/pexels-photo-7746476.jpeg', true),
                   ('Entire Nature Cabin', 'https://images.pexels.com/photos/7746470/pexels-photo-7746470.jpeg', false),
                   ('Entire Nature Cabin', 'https://images.pexels.com/photos/7746466/pexels-photo-7746466.jpeg', false),

                   -- Spruce Hideaway
                   ('Attic Room', 'https://images.pexels.com/photos/7745977/pexels-photo-7745977.jpeg', true),
                   ('Attic Room', 'https://images.pexels.com/photos/7745978/pexels-photo-7745978.jpeg', false),
                   ('Attic Room', 'https://images.pexels.com/photos/7745995/pexels-photo-7745995.jpeg', false),

                   -- Masurian Forest Lodge
                   ('Entire Lakeside Cottage', 'https://images.pexels.com/photos/6296918/pexels-photo-6296918.jpeg',
                    true),
                   ('Entire Lakeside Cottage', 'https://images.pexels.com/photos/6296919/pexels-photo-6296919.jpeg',
                    false),

                   -- Gdansk City Apartment
                   ('City Center Apartment', 'https://images.pexels.com/photos/6899438/pexels-photo-6899438.jpeg',
                    true),
                   ('City Center Apartment', 'https://images.pexels.com/photos/6899439/pexels-photo-6899439.jpeg',
                    false),
                   ('City Center Apartment', 'https://images.pexels.com/photos/6899443/pexels-photo-6899443.jpeg',
                    false),

                   -- Krakow Old Town Apartment
                   ('Historic Center Apartment', 'https://images.pexels.com/photos/6492403/pexels-photo-6492403.jpeg',
                    true),
                   ('Historic Center Apartment', 'https://images.pexels.com/photos/6492396/pexels-photo-6492396.jpeg',
                    false),
                   ('Historic Center Apartment', 'https://images.pexels.com/photos/6492391/pexels-photo-6492391.jpeg',
                    false),

                   -- Riverside Apartment
                   ('Odra View Apartment', 'https://images.pexels.com/photos/7018401/pexels-photo-7018401.jpeg', true),
                   ('Odra View Apartment', 'https://images.pexels.com/photos/7018387/pexels-photo-7018387.jpeg', false),
                   ('Odra View Apartment', 'https://images.pexels.com/photos/7018399/pexels-photo-7018399.jpeg', false),

                   -- Seaside Apartment
                   ('Beachside Apartment', 'https://images.pexels.com/photos/6489119/pexels-photo-6489119.jpeg', true),
                   ('Beachside Apartment', 'https://images.pexels.com/photos/6489105/pexels-photo-6489105.jpeg', false),
                   ('Beachside Apartment', 'https://images.pexels.com/photos/6489121/pexels-photo-6489121.jpeg', false),

                   -- Business Class Apartment
                   ('Executive Apartment', 'https://images.pexels.com/photos/8089170/pexels-photo-8089170.jpeg', true),
                   ('Executive Apartment', 'https://images.pexels.com/photos/8089158/pexels-photo-8089158.jpeg', false),
                   ('Executive Apartment', 'https://images.pexels.com/photos/8089195/pexels-photo-8089195.jpeg',
                    false),

                   -- Premium City Apartment
                   ('Premium City Apartment', 'https://images.pexels.com/photos/7546716/pexels-photo-7546716.jpeg',
                    true),
                   ('Premium City Apartment', 'https://images.pexels.com/photos/7546715/pexels-photo-7546715.jpeg',
                    false),
                   ('Premium City Apartment', 'https://images.pexels.com/photos/7546719/pexels-photo-7546719.jpeg',
                    false),

                   -- Riverside Deluxe Apartment
                   ('Riverside Deluxe Apartment', 'https://images.pexels.com/photos/7512035/pexels-photo-7512035.jpeg',
                    true),
                   ('Riverside Deluxe Apartment', 'https://images.pexels.com/photos/7512040/pexels-photo-7512040.jpeg',
                    false),
                   ('Riverside Deluxe Apartment', 'https://images.pexels.com/photos/7512030/pexels-photo-7512030.jpeg',
                    false),

                   -- Executive Plus Apartment
                   ('Executive Plus Apartment', 'https://images.pexels.com/photos/7511698/pexels-photo-7511698.jpeg',
                    true),
                   ('Executive Plus Apartment', 'https://images.pexels.com/photos/7511695/pexels-photo-7511695.jpeg',
                    false),
                   ('Executive Plus Apartment', 'https://images.pexels.com/photos/7511703/pexels-photo-7511703.jpeg',
                    false),

                   -- Seaside Luxury Apartment
                   ('Seaside Luxury Apartment', 'https://images.pexels.com/photos/6782569/pexels-photo-6782569.jpeg',
                    true),
                   ('Seaside Luxury Apartment', 'https://images.pexels.com/photos/6782581/pexels-photo-6782581.jpeg',
                    false),

                   -- Old Town Comfort Apartment
                   ('Old Town Comfort Apartment', 'https://images.pexels.com/photos/6444981/pexels-photo-6444981.jpeg',
                    true),
                   ('Old Town Comfort Apartment', 'https://images.pexels.com/photos/6444970/pexels-photo-6444970.jpeg',
                    false)) i(unit_name, url, is_main)
              ON u.name = i.unit_name;


INSERT INTO unit_settlement_items (id,
                                   unit_id,
                                   settlement_item_type,
                                   price_per_unit,
                                   measurement_unit,
                                   billing_type)
SELECT gen_random_uuid(),
       u.id,
       'WATER',
       18.50,
       'M3',
       'PER_USAGE'
FROM units u
WHERE u.name IN (
                 'Entire Forest Cabin',
                 'Entire Mountain Cabin',
                 'Entire Nature Cabin',
                 'Attic Room',
                 'Entire Lakeside Cottage',
                 'City Center Apartment',
                 'Historic Center Apartment',
                 'Odra View Apartment',
                 'Beachside Apartment',
                 'Executive Apartment',
                 'Premium City Apartment',
                 'Riverside Deluxe Apartment',
                 'Executive Plus Apartment',
                 'Seaside Luxury Apartment',
                 'Old Town Comfort Apartment'
    );

-- =====================================================================
-- Logika scenariuszy (kolumna pattern):
-- PCC – PENDING → CONFIRMED (po ~10 min) → COMPLETED (po dacie check-out) – rezerwacje z przeszłości, zrealizowane
-- PX – PENDING → CANCELLED (po >15 min braku potwierdzenia) – rezerwacje, które „wygasły"
-- PCX – PENDING → CONFIRMED → CANCELLED (gość/host odwołał przed check-in)
-- PC – PENDING → CONFIRMED – rezerwacje trwające/nadchodzące, potwierdzone
-- P – tylko PENDING – „świeżo" złożone (utworzone kilka minut temu, jeszcze w oknie 15 min)
-- =====================================================================

WITH reservation_data (
                       id, guest_username, unit_name, start_offset, nights,
                       status, pattern, currency, fx_rate, guest_message, pending_minutes_ago
    ) AS (
    VALUES
        -- ===================== PRZESZŁOŚĆ: ZAKOŃCZONE (PCC -> COMPLETED) =====================
        ('a1111111-1111-4111-8111-000000000001'::uuid, 'guest3',  'Entire Forest Cabin',       -150, 4, 'COMPLETED', 'PCC', 'PLN', 1.0000, 'Looking forward to a relaxing forest break!', 0),
        ('a1111111-1111-4111-8111-000000000002'::uuid, 'guest4',  'Entire Mountain Cabin',     -120, 7, 'COMPLETED', 'PCC', 'EUR', 4.3500, 'Traveling with the whole family, hoping for great views.', 0),
        ('a1111111-1111-4111-8111-000000000003'::uuid, 'guest5',  'Entire Nature Cabin',        -90, 3, 'COMPLETED', 'PCC', 'PLN', 1.0000, NULL, 0),
        ('a1111111-1111-4111-8111-000000000004'::uuid, 'guest6',  'Attic Room',                 -60, 2, 'COMPLETED', 'PCC', 'PLN', 1.0000, 'Quick weekend getaway.', 0),
        ('a1111111-1111-4111-8111-000000000005'::uuid, 'guest7',  'Entire Lakeside Cottage',    -45, 5, 'COMPLETED', 'PCC', 'PLN', 1.0000, 'Bringing kayaking gear, hope that is fine!', 0),
        ('a1111111-1111-4111-8111-000000000006'::uuid, 'guest8',  'City Center Apartment',      -30, 4, 'COMPLETED', 'PCC', 'USD', 3.9500, NULL, 0),
        ('a1111111-1111-4111-8111-000000000007'::uuid, 'guest9',  'Historic Center Apartment',  -20, 3, 'COMPLETED', 'PCC', 'PLN', 1.0000, 'Celebrating our anniversary.', 0),
        ('a1111111-1111-4111-8111-000000000008'::uuid, 'guest10', 'Odra View Apartment',        -10, 2, 'COMPLETED', 'PCC', 'PLN', 1.0000, NULL, 0),

        -- ===================== PRZESZŁOŚĆ: WYGASŁE PENDING (PX -> CANCELLED) =====================
        ('a1111111-1111-4111-8111-000000000009'::uuid, 'guest3',  'Beachside Apartment',        -80, 3, 'CANCELLED', 'PX',  'PLN', 1.0000, 'Just checking availability.', 0),
        ('a1111111-1111-4111-8111-000000000010'::uuid, 'owner3',  'Premium City Apartment',     -40, 2, 'CANCELLED', 'PX',  'PLN', 1.0000, NULL, 0),

        -- ===================== PRZESZŁOŚĆ: ODWOŁANE PO POTWIERDZENIU (PCX -> CANCELLED) =====================
        ('a1111111-1111-4111-8111-000000000011'::uuid, 'guest5',  'Executive Apartment',        -25, 4, 'CANCELLED', 'PCX', 'PLN', 1.0000, 'Plans changed unexpectedly, sorry for the trouble.', 0),
        ('a1111111-1111-4111-8111-000000000012'::uuid, 'guest6',  'Riverside Deluxe Apartment', -15, 2, 'CANCELLED', 'PCX', 'EUR', 4.3500, NULL, 0),

        -- ===================== TRWAJĄCE / NADCHODZĄCE: POTWIERDZONE (PC -> CONFIRMED) =====================
        ('a1111111-1111-4111-8111-000000000013'::uuid, 'guest7',  'Entire Forest Cabin',         -2, 6, 'CONFIRMED', 'PC',  'PLN', 1.0000, 'Excited for the fireplace evenings.', 0),
        ('a1111111-1111-4111-8111-000000000014'::uuid, 'guest8',  'Seaside Luxury Apartment',    -1, 4, 'CONFIRMED', 'PC',  'PLN', 1.0000, NULL, 0),
        ('a1111111-1111-4111-8111-000000000015'::uuid, 'guest9',  'Executive Plus Apartment',     5, 3, 'CONFIRMED', 'PC',  'PLN', 1.0000, 'Business trip, need a quiet workspace.', 0),
        ('a1111111-1111-4111-8111-000000000016'::uuid, 'guest10', 'Old Town Comfort Apartment',  10, 5, 'CONFIRMED', 'PC',  'PLN', 1.0000, NULL, 0),
        ('a1111111-1111-4111-8111-000000000017'::uuid, 'guest3',  'Entire Mountain Cabin',       20, 7, 'CONFIRMED', 'PC',  'PLN', 1.0000, 'Group hiking trip with friends.', 0),
        ('a1111111-1111-4111-8111-000000000018'::uuid, 'guest4',  'Entire Nature Cabin',         45, 4, 'CONFIRMED', 'PC',  'EUR', 4.3500, NULL, 0),
        ('a1111111-1111-4111-8111-000000000019'::uuid, 'owner4',  'Entire Lakeside Cottage',     60, 3, 'CONFIRMED', 'PC',  'PLN', 1.0000, 'Visiting friends nearby.', 0),
        ('a1111111-1111-4111-8111-000000000020'::uuid, 'guest6',  'City Center Apartment',       90, 2, 'CONFIRMED', 'PC',  'PLN', 1.0000, NULL, 0),
        ('a1111111-1111-4111-8111-000000000021'::uuid, 'guest7',  'Historic Center Apartment',  120, 6, 'CONFIRMED', 'PC',  'PLN', 1.0000, 'Family reunion trip.', 0),
        ('a1111111-1111-4111-8111-000000000022'::uuid, 'guest8',  'Premium City Apartment',     150, 3, 'CONFIRMED', 'PC',  'USD', 3.9500, NULL, 0),

        -- ===================== "ŚWIEŻE" PENDING (jeszcze w oknie 15 minut) =====================
        ('a1111111-1111-4111-8111-000000000023'::uuid, 'guest9',  'Odra View Apartment',         30, 2, 'PENDING',   'P',   'PLN', 1.0000, 'Just submitted, awaiting confirmation.', 5),
        ('a1111111-1111-4111-8111-000000000024'::uuid, 'guest10', 'Beachside Apartment',         75, 4, 'PENDING',   'P',   'PLN', 1.0000, NULL, 2),

        -- ===================== NADCHODZĄCE: WYGASŁE PENDING (PX -> CANCELLED) =====================
        ('a1111111-1111-4111-8111-000000000025'::uuid, 'guest3',  'Executive Apartment',        100, 3, 'CANCELLED', 'PX',  'PLN', 1.0000, NULL, 0),
        ('a1111111-1111-4111-8111-000000000026'::uuid, 'guest5',  'Riverside Deluxe Apartment', 170, 2, 'CANCELLED', 'PX',  'PLN', 1.0000, 'Had to cancel due to a scheduling conflict.', 0)
),


     computed AS (
         SELECT
             rd.id,
             rd.status,
             rd.pattern,
             rd.currency,
             rd.fx_rate,
             rd.guest_message,
             rd.pending_minutes_ago,
             rd.nights,
             gu.id    AS guest_id,
             gu.email AS guest_email,
             un.id    AS unit_id,
             un.price_per_night,
             pr.owner_id AS owner_id,
             (CURRENT_DATE + rd.start_offset)               AS start_date,
             (CURRENT_DATE + rd.start_offset + rd.nights)   AS end_date
         FROM reservation_data rd
                  JOIN users      gu ON gu.username = rd.guest_username
                  JOIN units      un ON un.name     = rd.unit_name
                  JOIN properties pr ON pr.id       = un.property_id
     ),

     ins_res AS (
INSERT INTO reservations (
    id, user_id, unit_id, start_date, end_date, status,
    price_per_night_snapshot, total_price, guest_message,
    payment_currency, payment_exchange_rate, guest_email,
    created_at, updated_at
)
SELECT
    c.id,
    c.guest_id,
    c.unit_id,
    c.start_date,
    c.end_date,
    c.status,
    c.price_per_night,
    c.price_per_night * c.nights,
    c.guest_message,
    c.currency,
    c.fx_rate,
    c.guest_email,
    -- created_at
    CASE
        WHEN c.pattern = 'PCC' THEN (c.start_date - 5) + TIME '10:00'
        WHEN c.pattern = 'PX' AND c.start_date < CURRENT_DATE THEN (c.start_date - 5) + TIME '14:30'
        WHEN c.pattern = 'PX' THEN (CURRENT_DATE - 20) + TIME '14:30'
        WHEN c.pattern = 'PCX' THEN (c.start_date - 7) + TIME '09:15'
        WHEN c.pattern = 'PC'  THEN (c.start_date - 5) + TIME '16:45'
        WHEN c.pattern = 'P'   THEN CURRENT_TIMESTAMP - (c.pending_minutes_ago * INTERVAL '1 minute')
        END,
    -- updated_at
    CASE
        WHEN c.pattern = 'PCC' THEN c.end_date + TIME '11:00'
        WHEN c.pattern = 'PX' AND c.start_date < CURRENT_DATE THEN (c.start_date - 5) + TIME '14:45'
        WHEN c.pattern = 'PX' THEN (CURRENT_DATE - 20) + TIME '14:45'
        WHEN c.pattern = 'PCX' THEN (c.start_date - 2) + TIME '12:00'
        WHEN c.pattern = 'PC'  THEN (c.start_date - 5) + TIME '16:55'
        WHEN c.pattern = 'P'   THEN CURRENT_TIMESTAMP - (c.pending_minutes_ago * INTERVAL '1 minute')
        END
FROM computed c
    RETURNING id, user_id, created_at, updated_at
)


INSERT INTO reservation_status_history (id, reservation_id, old_status, new_status, changed_by, changed_at)

-- Każda rezerwacja zaczyna się od NULL -> PENDING (utworzona przez gościa)
SELECT gen_random_uuid(), ir.id, NULL, 'PENDING', ir.user_id, ir.created_at
FROM ins_res ir

UNION ALL

-- PENDING -> CONFIRMED (potwierdzona przez właściciela ~10 min po utworzeniu)
SELECT gen_random_uuid(), ir.id, 'PENDING', 'CONFIRMED', c.owner_id, ir.created_at + INTERVAL '10 minutes'
FROM ins_res ir
    JOIN computed c ON c.id = ir.id
WHERE c.pattern IN ('PCC', 'PCX', 'PC')

UNION ALL

-- PENDING -> CANCELLED (brak potwierdzenia w 15 min - automatyczne odwołanie)
SELECT gen_random_uuid(), ir.id, 'PENDING', 'CANCELLED', ir.user_id, ir.updated_at
FROM ins_res ir
         JOIN computed c ON c.id = ir.id
WHERE c.pattern = 'PX'

UNION ALL

-- CONFIRMED -> COMPLETED (po dacie check-out)
SELECT gen_random_uuid(), ir.id, 'CONFIRMED', 'COMPLETED', c.owner_id, ir.updated_at
FROM ins_res ir
         JOIN computed c ON c.id = ir.id
WHERE c.pattern = 'PCC'

UNION ALL

-- CONFIRMED -> CANCELLED (odwołanie przed check-in po wcześniejszym potwierdzeniu)
SELECT gen_random_uuid(), ir.id, 'CONFIRMED', 'CANCELLED', ir.user_id, ir.updated_at
FROM ins_res ir
         JOIN computed c ON c.id = ir.id
WHERE c.pattern = 'PCX';



WITH settlement_meta (
                      reservation_id, water_m3, deposit_amount, discount_amount, settlement_status
    ) AS (
    VALUES
        -- ===================== COMPLETED -> PAID (z rozliczeniem wody) =====================
        ('a1111111-1111-4111-8111-000000000001'::uuid, 2.40::numeric, 0.00, 0.00,   'PAID'),
        ('a1111111-1111-4111-8111-000000000002'::uuid, 5.60::numeric, 0.00, 0.00, 'PAID'),
        ('a1111111-1111-4111-8111-000000000003'::uuid, 2.40::numeric, 0.00, 0.00,   'PAID'),
        ('a1111111-1111-4111-8111-000000000004'::uuid, 0.40::numeric, 0.00, 0.00,   'PAID'),
        ('a1111111-1111-4111-8111-000000000005'::uuid, 3.00::numeric, 0.00, 0.00, 'PAID'),
        ('a1111111-1111-4111-8111-000000000006'::uuid, 1.60::numeric, 0.00, 0.00,   'PAID'),
        ('a1111111-1111-4111-8111-000000000007'::uuid, 1.20::numeric, 0.00, 0.00,   'PAID'),
        ('a1111111-1111-4111-8111-000000000008'::uuid, 0.60::numeric, 0.00, 0.00,   'PAID'),

        -- ===================== CANCELLED (PX - wygasłe PENDING) =====================
        ('a1111111-1111-4111-8111-000000000009'::uuid, NULL::numeric, 0.00, 0.00, 'CANCELLED'),
        ('a1111111-1111-4111-8111-000000000010'::uuid, NULL::numeric, 0.00, 0.00, 'CANCELLED'),

        -- ===================== CANCELLED (PCX - odwołane po potwierdzeniu) =====================
        ('a1111111-1111-4111-8111-000000000011'::uuid, NULL::numeric, 0.00, 0.00, 'CANCELLED'),
        ('a1111111-1111-4111-8111-000000000012'::uuid, NULL::numeric, 0.00, 0.00, 'CANCELLED'),

        -- ===================== CONFIRMED -> nocleg opłacony (PARTIALLY_PAID) =====================
        ('a1111111-1111-4111-8111-000000000013'::uuid, NULL::numeric, 0.00, 0.00,   'PARTIALLY_PAID'),
        ('a1111111-1111-4111-8111-000000000014'::uuid, NULL::numeric, 0.00, 0.00,   'PARTIALLY_PAID'),
        ('a1111111-1111-4111-8111-000000000016'::uuid, NULL::numeric, 0.00, 0.00, 'PARTIALLY_PAID'),
        ('a1111111-1111-4111-8111-000000000019'::uuid, NULL::numeric, 0.00, 0.00,   'PARTIALLY_PAID'),
        ('a1111111-1111-4111-8111-000000000021'::uuid, NULL::numeric, 0.00, 0.00,   'PARTIALLY_PAID'),

        -- ===================== CONFIRMED -> nocleg jeszcze nieopłacony (DRAFT) =====================
        ('a1111111-1111-4111-8111-000000000015'::uuid, NULL::numeric, 0.00, 0.00, 'DRAFT'),
        ('a1111111-1111-4111-8111-000000000017'::uuid, NULL::numeric, 0.00, 0.00, 'DRAFT'),
        ('a1111111-1111-4111-8111-000000000018'::uuid, NULL::numeric, 0.00, 0.00, 'DRAFT'),
        ('a1111111-1111-4111-8111-000000000020'::uuid, NULL::numeric, 0.00, 0.00, 'DRAFT'),
        ('a1111111-1111-4111-8111-000000000022'::uuid, NULL::numeric, 0.00, 0.00, 'DRAFT'),

        -- ===================== PENDING -> wystawiony settlement do opłaty (DRAFT) =====================
        ('a1111111-1111-4111-8111-000000000023'::uuid, NULL::numeric, 0.00, 0.00, 'DRAFT'),
        ('a1111111-1111-4111-8111-000000000024'::uuid, NULL::numeric, 0.00, 0.00, 'DRAFT'),

        -- ===================== CANCELLED (przyszłe, wygasłe PENDING) =====================
        ('a1111111-1111-4111-8111-000000000025'::uuid, NULL::numeric, 0.00, 0.00, 'CANCELLED'),
        ('a1111111-1111-4111-8111-000000000026'::uuid, NULL::numeric, 0.00, 0.00, 'CANCELLED')
),

     computed AS (
         SELECT
             r.id                       AS reservation_id,
             r.created_at,
             r.updated_at,
             r.price_per_night_snapshot,
             (r.end_date - r.start_date)::numeric(10,2) AS nights,
             r.total_price              AS accommodation_amount,
             sm.water_m3,
             COALESCE(sm.water_m3 * 18.50, 0.00)::numeric(12,2) AS utilities_amount,
             sm.deposit_amount,
             sm.discount_amount,
             sm.settlement_status
         FROM reservations r
                  JOIN settlement_meta sm ON sm.reservation_id = r.id
     ),

     final AS (
         SELECT
             c.*,
             (c.accommodation_amount + c.utilities_amount + c.deposit_amount - c.discount_amount)::numeric(12,2) AS total_amount,
             CASE
                 WHEN c.settlement_status IN ('DRAFT', 'CANCELLED') THEN 0.00
                 ELSE (c.accommodation_amount + c.utilities_amount + c.deposit_amount - c.discount_amount)
                 END::numeric(12,2) AS amount_paid_calc,
             CASE
                 WHEN c.settlement_status = 'DRAFT'
                     THEN (c.accommodation_amount + c.utilities_amount + c.deposit_amount - c.discount_amount)
                 ELSE 0.00
                 END::numeric(12,2) AS balance_due_calc
         FROM computed c
     ),


     ins_settlements AS (
INSERT INTO settlements (
    id, reservation_id, status,
    accommodation_amount, utilities_amount, deposit_amount, discount_amount,
    total_amount, amount_paid, balance_due,
    issued_at, paid_at, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    f.reservation_id,
    f.settlement_status,
    f.accommodation_amount,
    f.utilities_amount,
    f.deposit_amount,
    f.discount_amount,
    f.total_amount,
    f.amount_paid_calc,
    f.balance_due_calc,
    f.created_at,
    CASE WHEN f.settlement_status IN ('PARTIALLY_PAID', 'PAID') THEN f.updated_at ELSE NULL END,
    f.created_at,
    f.updated_at
FROM final f
    RETURNING id, reservation_id
)


INSERT INTO settlement_items (id, settlement_id, type, description, quantity, unit_price, amount, created_at)

SELECT
    gen_random_uuid(),
    s.id,
    'ACCOMMODATION',
    'Accommodation payment for (' || f.nights::int || ' night(s))',
    f.nights,
    f.price_per_night_snapshot,
    f.accommodation_amount,
    f.created_at
FROM ins_settlements s
         JOIN final f ON f.reservation_id = s.reservation_id

UNION ALL

SELECT
    gen_random_uuid(),
    s.id,
    'WATER',
    'Water consumption settlement (rate PLN 18.50/m3)',
    f.water_m3,
    18.50,
    f.utilities_amount,
    f.updated_at
FROM ins_settlements s
         JOIN final f ON f.reservation_id = s.reservation_id
WHERE f.water_m3 IS NOT NULL;



WITH reading_data (
                   media_reading_id, reservation_id, final_reading, final_confidence
    ) AS (
    VALUES
        ('d1111111-1111-4111-8111-000000000001'::uuid, 'a1111111-1111-4111-8111-000000000001'::uuid, 92.383000::numeric(12,6), 0.778662::numeric(12,6)),
        ('d1111111-1111-4111-8111-000000000002'::uuid, 'a1111111-1111-4111-8111-000000000002'::uuid, 95.583000::numeric(12,6), 0.798311::numeric(12,6)),
        ('d1111111-1111-4111-8111-000000000003'::uuid, 'a1111111-1111-4111-8111-000000000003'::uuid, 92.383000::numeric(12,6), 0.786070::numeric(12,6)),
        ('d1111111-1111-4111-8111-000000000004'::uuid, 'a1111111-1111-4111-8111-000000000004'::uuid, 90.383000::numeric(12,6), 0.775040::numeric(12,6)),
        ('d1111111-1111-4111-8111-000000000005'::uuid, 'a1111111-1111-4111-8111-000000000005'::uuid, 92.983000::numeric(12,6), 0.751107::numeric(12,6)),
        ('d1111111-1111-4111-8111-000000000006'::uuid, 'a1111111-1111-4111-8111-000000000006'::uuid, 91.583000::numeric(12,6), 0.761524::numeric(12,6)),
        ('d1111111-1111-4111-8111-000000000007'::uuid, 'a1111111-1111-4111-8111-000000000007'::uuid, 91.183000::numeric(12,6), 0.777656::numeric(12,6)),
        ('d1111111-1111-4111-8111-000000000008'::uuid, 'a1111111-1111-4111-8111-000000000008'::uuid, 90.583000::numeric(12,6), 0.742221::numeric(12,6))
),


     computed AS (
         SELECT
             rd.media_reading_id,
             rd.reservation_id,
             rd.final_reading,
             rd.final_confidence,
             s.id AS settlement_id,
             r.start_date,
             r.end_date
         FROM reading_data rd
                  JOIN settlements   s ON s.reservation_id = rd.reservation_id
                  JOIN reservations  r ON r.id = rd.reservation_id
     ),


     ins_readings AS (
INSERT INTO media_readings (
    id, settlement_id, utility_type,
    initial_reading, initial_reading_status, initial_reading_source, initial_confidence_score,
    final_reading, final_reading_status, final_reading_source, final_confidence_score,
    unit_price,
    created_at, updated_at
)
SELECT
    c.media_reading_id,
    c.settlement_id,
    'WATER',
    89.983000::numeric(12,6),
    'AUTO_APPROVED',
    'OCR',
    0.784642::numeric(12,6),
    c.final_reading,
    'AUTO_APPROVED',
    'OCR',
    c.final_confidence,
    18.50,
    c.start_date + TIME '12:00',
    c.end_date   + TIME '11:00'
FROM computed c
    RETURNING id
)


INSERT INTO media_reading_upload_attempts (
    media_reading_id, meter_image, ocr_value, confidence_score, status, reading_type, attempted_at
)


SELECT
    ir.id,
    pg_read_binary_file('/images/check_in.jpg'),
    '89.983000',
    0.784642,
    'AUTO_APPROVED',
    'INITIAL',
    c.start_date + TIME '12:00'
FROM ins_readings ir
         JOIN computed c ON c.media_reading_id = ir.id

UNION ALL


SELECT
    ir.id,
    CASE c.reservation_id
        WHEN 'a1111111-1111-4111-8111-000000000001' THEN pg_read_binary_file('/images/check_out_1.png')
        WHEN 'a1111111-1111-4111-8111-000000000002' THEN pg_read_binary_file('/images/check_out_2.png')
        WHEN 'a1111111-1111-4111-8111-000000000003' THEN pg_read_binary_file('/images/check_out_3.png')
        WHEN 'a1111111-1111-4111-8111-000000000004' THEN pg_read_binary_file('/images/check_out_4.png')
        WHEN 'a1111111-1111-4111-8111-000000000005' THEN pg_read_binary_file('/images/check_out_5.png')
        WHEN 'a1111111-1111-4111-8111-000000000006' THEN pg_read_binary_file('/images/check_out_6.png')
        WHEN 'a1111111-1111-4111-8111-000000000007' THEN pg_read_binary_file('/images/check_out_7.png')
        WHEN 'a1111111-1111-4111-8111-000000000008' THEN pg_read_binary_file('/images/check_out_8.png')
        END,
    CASE c.reservation_id
        WHEN 'a1111111-1111-4111-8111-000000000001' THEN '92.383000'
        WHEN 'a1111111-1111-4111-8111-000000000002' THEN '95.583000'
        WHEN 'a1111111-1111-4111-8111-000000000003' THEN '92.383000'
        WHEN 'a1111111-1111-4111-8111-000000000004' THEN '90.383000'
        WHEN 'a1111111-1111-4111-8111-000000000005' THEN '92.983000'
        WHEN 'a1111111-1111-4111-8111-000000000006' THEN '91.583000'
        WHEN 'a1111111-1111-4111-8111-000000000007' THEN '91.183000'
        WHEN 'a1111111-1111-4111-8111-000000000008' THEN '90.583000'
        END,
    CASE c.reservation_id
        WHEN 'a1111111-1111-4111-8111-000000000001' THEN 0.778662
        WHEN 'a1111111-1111-4111-8111-000000000002' THEN 0.798311
        WHEN 'a1111111-1111-4111-8111-000000000003' THEN 0.786070
        WHEN 'a1111111-1111-4111-8111-000000000004' THEN 0.771110
        WHEN 'a1111111-1111-4111-8111-000000000005' THEN 0.751107
        WHEN 'a1111111-1111-4111-8111-000000000006' THEN 0.761524
        WHEN 'a1111111-1111-4111-8111-000000000007' THEN 0.777656
        WHEN 'a1111111-1111-4111-8111-000000000008' THEN 0.742221
        END,
    'AUTO_APPROVED',
    'FINAL',
    c.end_date + TIME '11:00'
FROM ins_readings ir
         JOIN computed c ON c.media_reading_id = ir.id;



