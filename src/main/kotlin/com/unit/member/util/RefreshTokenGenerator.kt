package com.unit.member.util

import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64

@Component
class RefreshTokenGenerator {

    private val secureRandom = SecureRandom()
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    fun generate(): String {
        val bytes = ByteArray(64)
        secureRandom.nextBytes(bytes)
        return encoder.encodeToString(bytes)
    }
}