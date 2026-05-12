package com.unit.member.service

import com.unit.member.dto.SchoolAuthDto.*

interface SchoolEmailVerificationUseCase {

    fun request(memberId: Long, request: SchoolEmailVerificationRequest): SchoolEmailVerificationResponse

    fun confirm(memberId: Long, request: SchoolEmailVerificationConfirmRequest): SchoolEmailVerificationConfirmResponse
}