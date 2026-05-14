package com.unit.member.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "unit.security.email-encryption")
data class EmailEncryptionProperties(
    val keyBase64: String,
)