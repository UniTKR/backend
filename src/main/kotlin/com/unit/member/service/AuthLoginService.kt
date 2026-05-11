package com.unit.member.service

import com.unit.member.dto.AuthLoginRequest
import com.unit.member.dto.AuthLoginResponse
import com.unit.member.dto.AuthenticatedMemberResponse
import com.unit.member.enums.MemberStatus
import com.unit.member.exception.MemberErrorCode
import com.unit.member.repository.MemberRepository
import com.unit.member.util.EmailHasher
import com.unit.platform.error.BusinessException
import com.unit.platform.security.JwtTokenProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AuthLoginService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
    private val emailHasher: EmailHasher,
    private val jwtTokenProvider: JwtTokenProvider
) : AuthLoginUseCase {

    override fun login(request: AuthLoginRequest): AuthLoginResponse {

        val emailHash = emailHasher.hash(request.email)

        val member = memberRepository.findByEmailHashAndDeletedAtIsNull(emailHash)
            ?: throw BusinessException(MemberErrorCode.INVALID_LOGIN_CREDENTIALS)

        val passwordHash = member.passwordHash
        if (passwordHash == null || !passwordEncoder.matches(request.password, passwordHash)) {
            throw BusinessException(MemberErrorCode.INVALID_LOGIN_CREDENTIALS)
        }

        if (member.status != MemberStatus.ACTIVE && member.status != MemberStatus.PENDING) {
            throw BusinessException(MemberErrorCode.MEMBER_LOGIN_FORBIDDEN)
        }

        val memberId = requireNotNull(member.id)

        return AuthLoginResponse(
            accessToken = jwtTokenProvider.createAccessToken(memberId),
            expiresIn = jwtTokenProvider.accessTokenExpiresIn(),
            member = AuthenticatedMemberResponse(
                memberId = memberId,
                nickname = member.nickname,
                status = member.status,
            ),
        )
    }
}