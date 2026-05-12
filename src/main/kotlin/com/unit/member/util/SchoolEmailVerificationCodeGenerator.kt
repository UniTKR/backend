package com.unit.member.util

import org.springframework.stereotype.Component
import java.security.SecureRandom

@Component
class SchoolEmailVerificationCodeGenerator {

    private val random = SecureRandom()

    fun generate(): String {
        return random.nextInt(1_000_000).toString().padStart(6, '0')
    }
}