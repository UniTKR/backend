package com.unit.platform.error

import org.springframework.http.HttpStatus

enum class CommonErrorCode(
    override val code: String,
    override val message: String,
    override val status: HttpStatus
) : UnitErrorCode {

    VALIDATION_FAILED("VALIDATION_FAILED", "요청 값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    MALFORMED_JSON("MALFORMED_JSON", "JSON 형식이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),

    AUTH_REQUIRED("AUTH_REQUIRED", "로그인이 필요합니다.", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("INVALID_TOKEN", "인증 토큰이 유효하지 않습니다.", HttpStatus.UNAUTHORIZED),

    FORBIDDEN("FORBIDDEN", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CONFLICT("CONFLICT", "현재 상태에서 처리할 수 없습니다.", HttpStatus.CONFLICT),
    RATE_LIMITED("RATE_LIMITED", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.", HttpStatus.TOO_MANY_REQUESTS),

    INTERNAL_ERROR("INTERNAL_ERROR", "서버 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
}