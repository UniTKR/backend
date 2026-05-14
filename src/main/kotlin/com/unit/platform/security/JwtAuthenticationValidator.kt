package com.unit.platform.security

import org.springframework.security.oauth2.jwt.Jwt

interface JwtAuthenticationValidator {
    fun isValid(jwt: Jwt): Boolean
}