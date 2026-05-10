package com.unit.member.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test

@DisplayName("EmailHasher 테스트")
class EmailHasherTest {

    private val emailHasher = EmailHasher()

    @Test
    @DisplayName("이메일을 SHA-256 해시로 변환한다")
    fun hashEmail() {
        val hash = emailHasher.hash("test@unit.com")

        assertThat(hash).hasSize(32)
    }

    @Test
    @DisplayName("같은 이메일은 항상 같은 해시를 만든다")
    fun sameEmailSameHash() {
        val hash1 = emailHasher.hash("test@unit.com")
        val hash2 = emailHasher.hash("test@unit.com")

        assertThat(hash1).isEqualTo(hash2)
    }

    @Test
    @DisplayName("이메일 앞뒤 공백과 대소문자는 정규화한다")
    fun normalizeEmail() {
        val hash1 = emailHasher.hash(" Test@Unit.com ")
        val hash2 = emailHasher.hash("test@unit.com")

        assertThat(hash1).isEqualTo(hash2)
    }

    @Test
    @DisplayName("다른 이메일은 다른 해시를 만든다")
    fun differentEmailDifferentHash() {
        val hash1 = emailHasher.hash("test1@unit.com")
        val hash2 = emailHasher.hash("test2@unit.com")

        assertThat(hash1).isNotEqualTo(hash2)
    }
}