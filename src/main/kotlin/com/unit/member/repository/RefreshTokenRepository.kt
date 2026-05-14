package com.unit.member.repository

import com.unit.member.entity.RefreshToken
import com.unit.member.enums.RefreshTokenStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByTokenHashAndStatus(
        tokenHash: ByteArray,
        status: RefreshTokenStatus = RefreshTokenStatus.ACTIVE,
    ): RefreshToken?

    fun findAllByMemberIdAndStatus(
        memberId: Long,
        status: RefreshTokenStatus = RefreshTokenStatus.ACTIVE,
    ): List<RefreshToken>
}
