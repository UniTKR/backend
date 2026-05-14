package com.unit.member.service

import com.unit.member.config.MemberConsentProperties
import com.unit.member.dto.MemberSignupRequest
import com.unit.member.entity.Member
import com.unit.member.entity.MemberConsent
import com.unit.member.entity.UserSchoolVerification
import com.unit.member.enums.MemberConsentType
import com.unit.member.enums.MemberStatus
import com.unit.member.enums.UserSchoolVerificationStatus
import com.unit.member.exception.MemberErrorCode
import com.unit.member.repository.MemberConsentRepository
import com.unit.member.repository.MemberRepository
import com.unit.member.repository.SchoolRepository
import com.unit.member.repository.UserSchoolVerificationRepository
import com.unit.member.util.EmailHasher
import com.unit.platform.error.BusinessException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.springframework.security.crypto.password.PasswordEncoder
import kotlin.test.Test

@DisplayName("MemberSignupService 테스트")
class MemberSignupServiceTest {

    private val memberRepository = mockk<MemberRepository>()
    private val schoolRepository = mockk<SchoolRepository>()
    private val userSchoolVerificationRepository = mockk<UserSchoolVerificationRepository>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val emailHasher = mockk<EmailHasher>()
    private val memberConsentRepository = mockk<MemberConsentRepository>()
    private val memberConsentProperties = MemberConsentProperties(
        termsVersion = "2026-05-14-test",
        privacyPolicyVersion = "2026-05-14-test",
    )

    private val memberSignupService = MemberSignupService(
        memberRepository = memberRepository,
        schoolRepository = schoolRepository,
        userSchoolVerificationRepository = userSchoolVerificationRepository,
        passwordEncoder = passwordEncoder,
        emailHasher = emailHasher,
        memberConsentRepository = memberConsentRepository,
        memberConsentProperties = memberConsentProperties,
    )

    @Test
    @DisplayName("회원가입에 성공하면 PENDING 회원과 학교 인증 요청을 생성한다")
    fun signup() {
        val request = createRequest(nickname = " unit_user ")
        val emailHash = ByteArray(32) { 1 }
        val encodedPassword = "encoded-password"

        val memberSlot = slot<Member>()
        val verificationSlot = slot<UserSchoolVerification>()
        val consentSlot = slot<Iterable<MemberConsent>>()

        every { emailHasher.hash(request.email) } returns emailHash
        every { memberRepository.existsByEmailHashAndDeletedAtIsNull(emailHash) } returns false
        every { memberRepository.existsByNicknameAndDeletedAtIsNull("unit_user") } returns false
        every { schoolRepository.existsByIdAndStatus(1L) } returns true
        every { passwordEncoder.encode(request.password) } returns encodedPassword
        every { memberRepository.save(capture(memberSlot)) } returns Member(
            id = 1L,
            emailHash = emailHash,
            passwordHash = encodedPassword,
            nickname = "unit_user",
            status = MemberStatus.PENDING,
        )
        every { userSchoolVerificationRepository.save(capture(verificationSlot)) } answers {
            firstArg()
        }
        every { memberConsentRepository.saveAll(capture(consentSlot)) } answers {
            consentSlot.captured.toList()
        }

        val response = memberSignupService.signup(request)

        assertThat(response.memberId).isEqualTo(1L)
        assertThat(response.nickname).isEqualTo("unit_user")
        assertThat(response.status).isEqualTo(MemberStatus.PENDING)
        assertThat(response.schoolVerificationStatus).isEqualTo(UserSchoolVerificationStatus.PENDING)

        assertThat(memberSlot.captured.emailHash).isEqualTo(emailHash)
        assertThat(memberSlot.captured.passwordHash).isEqualTo(encodedPassword)
        assertThat(memberSlot.captured.nickname).isEqualTo("unit_user")
        assertThat(memberSlot.captured.status).isEqualTo(MemberStatus.PENDING)

        assertThat(verificationSlot.captured.memberId).isEqualTo(1L)
        assertThat(verificationSlot.captured.schoolId).isEqualTo(1L)
        assertThat(verificationSlot.captured.status).isEqualTo(UserSchoolVerificationStatus.PENDING)

        val consents = consentSlot.captured.toList()

        assertThat(consents).hasSize(2)
        assertThat(consents.map { it.consentType })
            .containsExactlyInAnyOrder(
                MemberConsentType.TERMS_OF_SERVICE,
                MemberConsentType.PRIVACY_POLICY,
            )

        assertThat(consents)
            .anySatisfy {
                assertThat(it.memberId).isEqualTo(1L)
                assertThat(it.consentType).isEqualTo(MemberConsentType.TERMS_OF_SERVICE)
                assertThat(it.policyVersion).isEqualTo("2026-05-14-test")
                assertThat(it.agreed).isTrue()
                assertThat(it.agreedAt).isNotNull()
            }

        assertThat(consents)
            .anySatisfy {
                assertThat(it.memberId).isEqualTo(1L)
                assertThat(it.consentType).isEqualTo(MemberConsentType.PRIVACY_POLICY)
                assertThat(it.policyVersion).isEqualTo("2026-05-14-test")
                assertThat(it.agreed).isTrue()
                assertThat(it.agreedAt).isNotNull()
            }

        verify(exactly = 1) { userSchoolVerificationRepository.save(any()) }
        verify(exactly = 1) { memberConsentRepository.saveAll(any<Iterable<MemberConsent>>()) }
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 예외가 발생한다")
    fun signupWithDuplicatedEmail() {
        val request = createRequest()
        val emailHash = ByteArray(32) { 1 }

        every { emailHasher.hash(request.email) } returns emailHash
        every { memberRepository.existsByEmailHashAndDeletedAtIsNull(emailHash) } returns true

        assertThatThrownBy {
            memberSignupService.signup(request)
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(MemberErrorCode.EMAIL_ALREADY_EXISTS)

        verify(exactly = 0) { memberRepository.save(any()) }
        verify(exactly = 0) { userSchoolVerificationRepository.save(any()) }
        verify(exactly = 0) { memberConsentRepository.saveAll(any<Iterable<MemberConsent>>()) }

    }

    @Test
    @DisplayName("이미 사용 중인 닉네임이면 예외가 발생한다")
    fun signupWithDuplicatedNickname() {
        val request = createRequest(nickname = " unit_user ")
        val emailHash = ByteArray(32) { 1 }

        every { emailHasher.hash(request.email) } returns emailHash
        every { memberRepository.existsByEmailHashAndDeletedAtIsNull(emailHash) } returns false
        every { memberRepository.existsByNicknameAndDeletedAtIsNull("unit_user") } returns true

        assertThatThrownBy {
            memberSignupService.signup(request)
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(MemberErrorCode.NICKNAME_ALREADY_EXISTS)

        verify(exactly = 0) { memberRepository.save(any()) }
        verify(exactly = 0) { userSchoolVerificationRepository.save(any()) }
        verify(exactly = 0) { memberConsentRepository.saveAll(any<Iterable<MemberConsent>>()) }

    }

    @Test
    @DisplayName("존재하지 않는 학교이면 예외가 발생한다")
    fun signupWithNotFoundSchool() {
        val request = createRequest()
        val emailHash = ByteArray(32) { 1 }

        every { emailHasher.hash(request.email) } returns emailHash
        every { memberRepository.existsByEmailHashAndDeletedAtIsNull(emailHash) } returns false
        every { memberRepository.existsByNicknameAndDeletedAtIsNull(request.nickname) } returns false
        every { schoolRepository.existsByIdAndStatus(1L) } returns false

        assertThatThrownBy {
            memberSignupService.signup(request)
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(MemberErrorCode.SCHOOL_NOT_FOUND)

        verify(exactly = 0) { memberRepository.save(any()) }
        verify(exactly = 0) { userSchoolVerificationRepository.save(any()) }
        verify(exactly = 0) { memberConsentRepository.saveAll(any<Iterable<MemberConsent>>()) }


    }

    private fun createRequest(
        email: String = "test@unit.com",
        password: String = "password123!",
        nickname: String = "unit_user",
        schoolId: Long? = 1L,
        termsAgreed: Boolean = true,
        privacyPolicyAgreed: Boolean = true,
    ): MemberSignupRequest {
        return MemberSignupRequest(
            email = email,
            password = password,
            nickname = nickname,
            schoolId = schoolId,
            termsAgreed = termsAgreed,
            privacyPolicyAgreed = privacyPolicyAgreed,
        )
    }

}