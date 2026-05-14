CREATE TABLE member_consents
(
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    user_id        BIGINT      NOT NULL,
    consent_type   VARCHAR(40) NOT NULL,
    policy_version VARCHAR(40) NOT NULL,
    agreed         BOOLEAN     NOT NULL,
    agreed_at      DATETIME(6) NULL,
    withdrawn_at   DATETIME(6) NULL,
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,

    CONSTRAINT pk_member_consents PRIMARY KEY (id),
    CONSTRAINT uk_member_consents_user_type_version UNIQUE (user_id, consent_type, policy_version),
    CONSTRAINT chk_member_consents_type CHECK (consent_type IN ('TERMS_OF_SERVICE', 'PRIVACY_POLICY')),
    CONSTRAINT fk_member_consents_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    KEY            idx_member_consents_user_type (user_id, consent_type)
);
