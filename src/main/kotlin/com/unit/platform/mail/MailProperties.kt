package com.unit.platform.mail

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "unit.mail")
data class MailProperties(
    val enabled: Boolean = false,
    val fromAddress: String = "no-reply@unit.local",
    val fromName: String = "UniTT"
)
