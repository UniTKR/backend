package com.unit.member.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "unit.security.refresh-token")
data class RefreshTokenProperties(
    val expirationSeconds: Long
)
