ALTER TABLE users ADD COLUMN first_name VARCHAR(100);
ALTER TABLE users ADD COLUMN last_name VARCHAR(100);

-- Update existing demo data to have valid first and last names
UPDATE users SET first_name = 'Admin', last_name = 'User' WHERE username = 'admin';
UPDATE users SET first_name = 'John', last_name = 'Owner' WHERE username = 'owner1';
UPDATE users SET first_name = 'Jane', last_name = 'Owner' WHERE username = 'owner2';
UPDATE users SET first_name = 'Bob', last_name = 'Guest' WHERE username = 'guest1';
UPDATE users SET first_name = 'Alice', last_name = 'Guest' WHERE username = 'guest2';
