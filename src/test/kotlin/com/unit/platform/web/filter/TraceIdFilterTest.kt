package com.unit.platform.web.filter

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

@DisplayName("TraceId 필터")
class TraceIdFilterTest {

    private val filter = TraceIdFilter()

    @Test
    @DisplayName("요청 헤더에 traceId가 없으면 새 traceId를 응답 헤더에 추가한다")
    fun createsTraceIdWhenMissing() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        assertThat(response.getHeader(TraceIds.HEADER_NAME)).isNotBlank()
    }

    @Test
    @DisplayName("요청 헤더에 traceId가 있으면 같은 값을 응답 헤더에 사용한다")
    fun usesTraceIdFromRequestHeader() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()
        val traceId = "test-trace-id"

        request.addHeader(TraceIds.HEADER_NAME, traceId)

        filter.doFilter(request, response, chain)

        assertThat(response.getHeader(TraceIds.HEADER_NAME)).isEqualTo(traceId)
    }
}