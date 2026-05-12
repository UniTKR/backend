package com.unit.member.util

import org.springframework.stereotype.Component
import java.security.MessageDigest

@Component
class TokenHasher {

    fun hash(token: String): ByteArray {
        return MessageDigest
            .getInstance("SHA-256")
            .digest(token.toByteArray(Charsets.UTF_8))
    }
}