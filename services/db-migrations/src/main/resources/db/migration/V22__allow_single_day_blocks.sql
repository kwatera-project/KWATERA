ALTER TABLE reservations DROP CONSTRAINT check_dates;
ALTER TABLE reservations ADD CONSTRAINT check_dates CHECK (end_date >= start_date);
