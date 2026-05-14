package com.unit.member.service

import com.unit.member.dto.SchoolAuthDto.SchoolEmailVerificationConfirmRequest
import com.unit.member.dto.SchoolAuthDto.SchoolEmailVerificationRequest
import com.unit.member.entity.Member
import com.unit.member.entity.School
import com.unit.member.entity.SchoolEmailVerificationCode
import com.unit.member.entity.UserSchoolVerification
import com.unit.member.enums.*
import com.unit.member.exception.MemberErrorCode
import com.unit.member.repository.*
import com.unit.member.util.*
import com.unit.platform.error.BusinessException
import com.unit.platform.mail.EmailSender
import com.unit.platform.mail.EmailTemplateRenderer
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.LocalDateTime
import kotlin.test.Test

@DisplayName("학교 이메일 인증 서비스 테스트")
class SchoolEmailVerificationServiceTest {

    private val schoolRepository = mockk<SchoolRepository>()
    private val schoolEmailDomainRepository = mockk<SchoolEmailDomainRepository>()
    private val schoolEmailVerificationCodeRepository = mockk<SchoolEmailVerificationCodeRepository>()
    private val userSchoolVerificationRepository = mockk<UserSchoolVerificationRepository>()
    private val memberRepository = mockk<MemberRepository>()
    private val emailHasher = mockk<EmailHasher>()
    private val emailEncryptor = mockk<EmailEncryptor>()
    private val tokenHasher = mockk<TokenHasher>()
    private val codeGenerator = mockk<SchoolEmailVerificationCodeGenerator>()
    private val failureRecorder = mockk<SchoolEmailVerificationFailureRecorder>()
    private val emailSender = mockk<EmailSender>()
    private val emailTemplateRenderer = mockk<EmailTemplateRenderer>()

    private val service = SchoolEmailVerificationService(
        schoolRepository = schoolRepository,
        schoolEmailDomainRepository = schoolEmailDomainRepository,
        schoolEmailVerificationCodeRepository = schoolEmailVerificationCodeRepository,
        userSchoolVerificationRepository = userSchoolVerificationRepository,
        memberRepository = memberRepository,
        emailHasher = emailHasher,
        tokenHasher = tokenHasher,
        codeGenerator = codeGenerator,
        failureRecorder = failureRecorder,
        emailSender = emailSender,
        emailTemplateRenderer = emailTemplateRenderer,
        emailEncryptor = emailEncryptor,
    )

    @AfterEach
    fun tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }

    @Test
    @DisplayName("학교 이메일 인증 요청에 성공하면 기존 대기 코드를 취소하고 새 코드를 저장한다")
    fun requestSuccess() {
        val memberId = 1L
        val schoolId = 1L
        val emailHash = ByteArray(32) { 1 }
        val codeHash = ByteArray(32) { 2 }
        val previousCode = createVerificationCode(memberId = memberId, schoolId = schoolId)
        val savedSlot = slot<SchoolEmailVerificationCode>()

        every { schoolRepository.findByIdAndStatus(schoolId) } returns createSchool(schoolId)
        every {
            userSchoolVerificationRepository.existsByMemberIdAndSchoolIdAndStatus(
                memberId,
                schoolId,
                UserSchoolVerificationStatus.PENDING,
            )
        } returns true
        every { schoolEmailDomainRepository.existsBySchoolIdAndDomainAndStatus(schoolId, "snu.ac.kr") } returns true
        every { emailHasher.hash("test@snu.ac.kr") } returns emailHash

        every {
            schoolEmailVerificationCodeRepository.findTopBySchoolIdAndEmailHashAndStatusOrderByCreatedAtDesc(
                schoolId,
                emailHash,
                SchoolEmailVerificationStatus.PENDING,
            )
        } returns previousCode
        every { codeGenerator.generate() } returns "123456"
        every { tokenHasher.hash("123456") } returns codeHash
        every { schoolEmailVerificationCodeRepository.save(capture(savedSlot)) } answers { firstArg() }
        every {
            emailTemplateRenderer.render(
                templatePath = "mail/school-email-verification.html",
                variables = mapOf(
                    "code" to "123456",
                    "expiresInMinutes" to "5",
                ),
            )
        } returns "<html>인증코드 123456</html>"

        every { emailSender.send(any()) } just Runs

        val response = service.request(
            memberId = memberId,
            request = SchoolEmailVerificationRequest(
                schoolId = schoolId,
                email = " TEST@snu.ac.kr ",
            ),
        )

        assertThat(response.schoolId).isEqualTo(schoolId)
        assertThat(response.email).isEqualTo("test@snu.ac.kr")
        assertThat(response.expiresIn).isEqualTo(300)

        assertThat(previousCode.status).isEqualTo(SchoolEmailVerificationStatus.CANCELED)
        assertThat(savedSlot.captured.memberId).isEqualTo(memberId)
        assertThat(savedSlot.captured.schoolId).isEqualTo(schoolId)
        assertThat(savedSlot.captured.emailHash).isSameAs(emailHash)
        assertThat(savedSlot.captured.codeHash).isSameAs(codeHash)
        assertThat(savedSlot.captured.status).isEqualTo(SchoolEmailVerificationStatus.PENDING)

        verify(exactly = 1) {
            emailTemplateRenderer.render(
                templatePath = "mail/school-email-verification.html",
                variables = mapOf(
                    "code" to "123456",
                    "expiresInMinutes" to "5",
                ),
            )
        }

        verify(exactly = 1) {
            emailSender.send(
                match {
                    it.to == "test@snu.ac.kr" &&
                            it.body == "<html>인증코드 123456</html>" &&
                            it.html
                },
            )
        }

    }

    @Test
    @DisplayName("기존 대기 인증 코드가 없어도 새 인증 코드를 저장한다")
    fun requestSuccessWithoutPreviousCode() {
        val memberId = 1L
        val schoolId = 1L
        val emailHash = ByteArray(32) { 1 }
        val codeHash = ByteArray(32) { 2 }

        every { schoolRepository.findByIdAndStatus(schoolId) } returns createSchool(schoolId)
        every {
            userSchoolVerificationRepository.existsByMemberIdAndSchoolIdAndStatus(
                memberId,
                schoolId,
                UserSchoolVerificationStatus.PENDING,
            )
        } returns true
        every { schoolEmailDomainRepository.existsBySchoolIdAndDomainAndStatus(schoolId, "snu.ac.kr") } returns true
        every { emailHasher.hash("test@snu.ac.kr") } returns emailHash
        every {
            schoolEmailVerificationCodeRepository.findTopBySchoolIdAndEmailHashAndStatusOrderByCreatedAtDesc(
                schoolId,
                emailHash,
                SchoolEmailVerificationStatus.PENDING,
            )
        } returns null
        every { codeGenerator.generate() } returns "123456"
        every { tokenHasher.hash("123456") } returns codeHash
        every { schoolEmailVerificationCodeRepository.save(any()) } answers { firstArg() }
        every {
            emailTemplateRenderer.render(
                templatePath = "mail/school-email-verification.html",
                variables = mapOf(
                    "code" to "123456",
                    "expiresInMinutes" to "5",
                ),
            )
        } returns "<html>인증코드 123456</html>"

        every { emailSender.send(any()) } just Runs

        service.request(
            memberId = memberId,
            request = SchoolEmailVerificationRequest(
                schoolId = schoolId,
                email = "test@snu.ac.kr",
            ),
        )

        verify(exactly = 1) { schoolEmailVerificationCodeRepository.save(any()) }
        verify(exactly = 1) { schoolEmailVerificationCodeRepository.save(any()) }
        verify(exactly = 1) { emailSender.send(any()) }
    }

    @Test
    @DisplayName("학교 이메일 인증 메일은 트랜잭션 커밋 이후 발송한다")
    fun requestSendsEmailAfterCommit() {
        val memberId = 1L
        val schoolId = 1L
        val emailHash = ByteArray(32) { 1 }
        val codeHash = ByteArray(32) { 2 }

        every { schoolRepository.findByIdAndStatus(schoolId) } returns createSchool(schoolId)
        every {
            userSchoolVerificationRepository.existsByMemberIdAndSchoolIdAndStatus(
                memberId,
                schoolId,
                UserSchoolVerificationStatus.PENDING,
            )
        } returns true
        every { schoolEmailDomainRepository.existsBySchoolIdAndDomainAndStatus(schoolId, "snu.ac.kr") } returns true
        every { emailHasher.hash("test@snu.ac.kr") } returns emailHash
        every {
            schoolEmailVerificationCodeRepository.findTopBySchoolIdAndEmailHashAndStatusOrderByCreatedAtDesc(
                schoolId,
                emailHash,
                SchoolEmailVerificationStatus.PENDING,
            )
        } returns null
        every { codeGenerator.generate() } returns "123456"
        every { tokenHasher.hash("123456") } returns codeHash
        every { schoolEmailVerificationCodeRepository.save(any()) } answers { firstArg() }
        every {
            emailTemplateRenderer.render(
                templatePath = "mail/school-email-verification.html",
                variables = mapOf(
                    "code" to "123456",
                    "expiresInMinutes" to "5",
                ),
            )
        } returns "<html>인증코드 123456</html>"
        every { emailSender.send(any()) } just Runs

        TransactionSynchronizationManager.initSynchronization()

        service.request(
            memberId = memberId,
            request = SchoolEmailVerificationRequest(
                schoolId = schoolId,
                email = "test@snu.ac.kr",
            ),
        )

        verify(exactly = 0) { emailSender.send(any()) }

        val synchronizations = TransactionSynchronizationManager.getSynchronizations()
        assertThat(synchronizations).hasSize(1)

        synchronizations.forEach { it.afterCommit() }

        verify(exactly = 1) {
            emailSender.send(
                match {
                    it.to == "test@snu.ac.kr" &&
                            it.body == "<html>인증코드 123456</html>" &&
                            it.html
                },
            )
        }
    }


    @Test
    @DisplayName("학교가 없으면 인증 요청에 실패한다")
    fun requestWithNotFoundSchool() {
        every { schoolRepository.findByIdAndStatus(1L) } returns null

        assertThatThrownBy {
            service.request(
                memberId = 1L,
                request = SchoolEmailVerificationRequest(
                    schoolId = 1L,
                    email = "test@snu.ac.kr",
                ),
            )
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(MemberErrorCode.SCHOOL_NOT_FOUND)

        verify(exactly = 0) { schoolEmailVerificationCodeRepository.save(any()) }
        verify(exactly = 0) { emailTemplateRenderer.render(any(), any()) }
        verify(exactly = 0) { emailSender.send(any()) }
    }

    @Test
    @DisplayName("학교 이메일 도메인이 아니면 인증 요청에 실패한다")
    fun requestWithNotAllowedDomain() {
        every { schoolRepository.findByIdAndStatus(1L) } returns createSchool(1L)
        every {
            userSchoolVerificationRepository.existsByMemberIdAndSchoolIdAndStatus(
                1L,
                1L,
                UserSchoolVerificationStatus.PENDING,
            )
        } returns true
        every { schoolEmailDomainRepository.existsBySchoolIdAndDomainAndStatus(1L, "gmail.com") } returns false

        assertThatThrownBy {
            service.request(
                memberId = 1L,
                request = SchoolEmailVerificationRequest(
                    schoolId = 1L,
                    email = "test@gmail.com",
                ),
            )
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(MemberErrorCode.SCHOOL_EMAIL_DOMAIN_NOT_ALLOWED)

        verify(exactly = 0) { schoolEmailVerificationCodeRepository.save(any()) }
        verify(exactly = 0) { emailTemplateRenderer.render(any(), any()) }
        verify(exactly = 0) { emailSender.send(any()) }
    }

    @Test
    @DisplayName("회원가입 때 생성된 학교 인증 대기 row와 다른 학교로 인증 요청하면 실패한다")
    fun requestWithDifferentSchoolVerification() {
        val memberId = 1L
        val requestedSchoolId = 2L

        every { schoolRepository.findByIdAndStatus(requestedSchoolId) } returns createSchool(requestedSchoolId)
        every {
            userSchoolVerificationRepository.existsByMemberIdAndSchoolIdAndStatus(
                memberId,
                requestedSchoolId,
                UserSchoolVerificationStatus.PENDING,
            )
        } returns false

        assertThatThrownBy {
            service.request(
                memberId = memberId,
                request = SchoolEmailVerificationRequest(
                    schoolId = requestedSchoolId,
                    email = "test@snu.ac.kr",
                ),
            )
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(MemberErrorCode.SCHOOL_VERIFICATION_NOT_FOUND)

        verify(exactly = 0) { schoolEmailDomainRepository.existsBySchoolIdAndDomainAndStatus(any(), any()) }
        verify(exactly = 0) { emailHasher.hash(any()) }
        verify(exactly = 0) { schoolEmailVerificationCodeRepository.save(any()) }
        verify(exactly = 0) { emailTemplateRenderer.render(any(), any()) }
        verify(exactly = 0) { emailSender.send(any()) }
    }

    @Test
    @DisplayName("인증 요청의 schoolId가 null이면 예외가 발생한다")
    fun requestWithNullSchoolId() {
        assertThatThrownBy {
            service.request(
                memberId = 1L,
                request = SchoolEmailVerificationRequest(
                    schoolId = null,
                    email = "test@snu.ac.kr",
                ),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)

        verify(exactly = 0) { schoolRepository.findByIdAndStatus(any()) }
        verify(exactly = 0) { schoolEmailVerificationCodeRepository.save(any()) }
        verify(exactly = 0) { failureRecorder.expire(any()) }
        verify(exactly = 0) { failureRecorder.increaseAttempt(any()) }
        verify(exactly = 0) { emailTemplateRenderer.render(any(), any()) }
        verify(exactly = 0) { emailSender.send(any()) }
    }

    @Test
    @DisplayName("학교 이메일 인증 확인에 성공하면 코드, 학교 인증, 회원 상태를 갱신한다")
    fun confirmSuccess() {
        val memberId = 1L
        val schoolId = 1L
        val emailHash = ByteArray(32) { 1 }
        val emailEncrypted = ByteArray(64) { 3 }
        val codeHash = ByteArray(32) { 2 }
        val verificationCode = createVerificationCode(
            id = 10L,
            memberId = memberId,
            schoolId = schoolId,
            emailHash = emailHash,
            codeHash = codeHash
        )
        val schoolVerification = createUserSchoolVerification(memberId, schoolId)
        val member = createMember(memberId)

        every { emailHasher.hash("test@snu.ac.kr") } returns emailHash
        every {
            schoolEmailVerificationCodeRepository.findTopBySchoolIdAndEmailHashAndStatusOrderByCreatedAtDesc(
                schoolId, emailHash, SchoolEmailVerificationStatus.PENDING,
            )
        } returns verificationCode
        every { failureRecorder.increaseAttempt(10L) } just Runs
        every { tokenHasher.matches("123456", codeHash) } returns true
        every { emailEncryptor.encrypt("test@snu.ac.kr") } returns emailEncrypted
        every {
            userSchoolVerificationRepository.findByMemberIdAndSchoolId(
                memberId,
                schoolId
            )
        } returns schoolVerification
        every { memberRepository.findByIdAndStatusAndDeletedAtIsNull(memberId, MemberStatus.PENDING) } returns member

        val response = service.confirm(
            memberId,
            SchoolEmailVerificationConfirmRequest(schoolId, "test@snu.ac.kr", "123456"),
        )

        assertThat(response.status).isEqualTo(UserSchoolVerificationStatus.VERIFIED)
        assertThat(verificationCode.status).isEqualTo(SchoolEmailVerificationStatus.VERIFIED)
        assertThat(schoolVerification.status).isEqualTo(UserSchoolVerificationStatus.VERIFIED)
        assertThat(schoolVerification.verifiedEmailHash).containsExactly(*emailHash)
        assertThat(schoolVerification.verifiedEmailEncrypted).isEqualTo(emailEncrypted)
        assertThat(schoolVerification.verifiedAt).isNotNull()
        assertThat(member.status).isEqualTo(MemberStatus.ACTIVE)

        verify(exactly = 1) { failureRecorder.increaseAttempt(10L) }
        verify(exactly = 0) { failureRecorder.expire(any()) }
    }


    @Test
    @DisplayName("회원 ID가 없는 인증 코드이면 인증 확인에 실패한다")
    fun confirmWithAnonymousVerificationCode() {
        val emailHash = ByteArray(32) { 1 }
        val verificationCode = createVerificationCode(
            memberId = null,
            schoolId = 1L,
            emailHash = emailHash,
            expiresAt = LocalDateTime.now().plusMinutes(5),
        )

        every { emailHasher.hash("test@snu.ac.kr") } returns emailHash
        every {
            schoolEmailVerificationCodeRepository.findTopBySchoolIdAndEmailHashAndStatusOrderByCreatedAtDesc(
                1L,
                emailHash,
                SchoolEmailVerificationStatus.PENDING,
            )
        } returns verificationCode

        assertThatThrownBy {
            service.confirm(
                memberId = 1L,
                request = SchoolEmailVerificationConfirmRequest(
                    schoolId = 1L,
                    email = "test@snu.ac.kr",
                    code = "123456",
                ),
            )
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(MemberErrorCode.SCHOOL_EMAIL_VERIFICATION_CODE_NOT_FOUND)

        verify(exactly = 0) { tokenHasher.matches(any(), any()) }
        verify(exactly = 0) { failureRecorder.expire(any()) }
        verify(exactly = 0) { failureRecorder.increaseAttempt(any()) }
        verify(exactly = 0) { userSchoolVerificationRepository.findByMemberIdAndSchoolId(any(), any()) }
        verify(exactly = 0) { memberRepository.findByIdAndStatusAndDeletedAtIsNull(any(), any()) }
    }


    @Test
    @DisplayName("인증 코드가 없으면 인증 확인에 실패한다")
    fun confirmWithoutCode() {
        val emailHash = ByteArray(32) { 1 }

        every { emailHasher.hash("test@snu.ac.kr") } returns emailHash
        every {
            schoolEmailVerificationCodeRepository.findTopBySchoolIdAndEmailHashAndStatusOrderByCreatedAtDesc(
                1L,
                emailHash,
                SchoolEmailVerificationStatus.PENDING,
            )
        } returns null

        assertThatThrownBy {
            service.confirm(
                memberId = 1L,
                request = SchoolEmailVerificationConfirmRequest(
                    schoolId = 1L,
                    email = "test@snu.ac.kr",
                    code = "123456",
                ),
            )
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(MemberErrorCode.SCHOOL_EMAIL_VERIFICATION_CODE_NOT_FOUND)

        verify(exactly = 0) { failureRecorder.expire(any()) }
        verify(exactly = 0) { failureRecorder.increaseAttempt(any()) }
    }

    @Test
    @DisplayName("다른 회원의 인증 코드이면 인증 확인에 실패한다")
    fun confirmWithOtherMemberCode() {
        val emailHash = ByteArray(32) { 1 }
        val verificationCode = createVerificationCode(
            memberId = 2L,
            schoolId = 1L,
            emailHash = emailHash,
            expiresAt = LocalDateTime.now().plusMinutes(5),
        )

        every { emailHasher.hash("test@snu.ac.kr") } returns emailHash
        every {
            schoolEmailVerificationCodeRepository.findTopBySchoolIdAndEmailHashAndStatusOrderByCreatedAtDesc(
                1L,
                emailHash,
                SchoolEmailVerificationStatus.PENDING,
            )
        } returns verificationCode

        assertThatThrownBy {
            service.confirm(
                memberId = 1L,
                request = SchoolEmailVerificationConfirmRequest(
                    schoolId = 1L,
                    email = "test@snu.ac.kr",
                    code = "123456",
                ),
            )
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(MemberErrorCode.SCHOOL_EMAIL_VERIFICATION_CODE_NOT_FOUND)

        verify(exactly = 0) { tokenHasher.matches(any(), any()) }
        verify(exactly = 0) { failureRecorder.expire(any()) }
        verify(exactly = 0) { failureRecorder.increaseAttempt(any()) }
        verify(exactly = 0) { userSchoolVerificationRepository.findByMemberIdAndSchoolId(any(), any()) }
        verify(exactly = 0) { memberRepository.findByIdAndStatusAndDeletedAtIsNull(any(), any()) }
    }


    @Test
    @DisplayName("인증 코드가 만료되면 EXPIRED 처리 후 실패한다")
    fun confirmWithExpiredCode() {
        val emailHash = ByteArray(32) { 1 }
        val verificationCode = createVerificationCode(
            id = 10L,
            memberId = 1L,
            schoolId = 1L,
            emailHash = emailHash,
            expiresAt = LocalDateTime.now().minusSeconds(1),
        )

        every { emailHasher.hash("test@snu.ac.kr") } returns emailHash
        every {
            schoolEmailVerificationCodeRepository.findTopBySchoolIdAndEmailHashAndStatusOrderByCreatedAtDesc(
                1L,
                emailHash,
                SchoolEmailVerificationStatus.PENDING,
            )
        } returns verificationCode
        every { failureRecorder.expire(10L) } just Runs

        assertThatThrownBy {
            service.confirm(
                memberId = 1L,
                request = SchoolEmailVerificationConfirmRequest(
                    schoolId = 1L,
                    email = "test@snu.ac.kr",
                    code = "123456",
                ),
            )
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(MemberErrorCode.SCHOOL_EMAIL_VERIFICATION_CODE_EXPIRED)

        verify(exactly = 1) { failureRecorder.expire(10L) }
        verify(exactly = 0) { failureRecorder.increaseAttempt(any()) }
        verify(exactly = 0) { tokenHasher.matches(any(), any()) }
    }

    @Test
    @DisplayName("만료된 인증 코드의 ID가 없으면 예외가 발생한다")
    fun confirmWithExpiredCodeWithoutId() {
        val emailHash = ByteArray(32) { 1 }
        val verificationCode = createVerificationCode(
            id = null,
            memberId = 1L,
            schoolId = 1L,
            emailHash = emailHash,
            expiresAt = LocalDateTime.now().minusSeconds(1),
        )

        every { emailHasher.hash("test@snu.ac.kr") } returns emailHash
        every {
            schoolEmailVerificationCodeRepository.findTopBySchoolIdAndEmailHashAndStatusOrderByCreatedAtDesc(
                1L,
                emailHash,
                SchoolEmailVerificationStatus.PENDING,
            )
        } returns verificationCode

        assertThatThrownBy {
            service.confirm(
                memberId = 1L,
                request = SchoolEmailVerificationConfirmRequest(
                    schoolId = 1L,
                    email = "test@snu.ac.kr",
                    code = "123456",
                ),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)

        verify(exactly = 0) { failureRecorder.expire(any()) }
        verify(exactly = 0) { failureRecorder.increaseAttempt(any()) }
        verify(exactly = 0) { tokenHasher.matches(any(), any()) }
    }

    @Test
    @DisplayName("인증 코드가 일치하지 않으면 시도 횟수 증가 후 실패한다")
    fun confirmWithMismatchedCode() {
        val emailHash = ByteArray(32) { 1 }
        val codeHash = ByteArray(32) { 2 }
        val verificationCode = createVerificationCode(
            id = 10L,
            memberId = 1L,
            schoolId = 1L,
            emailHash = emailHash,
            codeHash = codeHash,
            expiresAt = LocalDateTime.now().plusMinutes(5),
        )

        every { emailHasher.hash("test@snu.ac.kr") } returns emailHash
        every {
            schoolEmailVerificationCodeRepository.findTopBySchoolIdAndEmailHashAndStatusOrderByCreatedAtDesc(
                1L,
                emailHash,
                SchoolEmailVerificationStatus.PENDING,
            )
        } returns verificationCode
        every { failureRecorder.increaseAttempt(10L) } just Runs
        every { tokenHasher.matches("000000", codeHash) } returns false

        assertThatThrownBy {
            service.confirm(
                memberId = 1L,
                request = SchoolEmailVerificationConfirmRequest(
                    schoolId = 1L,
                    email = "test@snu.ac.kr",
                    code = "000000",
                ),
            )
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(MemberErrorCode.SCHOOL_EMAIL_VERIFICATION_CODE_MISMATCHED)

        verify(exactly = 1) { failureRecorder.increaseAttempt(10L) }
        verify(exactly = 0) { failureRecorder.expire(any()) }
    }

    @Test
    @DisplayName("인증 코드의 ID가 없으면 실패 시도 횟수를 기록하지 않고 예외가 발생한다")
    fun confirmWithCodeWithoutId() {
        val emailHash = ByteArray(32) { 1 }
        val verificationCode = createVerificationCode(
            id = null,
            memberId = 1L,
            schoolId = 1L,
            emailHash = emailHash,
            expiresAt = LocalDateTime.now().plusMinutes(5),
        )

        every { emailHasher.hash("test@snu.ac.kr") } returns emailHash
        every {
            schoolEmailVerificationCodeRepository.findTopBySchoolIdAndEmailHashAndStatusOrderByCreatedAtDesc(
                1L,
                emailHash,
                SchoolEmailVerificationStatus.PENDING,
            )
        } returns verificationCode

        assertThatThrownBy {
            service.confirm(
                memberId = 1L,
                request = SchoolEmailVerificationConfirmRequest(
                    schoolId = 1L,
                    email = "test@snu.ac.kr",
                    code = "123456",
                ),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)

        verify(exactly = 0) { failureRecorder.expire(any()) }
        verify(exactly = 0) { failureRecorder.increaseAttempt(any()) }
        verify(exactly = 0) { tokenHasher.matches(any(), any()) }
    }

    @Test
    @DisplayName("학교 인증 정보가 없으면 인증 확인에 실패한다")
    fun confirmWithoutSchoolVerification() {
        val memberId = 1L
        val schoolId = 1L
        val emailHash = ByteArray(32) { 1 }
        val codeHash = ByteArray(32) { 2 }
        val verificationCode = createVerificationCode(
            id = 10L,
            memberId = memberId,
            schoolId = schoolId,
            emailHash = emailHash,
            codeHash = codeHash,
            expiresAt = LocalDateTime.now().plusMinutes(5),
        )

        every { emailHasher.hash("test@snu.ac.kr") } returns emailHash
        every {
            schoolEmailVerificationCodeRepository.findTopBySchoolIdAndEmailHashAndStatusOrderByCreatedAtDesc(
                schoolId,
                emailHash,
                SchoolEmailVerificationStatus.PENDING,
            )
        } returns verificationCode
        every { failureRecorder.increaseAttempt(10L) } just Runs
        every { tokenHasher.matches("123456", codeHash) } returns true
        every { userSchoolVerificationRepository.findByMemberIdAndSchoolId(memberId, schoolId) } returns null

        assertThatThrownBy {
            service.confirm(
                memberId = memberId,
                request = SchoolEmailVerificationConfirmRequest(
                    schoolId = schoolId,
                    email = "test@snu.ac.kr",
                    code = "123456",
                ),
            )
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(MemberErrorCode.SCHOOL_VERIFICATION_NOT_FOUND)

        verify(exactly = 1) { failureRecorder.increaseAttempt(10L) }
        verify(exactly = 0) { failureRecorder.expire(any()) }
    }

    @Test
    @DisplayName("PENDING 회원이 아니면 인증 확인에 실패한다")
    fun confirmWithNotPendingMember() {
        val memberId = 1L
        val schoolId = 1L
        val emailHash = ByteArray(32) { 1 }
        val codeHash = ByteArray(32) { 2 }
        val verificationCode = createVerificationCode(
            id = 10L,
            memberId = memberId,
            schoolId = schoolId,
            emailHash = emailHash,
            codeHash = codeHash,
            expiresAt = LocalDateTime.now().plusMinutes(5),
        )
        val schoolVerification = createUserSchoolVerification(memberId, schoolId)

        every { emailHasher.hash("test@snu.ac.kr") } returns emailHash
        every {
            schoolEmailVerificationCodeRepository.findTopBySchoolIdAndEmailHashAndStatusOrderByCreatedAtDesc(
                schoolId,
                emailHash,
                SchoolEmailVerificationStatus.PENDING,
            )
        } returns verificationCode
        every { failureRecorder.increaseAttempt(10L) } just Runs
        every { tokenHasher.matches("123456", codeHash) } returns true
        every {
            userSchoolVerificationRepository.findByMemberIdAndSchoolId(
                memberId,
                schoolId
            )
        } returns schoolVerification
        every { memberRepository.findByIdAndStatusAndDeletedAtIsNull(memberId, MemberStatus.PENDING) } returns null

        assertThatThrownBy {
            service.confirm(
                memberId = memberId,
                request = SchoolEmailVerificationConfirmRequest(
                    schoolId = schoolId,
                    email = "test@snu.ac.kr",
                    code = "123456",
                ),
            )
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(MemberErrorCode.MEMBER_LOGIN_FORBIDDEN)

        verify(exactly = 1) { failureRecorder.increaseAttempt(10L) }
        verify(exactly = 0) { failureRecorder.expire(any()) }
    }

    @Test
    @DisplayName("인증 확인의 schoolId가 null이면 예외가 발생한다")
    fun confirmWithNullSchoolId() {
        assertThatThrownBy {
            service.confirm(
                memberId = 1L,
                request = SchoolEmailVerificationConfirmRequest(
                    schoolId = null,
                    email = "test@snu.ac.kr",
                    code = "123456",
                ),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)

        verify(exactly = 0) {
            schoolEmailVerificationCodeRepository.findTopBySchoolIdAndEmailHashAndStatusOrderByCreatedAtDesc(
                any(),
                any(),
                any(),
            )
        }
        verify(exactly = 0) { failureRecorder.expire(any()) }
        verify(exactly = 0) { failureRecorder.increaseAttempt(any()) }
    }

    private fun createSchool(id: Long): School {
        return School(
            id = id,
            name = "서울대학교",
            region = "서울",
            status = SchoolStatus.ACTIVE,
        )
    }

    private fun createVerificationCode(
        id: Long? = 1L,
        memberId: Long? = 1L,
        schoolId: Long = 1L,
        emailHash: ByteArray = ByteArray(32) { 1 },
        codeHash: ByteArray = ByteArray(32) { 2 },
        expiresAt: LocalDateTime = LocalDateTime.now().plusMinutes(5),
    ): SchoolEmailVerificationCode {
        return SchoolEmailVerificationCode(
            id = id,
            memberId = memberId,
            schoolId = schoolId,
            emailHash = emailHash,
            codeHash = codeHash,
            expiresAt = expiresAt,
        )
    }

    private fun createUserSchoolVerification(
        memberId: Long,
        schoolId: Long,
    ): UserSchoolVerification {
        return UserSchoolVerification(
            memberId = memberId,
            schoolId = schoolId,
            method = UserSchoolVerificationMethod.EMAIL,
            status = UserSchoolVerificationStatus.PENDING,
        )
    }

    private fun createMember(id: Long): Member {
        return Member(
            id = id,
            emailHash = ByteArray(32) { 1 },
            passwordHash = "encoded-password",
            nickname = "unit_user",
            status = MemberStatus.PENDING,
        )
    }
}
