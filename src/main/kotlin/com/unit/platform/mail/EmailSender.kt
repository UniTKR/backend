package com.unit.platform.mail

interface EmailSender {

    fun send(message: EmailMessage)
}