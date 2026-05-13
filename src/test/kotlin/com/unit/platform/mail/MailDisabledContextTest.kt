package com.unit.platform.mail

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.test.Test

@SpringBootTest(
    properties = [
        "unit.mail.enabled=false",
    ],
)
@ActiveProfiles("test")
@DisplayName("메일 비활성화 설정 테스트")
class MailDisabledContextTest {

    @Autowired
    private lateinit var emailSender: EmailSender

    @Test
    @DisplayName("메일이 비활성화되면 NoOpEmailSender가 등록된다")
    fun noOpEmailSenderRegistered() {
        assertThat(emailSender).isInstanceOf(NoOpEmailSender::class.java)
    }
}
