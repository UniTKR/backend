package com.unit.member.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test

@DisplayName("TokenHasher 테스트")
class TokenHasherTest {

    private val tokenHasher = TokenHasher()

    @Test
    @DisplayName("토큰을 해시로 변환한다")
    fun hashToken() {
        val hash = tokenHasher.hash("refresh-token")

        assertThat(hash).hasSize(32)
    }

    @Test
    @DisplayName("같은 토큰은 같은 해시를 만든다")
    fun sameTokenSameHash() {
        val hash1 = tokenHasher.hash("refresh-token")
        val hash2 = tokenHasher.hash("refresh-token")

        assertThat(hash1).isEqualTo(hash2)
    }

    @Test
    @DisplayName("다른 토큰은 다른 해시를 만든다")
    fun differentTokenDifferentHash() {
        val hash1 = tokenHasher.hash("refresh-token-1")
        val hash2 = tokenHasher.hash("refresh-token-2")

        assertThat(hash1).isNotEqualTo(hash2)
    }
}