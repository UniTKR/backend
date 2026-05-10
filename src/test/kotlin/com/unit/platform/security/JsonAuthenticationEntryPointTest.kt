package com.unit.platform.security

import com.unit.platform.error.CommonErrorCode
import com.unit.platform.web.filter.TraceIds
import com.unit.platform.web.response.ErrorResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.AuthenticationException
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import kotlin.test.Test

@DisplayName("JsonAuthenticationEntryPoint 테스트")
class JsonAuthenticationEntryPointTest {

    private val objectMapper = jacksonObjectMapper()
    private val entryPoint = JsonAuthenticationEntryPoint(objectMapper)

    @Test
    @DisplayName("인증되지 않은 요청은 AUTH_REQUIRED 에러 응답을 반환한다")
    fun authRequired() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val exception = object : AuthenticationException("인증 실패") {}

        entryPoint.commence(request, response, exception)

        val body = objectMapper.readValue<ErrorResponse>(response.contentAsString)

        assertThat(response.status).isEqualTo(CommonErrorCode.AUTH_REQUIRED.status.value())
        assertThat(response.contentType).isEqualTo("application/json;charset=UTF-8")
        assertThat(response.characterEncoding).isEqualTo("UTF-8")
        assertThat(body.code).isEqualTo(CommonErrorCode.AUTH_REQUIRED.code)
        assertThat(body.message).isEqualTo(CommonErrorCode.AUTH_REQUIRED.message)
        assertThat(body.traceId).isNotBlank()
        assertThat(body.fieldErrors).isNull()
    }

    @Test
    @DisplayName("요청 Trace ID가 있으면 에러 응답 traceId에 같은 값을 사용한다")
    fun authRequiredWithTraceId() {
        val traceId = "test-trace-id"
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val exception = object : AuthenticationException("인증 실패") {}

        MDC.put(TraceIds.MDC_KEY, traceId)

        try {
            entryPoint.commence(request, response, exception)

            val body = objectMapper.readValue<ErrorResponse>(response.contentAsString)

            assertThat(body.traceId).isEqualTo(traceId)
        } finally {
            MDC.remove(TraceIds.MDC_KEY)
        }
    }
}