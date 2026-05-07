package com.unit.platform.error

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@WebMvcTest(ExceptionTestController::class)
@Import(GlobalExceptionHandler::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("전역 예외 처리")
class GlobalExceptionHandlerTest @Autowired constructor(
    private val mockMvc: MockMvc
) {

    @Test
    @DisplayName("BusinessException이 발생하면 정의된 에러 응답을 반환한다")
    fun businessException() {
        mockMvc.post("/test/business-exception")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("RESOURCE_NOT_FOUND") }
                jsonPath("$.message") { value("요청한 리소스를 찾을 수 없습니다.") }
                jsonPath("$.traceId") { exists() }
                jsonPath("$.fieldErrors") { doesNotExist() }
            }
    }

    @Test
    @DisplayName("요청 본문 검증에 실패하면 fieldErrors를 포함한다")
    fun validationError() {
        mockMvc.post("/test/validation") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name": ""}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("VALIDATION_FAILED") }
            jsonPath("$.message") { value("요청 값이 올바르지 않습니다.") }
            jsonPath("$.traceId") { exists() }
            jsonPath("$.fieldErrors[0].field") { value("name") }
        }
    }

    @Test
    @DisplayName("JSON 형식이 잘못되면 MALFORMED_JSON을 반환한다")
    fun malformedJson() {
        mockMvc.post("/test/validation") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name": """
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("MALFORMED_JSON") }
            jsonPath("$.message") { value("JSON 형식이 올바르지 않습니다.") }
            jsonPath("$.traceId") { exists() }
        }
    }

    @Test
    @DisplayName("예상하지 못한 예외는 INTERNAL_ERROR로 변환한다")
    fun unexpectedException() {
        mockMvc.post("/test/unexpected-exception")
            .andExpect {
                status { isInternalServerError() }
                jsonPath("$.code") { value("INTERNAL_ERROR") }
                jsonPath("$.message") { value("서버 오류가 발생했습니다.") }
                jsonPath("$.traceId") { exists() }
            }
    }
}

@RestController
private class ExceptionTestController {
    @PostMapping("/test/business-exception")
    fun businessException() {
        throw BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND)
    }

    @PostMapping("/test/validation")
    fun validation(
        @Valid @RequestBody request: TestRequest,
    ) {
    }

    @PostMapping("/test/unexpected-exception")
    fun unexpectedException() {
        throw IllegalStateException("boom")
    }
}

data class TestRequest(
    @field:NotBlank
    val name: String,
)