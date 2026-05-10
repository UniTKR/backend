package com.unit.member.dto

import jakarta.validation.constraints.*

data class MemberSignupRequest(
    @field:NotBlank
    @field:Email
    val email: String,

    @field:NotBlank
    @field:Size(min = 8, max = 72)
    val password: String,

    @field:NotBlank
    @field:Size(min = 2, max = 40)
    @field:Pattern(regexp = "^[가-힣a-zA-Z0-9_-]+$")
    val nickname: String,

    @field:NotNull
    val schoolId: Long?
)
