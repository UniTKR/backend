package com.unit.member.service

import com.unit.member.dto.MemberSignupRequest
import com.unit.member.dto.MemberSignupResponse

interface MemberSignupUseCase {
    fun signup(request: MemberSignupRequest): MemberSignupResponse
}