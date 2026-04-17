ALTER TABLE reservations
    ADD COLUMN checked_in_at TIMESTAMP,
    ADD COLUMN no_show_marked_at TIMESTAMP;
