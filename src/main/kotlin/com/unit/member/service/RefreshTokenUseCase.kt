package com.unit.member.service

import com.unit.member.dto.AuthLogoutRequest
import com.unit.member.dto.AuthTokenRefreshRequest
import com.unit.member.dto.AuthTokenRefreshResponse

interface RefreshTokenUseCase {

    fun issue(memberId: Long): String

    fun refresh(request: AuthTokenRefreshRequest): AuthTokenRefreshResponse

    fun logout(request: AuthLogoutRequest)

    fun revokeAll(memberId: Long)
}