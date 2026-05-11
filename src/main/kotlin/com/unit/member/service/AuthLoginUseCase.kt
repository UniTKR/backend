package com.unit.member.service

import com.unit.member.dto.AuthLoginRequest
import com.unit.member.dto.AuthLoginResponse

interface AuthLoginUseCase {

    fun login(request: AuthLoginRequest): AuthLoginResponse
}