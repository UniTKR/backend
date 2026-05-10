ALTER TABLE users
    ADD COLUMN password_hash VARCHAR(100) NULL AFTER phone_hash;

ALTER TABLE users
    ADD CONSTRAINT uk_users_nickname UNIQUE (nickname);