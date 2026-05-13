package com.unit.platform.mail

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "unit.mail",
    name = ["enabled"],
    havingValue = "false",
    matchIfMissing = true,
)
class NoOpEmailSender : EmailSender {

    override fun send(message: EmailMessage) {
    }
}