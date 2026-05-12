package com.unit.member.controller

import com.unit.member.dto.AuthLoginRequest
import com.unit.member.dto.AuthLoginResponse
import com.unit.member.dto.AuthLogoutRequest
import com.unit.member.dto.AuthTokenRefreshRequest
import com.unit.member.dto.AuthTokenRefreshResponse
import com.unit.member.service.AuthLoginUseCase
import com.unit.member.service.RefreshTokenUseCase
import com.unit.platform.web.response.ApiResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authLoginUseCase: AuthLoginUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase
) {
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: AuthLoginRequest): ApiResponse<AuthLoginResponse> {
        return ApiResponse.ok(authLoginUseCase.login(request))
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: AuthTokenRefreshRequest): ApiResponse<AuthTokenRefreshResponse> {
        return ApiResponse.ok(refreshTokenUseCase.refresh(request))
    }

    @PostMapping("/logout")
    fun logout(@Valid @RequestBody request: AuthLogoutRequest): ApiResponse<Unit> {
        refreshTokenUseCase.logout(request)
        return ApiResponse.ok()
    }
}