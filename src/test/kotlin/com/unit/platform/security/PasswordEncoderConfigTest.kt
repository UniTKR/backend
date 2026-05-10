package com.unit.platform.security

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import kotlin.test.Test

@SpringJUnitConfig(PasswordEncoderConfig::class)
@DisplayName("PasswordEncoderConfig 테스트")
class PasswordEncoderConfigTest @Autowired constructor(
    private val passwordEncoder: PasswordEncoder
) {

    @Test
    @DisplayName("encode 결과가 원문과 다르다")
    fun pwEncode() {
        val plainText = "plain-password"
        val encodedPassword = passwordEncoder.encode(plainText)

        assertThat(encodedPassword).isNotEqualTo(plainText)
    }

    @Test
    @DisplayName("encode한 비밀번호는 원문 비밀번호와 매칭된다")
    fun pwMatches() {
        val plainText = "plain-password"
        val encodedPassword = passwordEncoder.encode(plainText)

        assertThat(passwordEncoder.matches(plainText, encodedPassword)).isTrue()
    }
}