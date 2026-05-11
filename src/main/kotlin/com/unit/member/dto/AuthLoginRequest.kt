package com.unit.member.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class AuthLoginRequest(
    @field:NotBlank
    @field:Email
    val email: String,

    @field:NotBlank
    val password: String
)
