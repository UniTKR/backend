package com.unit.member.service

import com.unit.member.dto.SchoolAuthDto.SchoolEmailVerificationConfirmRequest
import com.unit.member.entity.SchoolEmailVerificationCode
import com.unit.member.enums.SchoolEmailVerificationStatus
import com.unit.member.exception.MemberErrorCode
import com.unit.member.repository.SchoolEmailVerificationCodeRepository
import com.unit.member.util.EmailHasher
import com.unit.member.util.TokenHasher
import com.unit.platform.error.BusinessException
import com.unit.platform.mail.EmailMessage
import com.unit.platform.mail.EmailSender
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@ActiveProfiles("test")
@SpringBootTest
@Import(SchoolEmailVerificationTransactionTest.MailTestConfig::class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("학교 이메일 인증 트랜잭션 테스트")
class SchoolEmailVerificationTransactionTest @Autowired constructor(
    private val service: SchoolEmailVerificationService,
    private val repository: SchoolEmailVerificationCodeRepository,
    private val emailHasher: EmailHasher,
    private val tokenHasher: TokenHasher,
) {

    @BeforeEach
    fun setUp() {
        repository.deleteAll()
    }

    @AfterEach
    fun tearDown() {
        repository.deleteAll()
    }

    @Test
    @DisplayName("만료된 인증 코드는 예외가 발생해도 EXPIRED 상태가 저장된다")
    fun expiredCodeStatusIsCommittedWhenConfirmFails() {
        val savedCode = repository.saveAndFlush(
            createVerificationCode(
                memberId = 1L,
                schoolId = 1L,
                email = "test@snu.ac.kr",
                code = "123456",
                expiresAt = LocalDateTime.now().minusSeconds(1),
            )
        )

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

        val foundCode = repository.findById(requireNotNull(savedCode.id)).orElseThrow()
        assertThat(foundCode.status).isEqualTo(SchoolEmailVerificationStatus.EXPIRED)
    }

    @Test
    @DisplayName("틀린 인증 코드는 예외가 발생해도 실패 시도 횟수가 저장된다")
    fun attemptCountIsCommittedWhenConfirmFailsWithMismatchedCode() {
        val savedCode = repository.saveAndFlush(
            createVerificationCode(
                memberId = 1L,
                schoolId = 1L,
                email = "test@snu.ac.kr",
                code = "123456",
                expiresAt = LocalDateTime.now().plusMinutes(5),
            )
        )

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

        val foundCode = repository.findById(requireNotNull(savedCode.id)).orElseThrow()
        assertThat(foundCode.attemptCount).isEqualTo(1)
        assertThat(foundCode.status).isEqualTo(SchoolEmailVerificationStatus.PENDING)
    }

    private fun createVerificationCode(
        memberId: Long,
        schoolId: Long,
        email: String,
        code: String,
        expiresAt: LocalDateTime,
    ): SchoolEmailVerificationCode {
        return SchoolEmailVerificationCode(
            memberId = memberId,
            schoolId = schoolId,
            emailHash = emailHasher.hash(email),
            codeHash = tokenHasher.hash(code),
            expiresAt = expiresAt,
        )
    }

    @TestConfiguration
    class MailTestConfig {

        @Bean
        fun emailSender(): EmailSender {
            return object : EmailSender {
                override fun send(message: EmailMessage) {
                }
            }
        }
    }
}
