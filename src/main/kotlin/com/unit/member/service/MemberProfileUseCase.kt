package com.unit.member.service

import com.unit.member.dto.MemberProfileUpdateRequest
import com.unit.member.dto.MemberProfileUpdateResponse

interface MemberProfileUseCase {

    fun updateProfile(memberId: Long, request: MemberProfileUpdateRequest): MemberProfileUpdateResponse
}