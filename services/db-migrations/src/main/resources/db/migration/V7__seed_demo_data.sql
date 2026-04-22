INSERT INTO users (id, role, username, email, password)
VALUES ('11111111-1111-1111-1111-111111111111', 'ADMIN', 'admin', 'admin@example.com',
        '$2a$12$Nn3JKEeeEFlGpJPGNCQ3RekQIauvI1nNxcPO7mGaO0qwnHODq5mo6'),
       ('22222222-2222-2222-2222-222222222222', 'OWNER', 'owner1', 'owner1@example.com',
        '$2a$12$Nn3JKEeeEFlGpJPGNCQ3RekQIauvI1nNxcPO7mGaO0qwnHODq5mo6'),
       ('33333333-3333-3333-3333-333333333333', 'OWNER', 'owner2', 'owner2@example.com',
        '$2a$12$Nn3JKEeeEFlGpJPGNCQ3RekQIauvI1nNxcPO7mGaO0qwnHODq5mo6'),
       ('44444444-4444-4444-4444-444444444444', 'GUEST', 'guest1', 'guest1@example.com',
        '$2a$12$Nn3JKEeeEFlGpJPGNCQ3RekQIauvI1nNxcPO7mGaO0qwnHODq5mo6'),
       ('55555555-5555-5555-5555-555555555555', 'GUEST', 'guest2', 'guest2@example.com',
        '$2a$12$Nn3JKEeeEFlGpJPGNCQ3RekQIauvI1nNxcPO7mGaO0qwnHODq5mo6') ON CONFLICT (id) DO NOTHING;

INSERT INTO properties (id, owner_id, title, description, location)
VALUES ('aaaa1111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222',
        'Sunset Apartments', 'Modern apartments in city center', 'Warsaw'),

       ('bbbb2222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333',
        'Mountain View Lodge', 'Cozy place near mountains', 'Zakopane') ON CONFLICT (id) DO NOTHING;

INSERT INTO units (id, property_id, name, description, price_per_night, capacity)
VALUES
-- Warsaw
('11111111-1111-1111-1111-222222222222', 'aaaa1111-1111-1111-1111-111111111111',
 'Studio 1', 'Small studio', 200.00, 2),

('22222222-2222-2222-2222-333333333333', 'aaaa1111-1111-1111-1111-111111111111',
 'Apartment Deluxe', 'Large apartment with balcony', 350.00, 4),
-- Zakopane
('33333333-3333-3333-3333-444444444444', 'bbbb2222-2222-2222-2222-222222222222',
 'Room 1', 'Mountain view room', 180.00, 2),

('44444444-4444-4444-4444-555555555555', 'bbbb2222-2222-2222-2222-222222222222',
 'Family Suite', 'Perfect for families', 400.00, 5) ON CONFLICT (id) DO NOTHING;

INSERT INTO reservations (id, user_id, unit_id, start_date, end_date, status)
VALUES

-- 1. COMPLETED reservation
('00000000-0000-0000-0000-111111111111',
 '44444444-4444-4444-4444-444444444444',
 '11111111-1111-1111-1111-222222222222',
 '2025-01-10', '2025-01-15', 'COMPLETED'),

-- 2. CONFIRMED reservation
('00000000-0000-0000-0000-222222222222',
 '55555555-5555-5555-5555-555555555555',
 '22222222-2222-2222-2222-333333333333',
 CURRENT_DATE - INTERVAL '1 day',
 CURRENT_DATE + INTERVAL '3 days',
 'CONFIRMED'),

-- 3. PENDING reservation
('00000000-0000-0000-0000-333333333333',
 '44444444-4444-4444-4444-444444444444',
 '33333333-3333-3333-3333-444444444444',
 CURRENT_DATE + INTERVAL '10 days',
 CURRENT_DATE + INTERVAL '15 days',
 'PENDING'),

-- 4. CANCELLED reservation
('00000000-0000-0000-0000-444444444444',
 '55555555-5555-5555-5555-555555555555',
 '44444444-4444-4444-4444-555555555555',
 CURRENT_DATE + INTERVAL '5 days',
 CURRENT_DATE + INTERVAL '8 days',
 'CANCELLED'),

-- 5. Back-to-back booking (unit from case 1)
('00000000-0000-0000-0000-555555555555',
 '44444444-4444-4444-4444-444444444444',
 '11111111-1111-1111-1111-222222222222',
 '2025-01-15', '2025-01-20', 'CONFIRMED') ON CONFLICT (id) DO NOTHING;

INSERT INTO property_images (id, property_id, url, is_main)
VALUES
-- Images for Sunset Apartments (Warsaw)
('11111111-1111-1111-1111-111111111111',
 'aaaa1111-1111-1111-1111-111111111111',
 'https://images.pexels.com/photos/34360416/pexels-photo-34360416.jpeg',
 TRUE),

('22222222-2222-2222-2222-222222222222',
 'aaaa1111-1111-1111-1111-111111111111',
 'https://images.pexels.com/photos/34360412/pexels-photo-34360412.jpeg',
 FALSE),

('33333333-3333-3333-3333-333333333333',
 'aaaa1111-1111-1111-1111-111111111111',
 'https://images.pexels.com/photos/34360417/pexels-photo-34360417.jpeg',
 FALSE),

-- Images for Mountain View Lodge (Zakopane)
('44444444-4444-4444-4444-444444444444',
 'bbbb2222-2222-2222-2222-222222222222',
 'https://images.pexels.com/photos/6552568/pexels-photo-6552568.jpeg',
 TRUE),

('55555555-5555-5555-5555-555555555555',
 'bbbb2222-2222-2222-2222-222222222222',
 'https://images.pexels.com/photos/11789386/pexels-photo-11789386.jpeg',
 FALSE) ON CONFLICT (id) DO NOTHING;

INSERT INTO unit_images (id, unit_id, url, is_main)
VALUES
-- Studio 1 (Warsaw)
('11111111-1111-1111-1111-111111111111',
 '11111111-1111-1111-1111-222222222222',
 'https://images.pexels.com/photos/19846366/pexels-photo-19846366.jpeg',
 TRUE),

('22222222-2222-2222-2222-222222222222',
 '11111111-1111-1111-1111-222222222222',
 'https://images.pexels.com/photos/19846387/pexels-photo-19846387.jpeg',
 FALSE),

-- Apartment Deluxe (Warsaw)
('33333333-3333-3333-3333-333333333333',
 '22222222-2222-2222-2222-333333333333',
 'https://images.pexels.com/photos/19916704/pexels-photo-19916704.jpeg',
 TRUE),

('44444444-4444-4444-4444-444444444444',
 '22222222-2222-2222-2222-333333333333',
 'https://images.pexels.com/photos/19916700/pexels-photo-19916700.jpeg',
 FALSE),

-- Room 1 (Zakopane)
('55555555-5555-5555-5555-555555555555',
 '33333333-3333-3333-3333-444444444444',
 'https://images.pexels.com/photos/30708768/pexels-photo-30708768.jpeg',
 TRUE),

-- Family Suite (Zakopane)
('66666666-6666-6666-6666-666666666666',
 '44444444-4444-4444-4444-555555555555',
 'https://images.pexels.com/photos/7598367/pexels-photo-7598367.jpeg',
 TRUE),

('77777777-7777-7777-7777-777777777777',
 '44444444-4444-4444-4444-555555555555',
 'https://images.pexels.com/photos/7598361/pexels-photo-7598361.jpeg',
 FALSE) ON CONFLICT (id) DO NOTHING;