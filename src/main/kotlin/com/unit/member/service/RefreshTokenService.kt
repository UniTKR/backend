package com.unit.member.service

import com.unit.member.config.RefreshTokenProperties
import com.unit.member.dto.AuthLogoutRequest
import com.unit.member.dto.AuthTokenRefreshRequest
import com.unit.member.dto.AuthTokenRefreshResponse
import com.unit.member.entity.RefreshToken
import com.unit.member.enums.MemberStatus
import com.unit.member.exception.MemberErrorCode
import com.unit.member.repository.MemberRepository
import com.unit.member.repository.RefreshTokenRepository
import com.unit.member.util.RefreshTokenGenerator
import com.unit.member.util.TokenHasher
import com.unit.platform.error.BusinessException
import com.unit.platform.security.JwtTokenProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val memberRepository: MemberRepository,
    private val refreshTokenGenerator: RefreshTokenGenerator,
    private val tokenHasher: TokenHasher,
    private val refreshTokenProperties: RefreshTokenProperties,
    private val jwtTokenProvider: JwtTokenProvider,
) : RefreshTokenUseCase {

    override fun issue(memberId: Long): String {
        val rawToken = refreshTokenGenerator.generate()
        val tokenHash = tokenHasher.hash(rawToken)

        refreshTokenRepository.save(
            RefreshToken(
                memberId = memberId,
                tokenHash = tokenHash,
                expiresAt = LocalDateTime.now().plusSeconds(refreshTokenProperties.expirationSeconds)
            )
        )

        return rawToken
    }

    override fun refresh(request: AuthTokenRefreshRequest): AuthTokenRefreshResponse {
        val now = LocalDateTime.now()
        val tokenHash = tokenHasher.hash(request.refreshToken)

        val currentToken = refreshTokenRepository.findByTokenHashAndStatus(tokenHash)
            ?: throw BusinessException(MemberErrorCode.INVALID_REFRESH_TOKEN)

        if (currentToken.expiresAt.isBefore(now) || currentToken.expiresAt.isEqual(now)) {
            currentToken.expire(now)
            throw BusinessException(MemberErrorCode.EXPIRED_REFRESH_TOKEN)
        }

        val member = memberRepository.findByIdAndStatusInAndDeletedAtIsNull(
            id = currentToken.memberId,
            statuses = listOf(MemberStatus.ACTIVE, MemberStatus.PENDING),
        ) ?: throw BusinessException(MemberErrorCode.INVALID_REFRESH_TOKEN)

        val memberId = requireNotNull(member.id)

        currentToken.markUsed(now)
        currentToken.rotate(now)

        val newRefreshToken = issue(memberId)

        return AuthTokenRefreshResponse(
            accessToken = jwtTokenProvider.createAccessToken(memberId),
            refreshToken = newRefreshToken,
            expiresIn = jwtTokenProvider.accessTokenExpiresIn(),
        )
    }

    override fun logout(request: AuthLogoutRequest) {
        val now = LocalDateTime.now()
        val tokenHash = tokenHasher.hash(request.refreshToken)

        val currentToken = refreshTokenRepository.findByTokenHashAndStatus(tokenHash)
            ?: return

        currentToken.revoke(now)
    }
}