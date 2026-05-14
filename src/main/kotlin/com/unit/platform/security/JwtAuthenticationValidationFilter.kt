package com.unit.platform.security

import com.unit.platform.error.CommonErrorCode
import com.unit.platform.web.filter.TraceIds
import com.unit.platform.web.response.ErrorResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.ObjectMapper

open class JwtAuthenticationValidationFilter(
    private val validators: List<JwtAuthenticationValidator>,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authentication = SecurityContextHolder.getContext().authentication
        val jwt = (authentication as? JwtAuthenticationToken)?.token

        if (jwt == null || validators.isEmpty()) {
            filterChain.doFilter(request, response)
            return
        }

        val valid = validators.all { it.isValid(jwt) }

        if (!valid) {
            writeInvalidTokenResponse(response)
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun writeInvalidTokenResponse(response: HttpServletResponse) {
        val errorCode = CommonErrorCode.INVALID_TOKEN

        response.status = errorCode.status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()

        objectMapper.writeValue(
            response.writer,
            ErrorResponse(
                code = errorCode.code,
                message = errorCode.message,
                traceId = TraceIds.current(),
            ),
        )
    }
}