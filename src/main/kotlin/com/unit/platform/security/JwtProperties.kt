package com.unit.platform.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "unit.security.jwt")
data class JwtProperties(
    val issuer: String,
    val secret: String,
    val accessTokenExpirationSeconds: Long
)