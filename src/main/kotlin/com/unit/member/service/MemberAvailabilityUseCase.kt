package com.unit.member.service

import com.unit.member.dto.MemberAvailabilityResponse

interface MemberAvailabilityUseCase {
    fun checkEmailAvailability(email: String): MemberAvailabilityResponse

    fun checkNicknameAvailability(nickname: String): MemberAvailabilityResponse
}
