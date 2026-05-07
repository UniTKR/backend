package com.unit.platform.error

import com.unit.platform.web.filter.TraceIds
import com.unit.platform.web.response.ErrorResponse
import com.unit.platform.web.response.FieldErrorResponse
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(ex: BusinessException): ResponseEntity<ErrorResponse> {
        return error(
            errorCode = ex.errorCode,
            message = ex.message ?: ex.errorCode.message
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(ex: MethodArgumentNotValidException) : ResponseEntity<ErrorResponse> {
        val fieldErrors = ex.bindingResult.fieldErrors.map {
            FieldErrorResponse(
                field = it.field,
                reason = it.defaultMessage ?: "올바르지 않은 값입니다."
            )
        }
        return error(
            errorCode = CommonErrorCode.VALIDATION_FAILED,
            fieldErrors = fieldErrors
        )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        ex: ConstraintViolationException,
    ): ResponseEntity<ErrorResponse> {
        val fieldErrors = ex.constraintViolations.map {
            FieldErrorResponse(
                field = it.propertyPath.toString(),
                reason = it.message,
            )
        }

        return error(
            errorCode = CommonErrorCode.VALIDATION_FAILED,
            fieldErrors = fieldErrors,
        )
    }

    @ExceptionHandler(
        HttpMessageNotReadableException::class,
        MissingServletRequestParameterException::class,
        MethodArgumentTypeMismatchException::class,
    )
    fun handleBadRequest(): ResponseEntity<ErrorResponse> {
        return error(CommonErrorCode.MALFORMED_JSON)
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthentication(): ResponseEntity<ErrorResponse> {
        return error(CommonErrorCode.AUTH_REQUIRED)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(): ResponseEntity<ErrorResponse> {
        return error(CommonErrorCode.FORBIDDEN)
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(): ResponseEntity<ErrorResponse> {
        return error(CommonErrorCode.CONFLICT)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(): ResponseEntity<ErrorResponse> {
        return error(CommonErrorCode.INTERNAL_ERROR)
    }

    private fun error(
        errorCode: UnitErrorCode,
        message: String = errorCode.message,
        fieldErrors: List<FieldErrorResponse>? = null
    ) : ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(errorCode.status)
            .body(
                ErrorResponse(
                    code = errorCode.code,
                    message = message,
                    traceId = TraceIds.current(),
                    fieldErrors = fieldErrors
                )
            )
    }
}