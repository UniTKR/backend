package com.unit.platform.mail

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("MailConfig 테스트")
class MailConfigTest {

    @Test
    @DisplayName("설정 객체를 생성할 수 있다")
    fun create() {
        assertThat(MailConfig()).isNotNull()
    }
}