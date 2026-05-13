package com.unit.platform.mail

import org.junit.jupiter.api.DisplayName
import kotlin.test.Test

@DisplayName("NoOpEmailSender 테스트")
class NoOpEmailSenderTest {

    private val emailSender = NoOpEmailSender()

    @Test
    @DisplayName("메일 발송 요청을 받아도 아무 작업도 하지 않는다")
    fun send() {
        emailSender.send(
            EmailMessage(
                to = "test@unit.com",
                subject = "subject",
                body = "body",
                html = false,
            ),
        )
    }
}
