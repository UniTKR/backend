package com.unit.platform.security

import com.unit.platform.error.CommonErrorCode
import com.unit.platform.web.filter.TraceIds
import com.unit.platform.web.response.ErrorResponse
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.access.AccessDeniedException
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import kotlin.test.Test

@DisplayName("JsonAccessDeniedHandler 테스트")
class JsonAccessDeniedHandlerTest {

    private val objectMapper = jacksonObjectMapper()
    private val accessDeniedHandler = JsonAccessDeniedHandler(objectMapper)

    @Test
    @DisplayName("권한이 부족한 요청은 FORBIDDEN 에러 응답을 반환한다")
    fun forbidden() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val exception = AccessDeniedException("접근 거부")

        accessDeniedHandler.handle(request, response, exception)

        val body = objectMapper.readValue<ErrorResponse>(response.contentAsString)

        assertThat(response.status).isEqualTo(CommonErrorCode.FORBIDDEN.status.value())
        assertThat(response.contentType).isEqualTo("application/json;charset=UTF-8")
        assertThat(response.characterEncoding).isEqualTo("UTF-8")
        assertThat(body.code).isEqualTo(CommonErrorCode.FORBIDDEN.code)
        assertThat(body.message).isEqualTo(CommonErrorCode.FORBIDDEN.message)
        assertThat(body.traceId).isNotBlank()
        assertThat(body.fieldErrors).isNull()
    }

    @Test
    @DisplayName("MDC에 Trace ID가 있으면 에러 응답 traceId에 같은 값을 사용한다")
    fun forbiddenWithTraceId() {
        val traceId = "test-trace-id"
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val exception = AccessDeniedException("접근 거부")

        MDC.put(TraceIds.MDC_KEY, traceId)

        try {
            accessDeniedHandler.handle(request, response, exception)

            val body = objectMapper.readValue<ErrorResponse>(response.contentAsString)

            assertThat(body.traceId).isEqualTo(traceId)
        } finally {
            MDC.remove(TraceIds.MDC_KEY)
        }
    }
}