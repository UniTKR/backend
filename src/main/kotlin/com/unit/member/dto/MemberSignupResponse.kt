package com.unit.member.dto

import com.unit.member.enums.MemberStatus
import com.unit.member.enums.UserSchoolVerificationStatus

data class MemberSignupResponse(
    val memberId: Long,
    val nickname: String,
    val status: MemberStatus,
    val schoolVerificationStatus: UserSchoolVerificationStatus
)
