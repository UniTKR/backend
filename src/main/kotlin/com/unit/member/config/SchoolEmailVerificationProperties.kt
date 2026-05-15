package com.unit.member.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "unit.school-email-verification")
data class SchoolEmailVerificationProperties(
    val codeExpirationSeconds: Long = 300,
    val resendCooldownSeconds: Long = 60,
    val maxAttemptCount: Int = 5,
    val verificationExpirationDays: Long = 365,
)