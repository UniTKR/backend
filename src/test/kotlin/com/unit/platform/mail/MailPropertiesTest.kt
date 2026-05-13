package com.unit.platform.mail

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("MailProperties 테스트")
class MailPropertiesTest {

    @Test
    @DisplayName("기본값을 가진다")
    fun defaultValues() {
        val properties = MailProperties()

        assertThat(properties.enabled).isFalse()
        assertThat(properties.fromAddress).isEqualTo("no-reply@unit.local")
        assertThat(properties.fromName).isEqualTo("UniTT")
    }
}
