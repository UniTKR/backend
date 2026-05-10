package com.unit.platform.error

import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Path
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException

@DisplayName("GlobalExceptionHandler 단위 테스트")
class GlobalExceptionHandlerUnitTest {

    private val handler = GlobalExceptionHandler()

    @Test
    @DisplayName("BusinessException 메시지가 null이면 ErrorCode 기본 메시지를 사용한다")
    fun handleBusinessException_nullMessage() {
        val exception = mock(BusinessException::class.java)

        given(exception.errorCode).willReturn(CommonErrorCode.RESOURCE_NOT_FOUND)
        given(exception.message).willReturn(null)

        val response = handler.handleBusinessException(exception)

        assertThat(response.statusCode).isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND.status)
        assertThat(response.body?.code).isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND.code)
        assertThat(response.body?.message).isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND.message)
    }

    @Test
    @DisplayName("필드 에러 기본 메시지가 null이면 fallback reason을 사용한다")
    fun handleMethodArgumentNotValid_nullDefaultMessage() {
        val exception = mock(MethodArgumentNotValidException::class.java)
        val bindingResult = mock(BindingResult::class.java)
        val fieldError = mock(FieldError::class.java)

        given(exception.bindingResult).willReturn(bindingResult)
        given(bindingResult.fieldErrors).willReturn(listOf(fieldError))
        given(fieldError.field).willReturn("name")
        given(fieldError.defaultMessage).willReturn(null)

        val response = handler.handleMethodArgumentNotValid(exception)

        assertThat(response.statusCode).isEqualTo(CommonErrorCode.VALIDATION_FAILED.status)
        assertThat(response.body?.code).isEqualTo(CommonErrorCode.VALIDATION_FAILED.code)
        assertThat(response.body?.fieldErrors).hasSize(1)
        assertThat(response.body?.fieldErrors?.first()?.field).isEqualTo("name")
        assertThat(response.body?.fieldErrors?.first()?.reason).isNotBlank()
    }

    @Test
    @DisplayName("ConstraintViolationException의 violation을 fieldErrors로 변환한다")
    fun handleConstraintViolation_withViolations() {
        val violation = mock(ConstraintViolation::class.java) as ConstraintViolation<*>
        val path = mock(Path::class.java)

        given(path.toString()).willReturn("name")
        given(violation.propertyPath).willReturn(path)
        given(violation.message).willReturn("must not be blank")

        val response = handler.handleConstraintViolation(
            ConstraintViolationException(setOf(violation)),
        )

        assertThat(response.statusCode).isEqualTo(CommonErrorCode.VALIDATION_FAILED.status)
        assertThat(response.body?.code).isEqualTo(CommonErrorCode.VALIDATION_FAILED.code)
        assertThat(response.body?.fieldErrors).hasSize(1)
        assertThat(response.body?.fieldErrors?.first()?.field).isEqualTo("name")
        assertThat(response.body?.fieldErrors?.first()?.reason).isEqualTo("must not be blank")
    }
}