package com.unit.member.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test

@DisplayName("학교 이메일 인증 설정 테스트")
class SchoolEmailVerificationPropertiesTest {

    @Test
    @DisplayName("기본 설정값을 가진다")
    fun defaultProperties() {
        val properties = SchoolEmailVerificationProperties()

        assertThat(properties.codeExpirationSeconds).isEqualTo(300)
        assertThat(properties.resendCooldownSeconds).isEqualTo(60)
        assertThat(properties.maxAttemptCount).isEqualTo(5)
        assertThat(properties.verificationExpirationDays).isEqualTo(365)
    }
}
