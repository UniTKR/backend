package com.unit.member.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "unit.consent")
data class MemberConsentProperties(
    val termsVersion: String,
    val privacyPolicyVersion: String,
)