package com.unit.member.entity

import com.unit.member.enums.RefreshTokenStatus
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("리프레시 토큰 엔티티 테스트")
class RefreshTokenTest {

    lateinit var refreshToken: RefreshToken

    @BeforeEach
    fun setUp() {
        refreshToken = RefreshToken(
            memberId = 1L,
            tokenHash = ByteArray(32) { 1 },
            expiresAt = LocalDateTime.of(2026, 5, 15, 12, 0)
        )
    }

    @Test
    @DisplayName("Revoke 정상 동작")
    fun revoke() {
        val now = LocalDateTime.of(2026, 5, 8, 12, 0)

        refreshToken.revoke(now)

        assertThat(refreshToken.status).isEqualTo(RefreshTokenStatus.REVOKED)
        assertThat(refreshToken.revokedAt).isEqualTo(now)

    }

    @Test
    @DisplayName("Rotate 정상 동작")
    fun rotate() {
        val now = LocalDateTime.of(2026, 5, 8, 12, 0)

        refreshToken.rotate(now)

        assertThat(refreshToken.status).isEqualTo(RefreshTokenStatus.ROTATED)
        assertThat(refreshToken.revokedAt).isEqualTo(now)
    }

    @Test
    @DisplayName("markUsed 정상 동작")
    fun markUsed() {
        val now = LocalDateTime.of(2026, 5, 8, 12, 0)

        refreshToken.markUsed(now)

        assertThat(refreshToken.lastUsedAt).isEqualTo(now)
    }
}