package com.unit.member.dto

import com.unit.member.enums.MemberStatus
import com.unit.member.enums.UserSchoolVerificationStatus

data class MemberMeResponse(
    val memberId: Long,
    val email: String?,
    val nickname: String,
    val profileImageUrl: String?,
    val status: MemberStatus,
    val trustScore: Int,
    val school: MemberSchoolResponse?,
)

data class MemberSchoolResponse(
    val schoolId: Long,
    val name: String,
    val verificationStatus: UserSchoolVerificationStatus,
    val verifiedEmail: String?,
)