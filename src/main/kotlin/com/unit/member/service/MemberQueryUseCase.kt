package com.unit.member.service

import com.unit.member.dto.MemberMeResponse

interface MemberQueryUseCase {
    fun getMe(memberId: Long): MemberMeResponse
}