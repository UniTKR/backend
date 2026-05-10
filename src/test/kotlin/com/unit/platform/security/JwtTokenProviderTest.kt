package com.unit.platform.security

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import kotlin.test.Test

@SpringJUnitConfig(
    classes = [
        JwtConfig::class,
        JwtTokenProvider::class
    ]
)
@TestPropertySource(
    properties = [
        "unit.security.jwt.issuer=unit-test",
        "unit.security.jwt.secret=test-jwt-secret-must-be-at-least-32-bytes",
        "unit.security.jwt.access-token-expiration-seconds=1800",
    ]
)
@DisplayName("JwtTokenProvider 테스트")
class JwtTokenProviderTest @Autowired constructor(
    private val jwtTokenProvider: JwtTokenProvider,
    private val jwtDecoder: JwtDecoder
) {

    @Test
    @DisplayName("access token을 발급한다")
    fun createAccessToken() {
        val memberId = 1L

        val token = jwtTokenProvider.createAccessToken(memberId)

        assertThat(token).isNotBlank()
    }

    @Test
    @DisplayName("발급한 access token은 디코딩할 수 있다")
    fun decodeAccessToken() {
        val memberId = 1L

        val token = jwtTokenProvider.createAccessToken(memberId)
        val jwt = jwtDecoder.decode(token)

        assertThat(jwt.subject).isEqualTo(memberId.toString())
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("unit-test")
        assertThat(jwt.expiresAt).isNotNull()
        assertThat(jwt.issuedAt).isNotNull()
    }

    @Test
    @DisplayName("access token에는 memberId claim이 포함된다")
    fun accessTokenContainsMemberIdClaim() {
        val memberId = 1L

        val token = jwtTokenProvider.createAccessToken(memberId)
        val jwt = jwtDecoder.decode(token)

        val claimMemberId = jwt.getClaim<Number>("memberId").toLong()

        assertThat(claimMemberId).isEqualTo(memberId)
    }

}