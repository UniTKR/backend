package com.unit.member.util

import org.springframework.stereotype.Component
import java.security.MessageDigest

@Component
class EmailHasher {

    fun hash(email: String): ByteArray {
        val normalized = email.trim().lowercase()
        return MessageDigest.getInstance("SHA-256")
            .digest(normalized.toByteArray(Charsets.UTF_8))
    }
}