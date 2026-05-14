package com.unit.platform.security

import com.unit.platform.error.CommonErrorCode
import com.unit.platform.web.response.ErrorResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import kotlin.test.Test

@DisplayName("JWT 인증 추가 검증 필터 테스트")
class JwtAuthenticationValidationFilterTest {

    private val objectMapper = jacksonObjectMapper()

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    @DisplayName("JWT 인증 정보가 없으면 다음 필터로 넘긴다")
    fun passThroughWithoutJwt() {
        val validator = mockk<JwtAuthenticationValidator>()
        val filter = JwtAuthenticationValidationFilter(listOf(validator), objectMapper)
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = mockk<FilterChain>(relaxed = true)

        filter.doFilter(request, response, chain)

        verify(exactly = 1) { chain.doFilter(request, response) }
        verify(exactly = 0) { validator.isValid(any()) }
    }

    @Test
    @DisplayName("검증기가 없으면 다음 필터로 넘긴다")
    fun passThroughWithoutValidators() {
        val filter = JwtAuthenticationValidationFilter(emptyList(), objectMapper)
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = mockk<FilterChain>(relaxed = true)

        SecurityContextHolder.getContext().authentication = JwtAuthenticationToken(createJwt())

        filter.doFilter(request, response, chain)

        verify(exactly = 1) { chain.doFilter(request, response) }
    }

    @Test
    @DisplayName("모든 검증을 통과하면 다음 필터로 넘긴다")
    fun passThroughWhenValid() {
        val jwt = createJwt()
        val validator = mockk<JwtAuthenticationValidator>()
        val filter = JwtAuthenticationValidationFilter(listOf(validator), objectMapper)
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = mockk<FilterChain>(relaxed = true)

        SecurityContextHolder.getContext().authentication = JwtAuthenticationToken(jwt)
        every { validator.isValid(jwt) } returns true

        filter.doFilter(request, response, chain)

        verify(exactly = 1) { validator.isValid(jwt) }
        verify(exactly = 1) { chain.doFilter(request, response) }
    }

    @Test
    @DisplayName("검증에 실패하면 INVALID_TOKEN 응답을 반환하고 다음 필터로 넘기지 않는다")
    fun rejectWhenInvalid() {
        val jwt = createJwt()
        val validator = mockk<JwtAuthenticationValidator>()
        val filter = JwtAuthenticationValidationFilter(listOf(validator), objectMapper)
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = mockk<FilterChain>(relaxed = true)

        SecurityContextHolder.getContext().authentication = JwtAuthenticationToken(jwt)
        every { validator.isValid(jwt) } returns false

        filter.doFilter(request, response, chain)

        val body = objectMapper.readValue<ErrorResponse>(response.contentAsString)

        assertThat(response.status).isEqualTo(CommonErrorCode.INVALID_TOKEN.status.value())
        assertThat(body.code).isEqualTo(CommonErrorCode.INVALID_TOKEN.code)
        assertThat(body.message).isEqualTo(CommonErrorCode.INVALID_TOKEN.message)
        assertThat(body.traceId).isNotBlank()

        verify(exactly = 1) { validator.isValid(jwt) }
        verify(exactly = 0) { chain.doFilter(any(), any()) }
    }

    private fun createJwt(subject: String = "1"): Jwt {
        return Jwt.withTokenValue("access-token")
            .header("alg", "none")
            .subject(subject)
            .build()
    }

}