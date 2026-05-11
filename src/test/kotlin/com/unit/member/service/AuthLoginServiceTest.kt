package com.unit.member.service

import com.unit.member.dto.AuthLoginRequest
import com.unit.member.entity.Member
import com.unit.member.enums.MemberStatus
import com.unit.member.exception.MemberErrorCode
import com.unit.member.repository.MemberRepository
import com.unit.member.util.EmailHasher
import com.unit.platform.error.BusinessException
import com.unit.platform.security.JwtTokenProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.security.crypto.password.PasswordEncoder
import kotlin.test.Test

@DisplayName("AuthLoginService 테스트")
class AuthLoginServiceTest {

    private val memberRepository = mockk<MemberRepository>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val emailHasher = mockk<EmailHasher>()
    private val jwtTokenProvider = mockk<JwtTokenProvider>()

    private val authLoginService = AuthLoginService(
        memberRepository = memberRepository,
        passwordEncoder = passwordEncoder,
        emailHasher = emailHasher,
        jwtTokenProvider = jwtTokenProvider,
    )

    @Test
    @DisplayName("로그인 성공")
    fun loginSuccess() {

        val member = createMember()
        val email = "test@unit.com"
        val emailHash = ByteArray(32) { 1 }
        val password = "plain-password"
        val passwordHash = member.passwordHash
        val accessToken = "access-token"
        val expiresIn = 1800L

        every { emailHasher.hash(email) } returns emailHash
        every { memberRepository.findByEmailHashAndDeletedAtIsNull(emailHash) } returns member
        every { passwordEncoder.matches(password, passwordHash) } returns true
        every { jwtTokenProvider.createAccessToken(1L) } returns accessToken
        every { jwtTokenProvider.accessTokenExpiresIn() } returns expiresIn

        val request = createLoginRequest(email, password)

        val response = authLoginService.login(request)

        assertThat(response.accessToken).isEqualTo(accessToken)
        assertThat(response.tokenType).isEqualTo("Bearer")
        assertThat(response.expiresIn).isEqualTo(expiresIn)
        assertThat(response.member.memberId).isEqualTo(1L)
        assertThat(response.member.nickname).isEqualTo("unit_user")
        assertThat(response.member.status).isEqualTo(MemberStatus.ACTIVE)

        verify(exactly = 1) { emailHasher.hash(email) }
        verify(exactly = 1) { memberRepository.findByEmailHashAndDeletedAtIsNull(emailHash) }
        verify(exactly = 1) { passwordEncoder.matches(password, passwordHash) }
        verify(exactly = 1) { jwtTokenProvider.createAccessToken(1L) }
        verify(exactly = 1) { jwtTokenProvider.accessTokenExpiresIn() }
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인 시도")
    fun loginWithUnknownEmail() {

        val email = "test2@unit.com"
        val emailHash = ByteArray(32) { 1 }
        val password = "plain-password"

        every { emailHasher.hash(email) } returns emailHash
        every { memberRepository.findByEmailHashAndDeletedAtIsNull(emailHash) } returns null

        val request = createLoginRequest(email, password)

        assertThatThrownBy {
            authLoginService.login(request)
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(MemberErrorCode.INVALID_LOGIN_CREDENTIALS)

        verify(exactly = 1) { emailHasher.hash(email) }
        verify(exactly = 1) { memberRepository.findByEmailHashAndDeletedAtIsNull(emailHash) }
        verify(exactly = 0) { passwordEncoder.matches(any(), any()) }
        verify(exactly = 0) { jwtTokenProvider.createAccessToken(any()) }
        verify(exactly = 0) { jwtTokenProvider.accessTokenExpiresIn() }
    }

    @Test
    @DisplayName("비밀번호 불일치")
    fun wrongPassword() {

        val member = createMember()
        val email = "test@unit.com"
        val emailHash = ByteArray(32) { 1 }
        val password = "wrong-password"
        val passwordHash = member.passwordHash

        every { emailHasher.hash(email) } returns emailHash
        every { memberRepository.findByEmailHashAndDeletedAtIsNull(emailHash) } returns member
        every { passwordEncoder.matches(password, passwordHash) } returns false

        val request = createLoginRequest(email, password)

        assertThatThrownBy {
            authLoginService.login(request)
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(MemberErrorCode.INVALID_LOGIN_CREDENTIALS)

        verify(exactly = 1) { emailHasher.hash(email) }
        verify(exactly = 1) { memberRepository.findByEmailHashAndDeletedAtIsNull(emailHash) }
        verify(exactly = 1) { passwordEncoder.matches(password, passwordHash) }
        verify(exactly = 0) { jwtTokenProvider.createAccessToken(any()) }
        verify(exactly = 0) { jwtTokenProvider.accessTokenExpiresIn() }
    }

    @Test
    @DisplayName("비밀번호 해시가 없으면 인증 실패 예외가 발생한다")
    fun loginWithNullPasswordHash() {

        val member = createMember(passwordHash = null)
        val email = "test@unit.com"
        val emailHash = ByteArray(32) { 1 }
        val password = "plain-password"

        every { emailHasher.hash(email) } returns emailHash
        every { memberRepository.findByEmailHashAndDeletedAtIsNull(emailHash) } returns member

        val request = createLoginRequest(email, password)

        assertThatThrownBy {
            authLoginService.login(request)
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(MemberErrorCode.INVALID_LOGIN_CREDENTIALS)

        verify(exactly = 1) { emailHasher.hash(email) }
        verify(exactly = 1) { memberRepository.findByEmailHashAndDeletedAtIsNull(emailHash) }
        verify(exactly = 0) { passwordEncoder.matches(any(), any()) }
        verify(exactly = 0) { jwtTokenProvider.createAccessToken(any()) }
        verify(exactly = 0) { jwtTokenProvider.accessTokenExpiresIn() }
    }

    @Test
    @DisplayName("PENDING 회원은 로그인할 수 있다")
    fun loginWithPendingMember() {
        val member = createMember(status = MemberStatus.PENDING)
        val email = "test@unit.com"
        val emailHash = ByteArray(32) { 1 }
        val password = "plain-password"
        val passwordHash = member.passwordHash
        val accessToken = "access-token"
        val expiresIn = 1800L

        every { emailHasher.hash(email) } returns emailHash
        every { memberRepository.findByEmailHashAndDeletedAtIsNull(emailHash) } returns member
        every { passwordEncoder.matches(password, passwordHash) } returns true
        every { jwtTokenProvider.createAccessToken(1L) } returns accessToken
        every { jwtTokenProvider.accessTokenExpiresIn() } returns expiresIn

        val request = createLoginRequest(email, password)

        val response = authLoginService.login(request)

        assertThat(response.accessToken).isEqualTo(accessToken)
        assertThat(response.tokenType).isEqualTo("Bearer")
        assertThat(response.expiresIn).isEqualTo(expiresIn)
        assertThat(response.member.memberId).isEqualTo(1L)
        assertThat(response.member.nickname).isEqualTo("unit_user")
        assertThat(response.member.status).isEqualTo(MemberStatus.PENDING)

        verify(exactly = 1) { emailHasher.hash(email) }
        verify(exactly = 1) { memberRepository.findByEmailHashAndDeletedAtIsNull(emailHash) }
        verify(exactly = 1) { passwordEncoder.matches(password, passwordHash) }
        verify(exactly = 1) { jwtTokenProvider.createAccessToken(1L) }
        verify(exactly = 1) { jwtTokenProvider.accessTokenExpiresIn() }
    }

    @Test
    @DisplayName("SUSPENDED 회원은 로그인할 수 없다")
    fun loginWithSuspendedMember() {
        val member = createMember(status = MemberStatus.SUSPENDED)
        val email = "test@unit.com"
        val emailHash = ByteArray(32) { 1 }
        val password = "plain-password"
        val passwordHash = member.passwordHash


        every { emailHasher.hash(email) } returns emailHash
        every { memberRepository.findByEmailHashAndDeletedAtIsNull(emailHash) } returns member
        every { passwordEncoder.matches(password, passwordHash) } returns true

        val request = createLoginRequest(email, password)

        assertThatThrownBy {
            authLoginService.login(request)
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(MemberErrorCode.MEMBER_LOGIN_FORBIDDEN)

        verify(exactly = 1) { emailHasher.hash(email) }
        verify(exactly = 1) { memberRepository.findByEmailHashAndDeletedAtIsNull(emailHash) }
        verify(exactly = 1) { passwordEncoder.matches(any(), any()) }
        verify(exactly = 0) { jwtTokenProvider.createAccessToken(any()) }
        verify(exactly = 0) { jwtTokenProvider.accessTokenExpiresIn() }
    }

    @Test
    @DisplayName("DELETED 회원은 로그인할 수 없다")
    fun loginWithDeletedMember() {
        val member = createMember(status = MemberStatus.DELETED)
        val email = "test@unit.com"
        val emailHash = ByteArray(32) { 1 }
        val password = "plain-password"
        val passwordHash = member.passwordHash

        every { emailHasher.hash(email) } returns emailHash
        every { memberRepository.findByEmailHashAndDeletedAtIsNull(emailHash) } returns member
        every { passwordEncoder.matches(password, passwordHash) } returns true

        val request = createLoginRequest(email, password)

        assertThatThrownBy {
            authLoginService.login(request)
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(MemberErrorCode.MEMBER_LOGIN_FORBIDDEN)

        verify(exactly = 1) { emailHasher.hash(email) }
        verify(exactly = 1) { memberRepository.findByEmailHashAndDeletedAtIsNull(emailHash) }
        verify(exactly = 1) { passwordEncoder.matches(password, passwordHash) }
        verify(exactly = 0) { jwtTokenProvider.createAccessToken(any()) }
        verify(exactly = 0) { jwtTokenProvider.accessTokenExpiresIn() }
    }

    @ParameterizedTest
    @EnumSource(value = MemberStatus::class, names = ["ACTIVE", "PENDING"])
    @DisplayName("로그인 가능한 회원 상태이면 로그인에 성공한다")
    fun loginAllowedStatus(status: MemberStatus) {
        val member = createMember(status = status)
        val email = "test@unit.com"
        val emailHash = ByteArray(32) { 1 }
        val password = "plain-password"
        val passwordHash = member.passwordHash
        val accessToken = "access-token"
        val expiresIn = 1800L

        every { emailHasher.hash(email) } returns emailHash
        every { memberRepository.findByEmailHashAndDeletedAtIsNull(emailHash) } returns member
        every { passwordEncoder.matches(password, passwordHash) } returns true
        every { jwtTokenProvider.createAccessToken(1L) } returns accessToken
        every { jwtTokenProvider.accessTokenExpiresIn() } returns expiresIn

        val request = createLoginRequest(email, password)

        val response = authLoginService.login(request)

        assertThat(response.accessToken).isEqualTo(accessToken)
        assertThat(response.tokenType).isEqualTo("Bearer")
        assertThat(response.expiresIn).isEqualTo(expiresIn)
        assertThat(response.member.memberId).isEqualTo(1L)
        assertThat(response.member.nickname).isEqualTo("unit_user")
        assertThat(response.member.status).isEqualTo(status)

        verify(exactly = 1) { emailHasher.hash(email) }
        verify(exactly = 1) { memberRepository.findByEmailHashAndDeletedAtIsNull(emailHash) }
        verify(exactly = 1) { passwordEncoder.matches(password, passwordHash) }
        verify(exactly = 1) { jwtTokenProvider.createAccessToken(1L) }
        verify(exactly = 1) { jwtTokenProvider.accessTokenExpiresIn() }
    }

    @ParameterizedTest
    @EnumSource(value = MemberStatus::class, names = ["SUSPENDED", "DELETED"])
    @DisplayName("로그인 불가 상태이면 예외가 발생한다")
    fun loginForbiddenStatus(status: MemberStatus) {
        val member = createMember(status = status)
        val email = "test@unit.com"
        val emailHash = ByteArray(32) { 1 }
        val password = "plain-password"
        val passwordHash = member.passwordHash

        every { emailHasher.hash(email) } returns emailHash
        every { memberRepository.findByEmailHashAndDeletedAtIsNull(emailHash) } returns member
        every { passwordEncoder.matches(password, passwordHash) } returns true

        val request = createLoginRequest(email, password)

        assertThatThrownBy {
            authLoginService.login(request)
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(MemberErrorCode.MEMBER_LOGIN_FORBIDDEN)

        verify(exactly = 1) { emailHasher.hash(email) }
        verify(exactly = 1) { memberRepository.findByEmailHashAndDeletedAtIsNull(emailHash) }
        verify(exactly = 1) { passwordEncoder.matches(password, passwordHash) }
        verify(exactly = 0) { jwtTokenProvider.createAccessToken(any()) }
        verify(exactly = 0) { jwtTokenProvider.accessTokenExpiresIn() }
    }

    @Test
    @DisplayName("회원 ID가 없으면 예외가 발생한다")
    fun loginWithNullMemberId() {
        val member = createMember(id = null)
        val email = "test@unit.com"
        val emailHash = ByteArray(32) { 1 }
        val password = "plain-password"
        val passwordHash = member.passwordHash

        every { emailHasher.hash(email) } returns emailHash
        every { memberRepository.findByEmailHashAndDeletedAtIsNull(emailHash) } returns member
        every { passwordEncoder.matches(password, passwordHash) } returns true

        val request = createLoginRequest(email, password)

        assertThatThrownBy {
            authLoginService.login(request)
        }
            .isInstanceOf(IllegalArgumentException::class.java)

        verify(exactly = 1) { emailHasher.hash(email) }
        verify(exactly = 1) { memberRepository.findByEmailHashAndDeletedAtIsNull(emailHash) }
        verify(exactly = 1) { passwordEncoder.matches(password, passwordHash) }
        verify(exactly = 0) { jwtTokenProvider.createAccessToken(any()) }
        verify(exactly = 0) { jwtTokenProvider.accessTokenExpiresIn() }
    }


    private fun createLoginRequest(
        email: String,
        password: String
    ): AuthLoginRequest {
        return AuthLoginRequest(
            email = email,
            password = password
        )
    }


    private fun createMember(
        id: Long? = 1L,
        emailHash: ByteArray = ByteArray(32) { 1 },
        passwordHash: String? = "encoded-password",
        nickname: String = "unit_user",
        status: MemberStatus = MemberStatus.ACTIVE
    ): Member {
        return Member(
            id = id,
            emailHash = emailHash,
            passwordHash = passwordHash,
            nickname = nickname,
            status = status
        )
    }

}