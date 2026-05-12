ALTER TABLE users
DROP INDEX uk_users_email_hash,
DROP INDEX uk_users_phone_hash,
DROP INDEX uk_users_nickname;

ALTER TABLE users
    ADD COLUMN active_email_hash BINARY(32)
        GENERATED ALWAYS AS (
            CASE WHEN deleted_at IS NULL THEN email_hash ELSE NULL END
        ) STORED,
    ADD COLUMN active_phone_hash BINARY(32)
        GENERATED ALWAYS AS (
            CASE WHEN deleted_at IS NULL THEN phone_hash ELSE NULL END
        ) STORED,
    ADD COLUMN active_nickname VARCHAR(40)
        GENERATED ALWAYS AS (
            CASE WHEN deleted_at IS NULL THEN nickname ELSE NULL END
        ) STORED,
    ADD UNIQUE KEY uk_users_active_email_hash (active_email_hash),
    ADD UNIQUE KEY uk_users_active_phone_hash (active_phone_hash),
    ADD UNIQUE KEY uk_users_active_nickname (active_nickname),
    ADD INDEX idx_users_email_hash_deleted_at_id (email_hash, deleted_at, id);

ALTER TABLE user_school_verifications
    DROP INDEX uk_user_school_verifications_user_school,
    ADD UNIQUE KEY uk_user_school_verifications_user (user_id);
