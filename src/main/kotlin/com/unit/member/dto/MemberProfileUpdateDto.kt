package com.unit.member.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class MemberProfileUpdateRequest(

    @field:NotBlank
    @field:Size(min = 2, max = 40)
    @field:Pattern(regexp = "^[가-힣a-zA-Z0-9_-]+$")
    val nickname: String,

    @field:Size(max = 500)
    val profileImageUrl: String? = null,
)

data class MemberProfileUpdateResponse(
    val memberId: Long,
    val nickname: String,
    val profileImageUrl: String?,
)
