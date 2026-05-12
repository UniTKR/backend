package com.unit.member.service

import com.unit.member.config.RefreshTokenProperties
import com.unit.member.dto.AuthLogoutRequest
import com.unit.member.dto.AuthTokenRefreshRequest
import com.unit.member.entity.Member
import com.unit.member.entity.RefreshToken
import com.unit.member.enums.MemberStatus
import com.unit.member.enums.RefreshTokenStatus
import com.unit.member.exception.MemberErrorCode
import com.unit.member.repository.MemberRepository
import com.unit.member.repository.RefreshTokenRepository
import com.unit.member.util.RefreshTokenGenerator
import com.unit.member.util.TokenHasher
import com.unit.platform.error.BusinessException
import com.unit.platform.security.JwtTokenProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import java.time.LocalDateTime
import kotlin.test.Test

@DisplayName("RefreshTokenService 테스트")
class RefreshTokenServiceTest {

    private val refreshTokenRepository = mockk<RefreshTokenRepository>()
    private val memberRepository = mockk<MemberRepository>()
    private val refreshTokenGenerator = mockk<RefreshTokenGenerator>()
    private val tokenHasher = mockk<TokenHasher>()
    private val refreshTokenProperties = RefreshTokenProperties(expirationSeconds = 1_209_600L)
    private val jwtTokenProvider = mockk<JwtTokenProvider>()

    private val refreshTokenService = RefreshTokenService(
        refreshTokenRepository = refreshTokenRepository,
        memberRepository = memberRepository,
        refreshTokenGenerator = refreshTokenGenerator,
        tokenHasher = tokenHasher,
        refreshTokenProperties = refreshTokenProperties,
        jwtTokenProvider = jwtTokenProvider
    )

    @Test
    @DisplayName("리프레시 토큰을 발급한다")
    fun issue() {
        val refreshToken = "refresh-token"
        val tokenHash = ByteArray(32) { 1 }
        val savedToken = slot<RefreshToken>()

        every { refreshTokenGenerator.generate() } returns refreshToken
        every { tokenHasher.hash(refreshToken) } returns tokenHash
        every { refreshTokenRepository.save(capture(savedToken)) } answers {firstArg()}

        val token = refreshTokenService.issue(1L)

        assertThat(token).isEqualTo(refreshToken)
        assertThat(savedToken.captured.memberId).isEqualTo(1L)
        assertThat(savedToken.captured.tokenHash).isEqualTo(tokenHash)
        assertThat(savedToken.captured.status).isEqualTo(RefreshTokenStatus.ACTIVE)

        verify(exactly = 1) { refreshTokenRepository.save(any<RefreshToken>()) }
    }

    @Test
    @DisplayName("유효한 Refresh Token이면 Access Token과 새 Refresh Token을 발급한다")
    fun refresh() {
        val oldRawToken = "old-refresh-token"
        val oldTokenHash = ByteArray(32) { 1 }
        val newRawToken = "new-refresh-token"
        val newTokenHash = ByteArray(32) { 2 }
        val currentToken = createRefreshToken(tokenHash = oldTokenHash)
        val member = createMember()

        every { tokenHasher.hash(oldRawToken) } returns oldTokenHash
        every { refreshTokenRepository.findByTokenHashAndStatus(oldTokenHash) } returns currentToken
        every {
            memberRepository.findByIdAndStatusInAndDeletedAtIsNull(
                1L,
                listOf(MemberStatus.ACTIVE, MemberStatus.PENDING),
            )
        } returns member
        every { refreshTokenGenerator.generate() } returns newRawToken
        every { tokenHasher.hash(newRawToken) } returns newTokenHash
        every { refreshTokenRepository.save(any<RefreshToken>()) } answers { firstArg() }
        every { jwtTokenProvider.createAccessToken(1L) } returns "access-token"
        every { jwtTokenProvider.accessTokenExpiresIn() } returns 1800L

        val response = refreshTokenService.refresh(AuthTokenRefreshRequest(oldRawToken))

        assertThat(response.accessToken).isEqualTo("access-token")
        assertThat(response.refreshToken).isEqualTo(newRawToken)
        assertThat(response.tokenType).isEqualTo("Bearer")
        assertThat(response.expiresIn).isEqualTo(1800L)
        assertThat(currentToken.status).isEqualTo(RefreshTokenStatus.ROTATED)
        assertThat(currentToken.lastUsedAt).isNotNull()
        assertThat(currentToken.revokedAt).isNotNull()
    }

    @Test
    @DisplayName("존재하지 않는 Refresh Token이면 예외가 발생한다")
    fun refreshWithInvalidToken() {
        val rawToken = "invalid-refresh-token"
        val tokenHash = ByteArray(32) { 1 }

        every { tokenHasher.hash(rawToken) } returns tokenHash
        every { refreshTokenRepository.findByTokenHashAndStatus(tokenHash) } returns null

        assertThatThrownBy {
            refreshTokenService.refresh(AuthTokenRefreshRequest(rawToken))
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(MemberErrorCode.INVALID_REFRESH_TOKEN)
    }

    @Test
    @DisplayName("만료된 Refresh Token이면 만료 처리 후 예외가 발생한다")
    fun refreshWithExpiredToken() {
        val rawToken = "expired-refresh-token"
        val tokenHash = ByteArray(32) { 1 }
        val currentToken = createRefreshToken(
            tokenHash = tokenHash,
            expiresAt = LocalDateTime.now().minusSeconds(1),
        )

        every { tokenHasher.hash(rawToken) } returns tokenHash
        every { refreshTokenRepository.findByTokenHashAndStatus(tokenHash) } returns currentToken

        assertThatThrownBy {
            refreshTokenService.refresh(AuthTokenRefreshRequest(rawToken))
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(MemberErrorCode.EXPIRED_REFRESH_TOKEN)

        assertThat(currentToken.status).isEqualTo(RefreshTokenStatus.EXPIRED)
        assertThat(currentToken.revokedAt).isNotNull()
    }

    @Test
    @DisplayName("Refresh Token을 로그아웃 처리한다")
    fun logout() {
        val rawToken = "refresh-token"
        val tokenHash = ByteArray(32) { 1 }
        val currentToken = createRefreshToken(tokenHash = tokenHash)

        every { tokenHasher.hash(rawToken) } returns tokenHash
        every { refreshTokenRepository.findByTokenHashAndStatus(tokenHash) } returns currentToken

        refreshTokenService.logout(AuthLogoutRequest(rawToken))

        assertThat(currentToken.status).isEqualTo(RefreshTokenStatus.REVOKED)
        assertThat(currentToken.revokedAt).isNotNull()
    }

    @Test
    @DisplayName("Refresh Token 만료 시간이 현재 시간과 같으면 만료 처리 후 예외가 발생한다")
    fun refreshWithExpiredTokenAtNow() {
        val now = LocalDateTime.of(2026, 5, 13, 12, 0)
        val rawToken = "expired-refresh-token"
        val tokenHash = ByteArray(32) { 1 }
        val currentToken = createRefreshToken(
            tokenHash = tokenHash,
            expiresAt = now,
        )

        mockkStatic(LocalDateTime::class)

        try {
            every { LocalDateTime.now() } returns now
            every { tokenHasher.hash(rawToken) } returns tokenHash
            every { refreshTokenRepository.findByTokenHashAndStatus(tokenHash) } returns currentToken

            assertThatThrownBy {
                refreshTokenService.refresh(AuthTokenRefreshRequest(rawToken))
            }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(MemberErrorCode.EXPIRED_REFRESH_TOKEN)

            assertThat(currentToken.status).isEqualTo(RefreshTokenStatus.EXPIRED)
            assertThat(currentToken.revokedAt).isEqualTo(now)
        } finally {
            unmockkStatic(LocalDateTime::class)
        }
    }

    @Test
    @DisplayName("Refresh Token의 회원을 찾을 수 없으면 예외가 발생한다")
    fun refreshWithUnknownMember() {
        val rawToken = "refresh-token"
        val tokenHash = ByteArray(32) { 1 }
        val currentToken = createRefreshToken(tokenHash = tokenHash)

        every { tokenHasher.hash(rawToken) } returns tokenHash
        every { refreshTokenRepository.findByTokenHashAndStatus(tokenHash) } returns currentToken
        every {
            memberRepository.findByIdAndStatusInAndDeletedAtIsNull(
                1L,
                listOf(MemberStatus.ACTIVE, MemberStatus.PENDING),
            )
        } returns null

        assertThatThrownBy {
            refreshTokenService.refresh(AuthTokenRefreshRequest(rawToken))
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(MemberErrorCode.INVALID_REFRESH_TOKEN)

        assertThat(currentToken.status).isEqualTo(RefreshTokenStatus.ACTIVE)
        assertThat(currentToken.lastUsedAt).isNull()
        assertThat(currentToken.revokedAt).isNull()
    }

    @Test
    @DisplayName("회원 ID가 없으면 예외가 발생한다")
    fun refreshWithNullMemberId() {
        val rawToken = "refresh-token"
        val tokenHash = ByteArray(32) { 1 }
        val currentToken = createRefreshToken(tokenHash = tokenHash)
        val member = createMember(id = null)

        every { tokenHasher.hash(rawToken) } returns tokenHash
        every { refreshTokenRepository.findByTokenHashAndStatus(tokenHash) } returns currentToken
        every {
            memberRepository.findByIdAndStatusInAndDeletedAtIsNull(
                1L,
                listOf(MemberStatus.ACTIVE, MemberStatus.PENDING),
            )
        } returns member

        assertThatThrownBy {
            refreshTokenService.refresh(AuthTokenRefreshRequest(rawToken))
        }
            .isInstanceOf(IllegalArgumentException::class.java)

        assertThat(currentToken.status).isEqualTo(RefreshTokenStatus.ACTIVE)
        assertThat(currentToken.lastUsedAt).isNull()
        assertThat(currentToken.revokedAt).isNull()
    }

    @Test
    @DisplayName("로그아웃할 Refresh Token이 없으면 아무 처리도 하지 않는다")
    fun logoutWithUnknownToken() {
        val rawToken = "unknown-refresh-token"
        val tokenHash = ByteArray(32) { 1 }

        every { tokenHasher.hash(rawToken) } returns tokenHash
        every { refreshTokenRepository.findByTokenHashAndStatus(tokenHash) } returns null

        refreshTokenService.logout(AuthLogoutRequest(rawToken))

        verify(exactly = 1) { tokenHasher.hash(rawToken) }
        verify(exactly = 1) { refreshTokenRepository.findByTokenHashAndStatus(tokenHash) }
    }

    private fun createRefreshToken(
        tokenHash: ByteArray = ByteArray(32) { 1 },
        expiresAt: LocalDateTime = LocalDateTime.now().plusHours(1),
    ): RefreshToken {
        return RefreshToken(
            id = 1L,
            memberId = 1L,
            tokenHash = tokenHash,
            expiresAt = expiresAt,
        )
    }

    private fun createMember(
        id: Long? = 1L,
        status: MemberStatus = MemberStatus.ACTIVE,
    ): Member {
        return Member(
            id = id,
            emailHash = ByteArray(32) { 1 },
            passwordHash = "encoded-password",
            nickname = "unit_user",
            status = status,
        )
    }
}