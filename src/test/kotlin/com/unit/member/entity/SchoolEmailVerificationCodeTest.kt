package com.unit.member.entity

import com.unit.member.enums.SchoolEmailVerificationStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.BeforeTest

@DisplayName("SchoolEmailVerificationCode 엔티티 테스트")
class SchoolEmailVerificationCodeTest {

    lateinit var schoolEmailVerificationCode: SchoolEmailVerificationCode

    @BeforeTest
    fun setUp() {
        schoolEmailVerificationCode = SchoolEmailVerificationCode(
            schoolId = 1L,
            emailHash = ByteArray(32) { 1 },
            codeHash = ByteArray(32) { 1 },
            expiresAt = LocalDateTime.of(2026, 5, 8, 12, 0),
        )
    }

    @Test
    @DisplayName("increaseAttempt() 호출 시 attemptCount가 증가한다")
    fun increaseAttempt() {
        schoolEmailVerificationCode.increaseAttempt()

        assertThat(schoolEmailVerificationCode.attemptCount).isEqualTo(1)
    }

    @Test
    @DisplayName("verify() 호출")
    fun verify() {
        val now = LocalDateTime.of(2026, 5, 8, 12, 0)

        schoolEmailVerificationCode.verify(now)

        assertThat(schoolEmailVerificationCode.status).isEqualTo(SchoolEmailVerificationStatus.VERIFIED)
        assertThat(schoolEmailVerificationCode.verifiedAt).isEqualTo(now)
    }

    @Test
    @DisplayName("expire() 호출")
    fun expire() {
        schoolEmailVerificationCode.expire()

        assertThat(schoolEmailVerificationCode.status).isEqualTo(SchoolEmailVerificationStatus.EXPIRED)
    }

    @Test
    @DisplayName("cancel() 호출")
    fun cancel() {
        schoolEmailVerificationCode.cancel()

        assertThat(schoolEmailVerificationCode.status).isEqualTo(SchoolEmailVerificationStatus.CANCELED)
    }
}