package com.unit.platform.mail

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("EmailMessage 테스트")
class EmailMessageTest {

    @Test
    @DisplayName("html 기본값은 false다")
    fun defaultHtmlIsFalse() {
        val message = EmailMessage(
            to = "test@snu.ac.kr",
            subject = "subject",
            body = "body",
        )

        assertThat(message.html).isFalse()
    }
}