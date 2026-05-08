package com.unit.member.repository

import com.unit.member.entity.SchoolEmailVerificationCode
import com.unit.member.enums.SchoolEmailVerificationStatus
import org.springframework.data.jpa.repository.JpaRepository

interface SchoolEmailVerificationCodeRepository : JpaRepository<SchoolEmailVerificationCode, Long> {
    fun findTopBySchoolIdAndEmailHashAndStatusOrderByCreatedAtDesc(
        schoolId: Long,
        emailHash: ByteArray,
        status: SchoolEmailVerificationStatus = SchoolEmailVerificationStatus.PENDING,
    ): SchoolEmailVerificationCode?

    fun findTopByMemberIdAndStatusOrderByCreatedAtDesc(
        memberId: Long,
        status: SchoolEmailVerificationStatus = SchoolEmailVerificationStatus.PENDING,
    ): SchoolEmailVerificationCode?
}