package com.unit.platform.mail

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mail.javamail.JavaMailSender
import java.util.*

@DisplayName("JavaMailEmailSender 테스트")
class JavaMailEmailSenderTest {

    private val javaMailSender = mockk<JavaMailSender>()

    private val sender = JavaMailEmailSender(
        javaMailSender = javaMailSender,
        properties = MailProperties(
            enabled = true,
            fromAddress = "no-reply@unit.test",
            fromName = "UniTT",
        ),
    )

    @Test
    @DisplayName("HTML 메일을 전송한다")
    fun sendHtmlMail() {
        val mimeMessage = createMimeMessage()

        every { javaMailSender.createMimeMessage() } returns mimeMessage
        every { javaMailSender.send(mimeMessage) } answers {
            mimeMessage.saveChanges()
            Unit
        }

        sender.send(
            EmailMessage(
                to = "test@snu.ac.kr",
                subject = "[UniT] 학교 이메일 인증 코드",
                body = "<html><body><strong>123456</strong></body></html>",
                html = true,
            ),
        )

        val from = mimeMessage.from.first() as InternetAddress
        val to = mimeMessage.allRecipients.first() as InternetAddress

        assertThat(from.address).isEqualTo("no-reply@unit.test")
        assertThat(from.personal).isEqualTo("UniTT")
        assertThat(to.address).isEqualTo("test@snu.ac.kr")
        assertThat(mimeMessage.subject).isEqualTo("[UniT] 학교 이메일 인증 코드")
        assertThat(mimeMessage.contentType).contains("text/html")
        assertThat(mimeMessage.content.toString()).contains("<strong>123456</strong>")

        verify(exactly = 1) { javaMailSender.createMimeMessage() }
        verify(exactly = 1) { javaMailSender.send(mimeMessage) }
    }

    @Test
    @DisplayName("일반 텍스트 메일을 전송한다")
    fun sendPlainTextMail() {
        val mimeMessage = createMimeMessage()

        every { javaMailSender.createMimeMessage() } returns mimeMessage
        every { javaMailSender.createMimeMessage() } returns mimeMessage
        every { javaMailSender.send(mimeMessage) } answers {
            mimeMessage.saveChanges()
            Unit
        }

        sender.send(
            EmailMessage(
                to = "test@snu.ac.kr",
                subject = "plain subject",
                body = "plain body",
                html = false,
            ),
        )

        assertThat(mimeMessage.subject).isEqualTo("plain subject")
        assertThat(mimeMessage.contentType).contains("text/plain")
        assertThat(mimeMessage.content.toString()).contains("plain body")

        verify(exactly = 1) { javaMailSender.send(mimeMessage) }
    }

    private fun createMimeMessage(): MimeMessage {
        return MimeMessage(Session.getInstance(Properties()))
    }

}