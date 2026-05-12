package com.unit.member.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test

@DisplayName("RefreshTokenGenerator 테스트")
class RefreshTokenGeneratorTest {

    private val refreshTokenGenerator = RefreshTokenGenerator()

    @Test
    @DisplayName("리프레시 토큰 정상 생성")
    fun generate() {
        val token = refreshTokenGenerator.generate()

        assertThat(token).isNotBlank()
        assertThat(token).hasSize(86)
        assertThat(token).doesNotContain("=")
        assertThat(token).matches("^[A-Za-z0-9_-]+$")
    }

    @Test
    @DisplayName("매번 다른 토큰을 생성한다")
    fun generateDifferentToken() {
        val tokens = (1..20)
            .map { refreshTokenGenerator.generate() }
            .toSet()

        assertThat(tokens).hasSize(20)
    }
}