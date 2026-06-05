ALTER TABLE complaints
    ADD COLUMN train_no    VARCHAR(50)  NULL AFTER station_name,
    ADD COLUMN line_code   VARCHAR(20)  NULL AFTER train_no,
    ADD COLUMN line_name   VARCHAR(50)  NULL AFTER line_code,
    ADD COLUMN direction   VARCHAR(50)  NULL AFTER line_name,
    ADD COLUMN destination VARCHAR(50)  NULL AFTER direction;
