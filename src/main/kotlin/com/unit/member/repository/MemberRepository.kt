package com.unit.member.repository

import com.unit.member.entity.Member
import com.unit.member.enums.MemberStatus
import org.springframework.data.jpa.repository.JpaRepository

interface MemberRepository : JpaRepository<Member, Long> {
    fun existsByEmailHashAndDeletedAtIsNull(emailHash: ByteArray): Boolean

    fun findByEmailHashAndDeletedAtIsNull(emailHash: ByteArray): Member?

    fun findByIdAndStatusAndDeletedAtIsNull(
        id: Long,
        status: MemberStatus = MemberStatus.ACTIVE,
    ): Member?

    fun existsByNicknameAndDeletedAtIsNull(nickname: String): Boolean

    fun findByIdAndStatusInAndDeletedAtIsNull(
        id: Long,
        statuses: Collection<MemberStatus>,
    ): Member?
}