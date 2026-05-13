package com.unit.platform.mail

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "unit.mail", name = ["enabled"], havingValue = "true")
class JavaMailEmailSender(
    private val javaMailSender: JavaMailSender,
    private val properties: MailProperties
) : EmailSender {

    override fun send(message: EmailMessage) {
        val mimeMessage = javaMailSender.createMimeMessage()
        val helper = MimeMessageHelper(
            mimeMessage,
            false,
            Charsets.UTF_8.name(),
        )

        helper.setFrom(properties.fromAddress, properties.fromName)
        helper.setTo(message.to)
        helper.setSubject(message.subject)
        helper.setText(message.body, message.html)

        javaMailSender.send(mimeMessage)
    }
}