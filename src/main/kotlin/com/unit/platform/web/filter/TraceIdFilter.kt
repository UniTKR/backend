package com.unit.platform.web.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

object TraceIds {
    const val HEADER_NAME = "X-Trace-Id"
    const val MDC_KEY = "traceId"

    fun current(): String {
        return MDC.get(MDC_KEY) ?: UUID.randomUUID().toString().replace("-", "")
    }
}

@Component
class TraceIdFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val traceId = request.getHeader(TraceIds.HEADER_NAME)
            ?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString().replace("-", "")

        MDC.put(TraceIds.MDC_KEY, traceId)
        response.setHeader(TraceIds.HEADER_NAME, traceId)

        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(TraceIds.MDC_KEY)
        }
    }
}