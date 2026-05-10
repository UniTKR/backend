package com.unit.platform.security

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test

@DisplayName("JwtConfig 테스트")
class JwtConfigTest {

    @Test
    @DisplayName("JWT secret이 32바이트보다 짧으면 예외가 발생한다")
    fun shortSecretThrowsException() {
        val properties = JwtProperties(
            issuer = "https://unit.test",
            secret = "short-secret",
            accessTokenExpirationSeconds = 1800,
        )

        val jwtConfig = JwtConfig(properties)

        Assertions.assertThatThrownBy { jwtConfig.jwtDecoder() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("JWT secret must be at least 32 bytes.")
    }

}