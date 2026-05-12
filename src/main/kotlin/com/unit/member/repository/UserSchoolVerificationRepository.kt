package com.unit.member.repository

import com.unit.member.entity.UserSchoolVerification
import com.unit.member.enums.UserSchoolVerificationStatus
import org.springframework.data.jpa.repository.JpaRepository

interface UserSchoolVerificationRepository : JpaRepository<UserSchoolVerification, Long> {
    fun findByMemberIdAndSchoolId(
        memberId: Long,
        schoolId: Long,
    ): UserSchoolVerification?

    fun findByMemberIdAndStatus(
        memberId: Long,
        status: UserSchoolVerificationStatus = UserSchoolVerificationStatus.VERIFIED,
    ): UserSchoolVerification?

    fun existsByMemberIdAndSchoolIdAndStatus(
        memberId: Long,
        schoolId: Long,
        status: UserSchoolVerificationStatus = UserSchoolVerificationStatus.VERIFIED,
    ): Boolean
}
