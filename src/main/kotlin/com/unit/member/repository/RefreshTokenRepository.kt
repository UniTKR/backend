package com.unit.member.repository

import com.unit.member.entity.RefreshToken
import com.unit.member.enums.RefreshTokenStatus
import org.springframework.data.jpa.repository.JpaRepository

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByTokenHashAndStatus(
        tokenHash: ByteArray,
        status: RefreshTokenStatus = RefreshTokenStatus.ACTIVE,
    ): RefreshToken?

    fun findAllByMemberIdAndStatus(
        memberId: Long,
        status: RefreshTokenStatus = RefreshTokenStatus.ACTIVE,
    ): List<RefreshToken>
}
