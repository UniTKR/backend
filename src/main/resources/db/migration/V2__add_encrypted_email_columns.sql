ALTER TABLE users
    ADD COLUMN email_encrypted VARBINARY(512) NULL AFTER email_hash;

ALTER TABLE user_school_verifications
    ADD COLUMN verified_email_encrypted VARBINARY(512) NULL AFTER verified_email_hash;
