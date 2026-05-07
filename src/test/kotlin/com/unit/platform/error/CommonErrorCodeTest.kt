package com.unit.platform.error

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

@DisplayName("공통 에러 코드")
class CommonErrorCodeTest {

    @Test
    @DisplayName("에러 코드는 code, message, status를 가진다")
    fun exposesCodeMessageStatus() {
        val errorCode = CommonErrorCode.VALIDATION_FAILED

        assertThat(errorCode.code).isEqualTo("VALIDATION_FAILED")
        assertThat(errorCode.message).isEqualTo("요청 값이 올바르지 않습니다.")
        assertThat(errorCode.status).isEqualTo(HttpStatus.BAD_REQUEST)
    }
}