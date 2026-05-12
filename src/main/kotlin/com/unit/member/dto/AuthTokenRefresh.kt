package com.unit.member.dto

import jakarta.validation.constraints.NotBlank

data class AuthTokenRefreshRequest(
    @field:NotBlank
    val refreshToken: String
)

data class AuthTokenRefreshResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
)

data class AuthLogoutRequest(
    @field:NotBlank
    val refreshToken: String,
)