package com.unit.member.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test

@DisplayName("SchoolEmailVerificationCodeGenerator 테스트")
class SchoolEmailVerificationCodeGeneratorTest {

    private val schoolEmailVerificationCodeGenerator = SchoolEmailVerificationCodeGenerator()

    @Test
    @DisplayName("이메일 인증 토큰 정상 생성")
    fun generate() {
        val token = schoolEmailVerificationCodeGenerator.generate()

        assertThat(token).isNotBlank()
        assertThat(token).hasSize(6)
        assertThat(token).matches("^[0-9]+$")
    }


    @Test
    @DisplayName("매번 다른 토큰을 생성한다")
    fun generateDifferentToken() {
        val tokens = (1..20)
            .map { schoolEmailVerificationCodeGenerator.generate() }
            .toSet()

        assertThat(tokens).hasSize(20)
    }
}