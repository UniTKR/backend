package com.unit.member.repository

import com.unit.member.entity.RefreshToken
import com.unit.member.enums.RefreshTokenStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.test.context.TestPropertySource
import java.time.LocalDateTime

@DataJpaTest
@TestPropertySource(
    properties = [
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
@DisplayName("RefreshToken Repository 테스트")
class RefreshTokenRepositoryTest @Autowired constructor(
    private val refreshTokenRepository: RefreshTokenRepository,
) {

    @Test
    @DisplayName("토큰 해시와 상태로 RefreshToken을 조회한다")
    fun findByTokenHashAndStatus() {
        val tokenHash = ByteArray(32) { 1 }
        val saved = refreshTokenRepository.save(createRefreshToken(tokenHash = tokenHash))

        val found = refreshTokenRepository.findByTokenHashAndStatus(tokenHash)

        assertThat(found).isNotNull
        assertThat(found?.id).isEqualTo(saved.id)
    }

    @Test
    @DisplayName("상태가 다른 RefreshToken은 토큰 해시 조회에서 제외한다")
    fun findByTokenHashAndStatus_differentStatus() {
        val tokenHash = ByteArray(32) { 1 }
        refreshTokenRepository.save(
            createRefreshToken(
                tokenHash = tokenHash,
                status = RefreshTokenStatus.REVOKED,
            ),
        )

        val found = refreshTokenRepository.findByTokenHashAndStatus(tokenHash)

        assertThat(found).isNull()
    }

    @Test
    @DisplayName("회원 ID와 상태로 RefreshToken 목록을 조회한다")
    fun findAllByMemberIdAndStatus() {
        val memberId = 1L
        val active = refreshTokenRepository.save(
            createRefreshToken(memberId = memberId, tokenHash = ByteArray(32) { 1 }),
        )
        refreshTokenRepository.save(
            createRefreshToken(
                memberId = memberId,
                tokenHash = ByteArray(32) { 2 },
                status = RefreshTokenStatus.REVOKED,
            ),
        )
        refreshTokenRepository.save(
            createRefreshToken(memberId = 2L, tokenHash = ByteArray(32) { 3 }),
        )

        val tokens = refreshTokenRepository.findAllByMemberIdAndStatus(memberId)

        assertThat(tokens.map { it.id }).containsExactly(active.id)
    }

    private fun createRefreshToken(
        memberId: Long = 1L,
        tokenHash: ByteArray = ByteArray(32) { 1 },
        status: RefreshTokenStatus = RefreshTokenStatus.ACTIVE,
        expiresAt: LocalDateTime = LocalDateTime.of(2026, 5, 15, 12, 0),
    ): RefreshToken {
        return RefreshToken(
            memberId = memberId,
            tokenHash = tokenHash,
            status = status,
            expiresAt = expiresAt,
        )
    }
}
