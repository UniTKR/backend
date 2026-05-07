package com.unit.platform.web.response

data class ErrorResponse(
    val code: String,
    val message: String,
    val traceId: String,
    val fieldErrors: List<FieldErrorResponse>? = null
)

data class FieldErrorResponse(
    val field: String,
    val reason: String,
)
