package com.unit.platform.mail

data class EmailMessage(
    val to: String,
    val subject: String,
    val body: String,
    val html: Boolean = false,
)