package com.unit.member.dto

import com.unit.member.enums.UserSchoolVerificationStatus
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern

class SchoolAuthDto {

    data class SchoolEmailVerificationRequest(
        @field:NotNull
        val schoolId: Long?,

        @field:NotBlank
        @field:Email
        val email: String,
    )

    data class SchoolEmailVerificationResponse(
        val schoolId: Long,
        val email: String,
        val expiresIn: Long,
    )

    data class SchoolEmailVerificationConfirmRequest(
        @field:NotNull
        val schoolId: Long?,

        @field:NotBlank
        @field:Email
        val email: String,

        @field:NotBlank
        @field:Pattern(regexp = "^\\d{6}$")
        val code: String,
    )

    data class SchoolEmailVerificationConfirmResponse(
        val schoolId: Long,
        val status: UserSchoolVerificationStatus,
    )
}