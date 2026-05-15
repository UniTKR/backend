package com.unit.member.util

import com.unit.member.entity.SchoolEmailVerificationCode
import com.unit.member.enums.SchoolEmailVerificationStatus
import com.unit.member.repository.SchoolEmailVerificationCodeRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.Test

@DisplayName("학교 이메일 인증 실패 기록기 테스트")
class SchoolEmailVerificationFailureRecorderTest {

    private val repository = mockk<SchoolEmailVerificationCodeRepository>()
    private val recorder = SchoolEmailVerificationFailureRecorder(repository)

    @Test
    @DisplayName("인증 코드를 만료 처리한다")
    fun expire() {
        val code = createVerificationCode()

        every { repository.findById(10L) } returns Optional.of(code)

        recorder.expire(10L)

        assertThat(code.status).isEqualTo(SchoolEmailVerificationStatus.EXPIRED)
    }

    @Test
    @DisplayName("코드 불일치 시도 횟수가 최대 횟수 미만이면 만료하지 않는다")
    fun recordMismatchWithoutAttemptLimitExceeded() {
        val code = createVerificationCode(attemptCount = 3)

        every { repository.findById(10L) } returns Optional.of(code)

        val attemptLimitExceeded = recorder.recordMismatch(
            id = 10L,
            maxAttemptCount = 5,
        )

        assertThat(attemptLimitExceeded).isFalse()
        assertThat(code.attemptCount).isEqualTo(4)
        assertThat(code.status).isEqualTo(SchoolEmailVerificationStatus.PENDING)
    }

    @Test
    @DisplayName("코드 불일치 시도 횟수가 최대 횟수에 도달하면 만료 처리한다")
    fun recordMismatchWithAttemptLimitExceeded() {
        val code = createVerificationCode(attemptCount = 4)

        every { repository.findById(10L) } returns Optional.of(code)

        val attemptLimitExceeded = recorder.recordMismatch(
            id = 10L,
            maxAttemptCount = 5,
        )

        assertThat(attemptLimitExceeded).isTrue()
        assertThat(code.attemptCount).isEqualTo(5)
        assertThat(code.status).isEqualTo(SchoolEmailVerificationStatus.EXPIRED)
    }

    private fun createVerificationCode(
        attemptCount: Int = 0,
    ): SchoolEmailVerificationCode {
        return SchoolEmailVerificationCode(
            id = 10L,
            memberId = 1L,
            schoolId = 1L,
            emailHash = ByteArray(32) { 1 },
            codeHash = ByteArray(32) { 2 },
            expiresAt = LocalDateTime.now().plusMinutes(5),
            attemptCount = attemptCount,
        )
    }
}
