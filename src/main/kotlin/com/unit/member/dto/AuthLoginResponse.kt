package com.unit.member.dto

import com.unit.member.enums.MemberStatus

data class AuthLoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val member: AuthenticatedMemberResponse,
)

data class AuthenticatedMemberResponse(
    val memberId: Long,
    val nickname: String,
    val status: MemberStatus,
)