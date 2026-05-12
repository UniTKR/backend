package com.unit.member.dto

import jakarta.validation.constraints.*

data class MemberSignupRequest(
    @field:NotBlank
    @field:Email
    val email: String,

    @field:NotBlank
    @field:Size(min = 8, max = 72)
    @field:Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?`~])\\S+$",
        message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 하며 공백을 사용할 수 없습니다."
    )
    val password: String,

    @field:NotBlank
    @field:Size(min = 2, max = 40)
    @field:Pattern(regexp = "^[가-힣a-zA-Z0-9_-]+$")
    val nickname: String,

    @field:NotNull
    val schoolId: Long?
)
