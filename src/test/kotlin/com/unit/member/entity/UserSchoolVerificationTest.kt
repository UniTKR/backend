package com.unit.member.entity

import com.unit.member.enums.UserSchoolVerificationMethod
import com.unit.member.enums.UserSchoolVerificationStatus
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.BeforeTest

@DisplayName("UserSchoolVerification 엔티티 테스트")
class UserSchoolVerificationTest {

    lateinit var userSchoolVerification: UserSchoolVerification

    @BeforeTest
    fun setUp() {
        userSchoolVerification = UserSchoolVerification(
            memberId = 1L,
            schoolId = 1L,
            method = UserSchoolVerificationMethod.EMAIL
        )
    }

    @Test
    @DisplayName("revoke() 호출")
    fun revoke() {
        userSchoolVerification.revoke()

        Assertions.assertThat(userSchoolVerification.status).isEqualTo(UserSchoolVerificationStatus.REVOKED)
    }

    @Test
    @DisplayName("expire() 호출")
    fun expire() {
        userSchoolVerification.expire()

        Assertions.assertThat(userSchoolVerification.status).isEqualTo(UserSchoolVerificationStatus.EXPIRED)
    }

    @Test
    @DisplayName("학교 이메일 인증 성공 시 인증 정보와 만료 시각을 기록한다")
    fun verifyByEmail() {
        val now = LocalDateTime.of(2026, 5, 15, 12, 0)
        val expiresAt = now.plusDays(365)
        val emailHash = ByteArray(32) { 1 }
        val emailEncrypted = ByteArray(64) { 2 }

        userSchoolVerification.verifyByEmail(
            now = now,
            emailHash = emailHash,
            emailEncrypted = emailEncrypted,
            expiresAt = expiresAt,
        )

        Assertions.assertThat(userSchoolVerification.status).isEqualTo(UserSchoolVerificationStatus.VERIFIED)
        Assertions.assertThat(userSchoolVerification.verifiedAt).isEqualTo(now)
        Assertions.assertThat(userSchoolVerification.expiresAt).isEqualTo(expiresAt)
        Assertions.assertThat(userSchoolVerification.verifiedEmailHash).isEqualTo(emailHash)
        Assertions.assertThat(userSchoolVerification.verifiedEmailEncrypted).isEqualTo(emailEncrypted)
    }

    @Test
    @DisplayName("만료 시각이 없으면 만료되지 않은 것으로 판단한다")
    fun isExpiredWithoutExpiresAt() {
        val now = LocalDateTime.of(2026, 5, 15, 12, 0)

        Assertions.assertThat(userSchoolVerification.isExpired(now)).isFalse()
    }

    @Test
    @DisplayName("만료 시각이 현재 시각 이후이면 만료되지 않은 것으로 판단한다")
    fun isExpiredWithFutureExpiresAt() {
        val now = LocalDateTime.of(2026, 5, 15, 12, 0)
        userSchoolVerification.verifyByEmail(
            now = now.minusDays(1),
            emailHash = ByteArray(32) { 1 },
            expiresAt = now.plusSeconds(1),
        )

        Assertions.assertThat(userSchoolVerification.isExpired(now)).isFalse()
    }

    @Test
    @DisplayName("만료 시각이 현재 시각과 같거나 이전이면 만료된 것으로 판단한다")
    fun isExpiredWithExpiredExpiresAt() {
        val now = LocalDateTime.of(2026, 5, 15, 12, 0)
        userSchoolVerification.verifyByEmail(
            now = now.minusDays(1),
            emailHash = ByteArray(32) { 1 },
            expiresAt = now,
        )

        Assertions.assertThat(userSchoolVerification.isExpired(now)).isTrue()
    }

}
