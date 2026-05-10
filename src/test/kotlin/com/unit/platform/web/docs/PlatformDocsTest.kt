package com.unit.platform.web.docs

import com.unit.platform.error.GlobalExceptionHandler
import com.unit.platform.security.SecurityConfig
import com.unit.platform.web.filter.TraceIdFilter
import com.unit.platform.web.filter.TraceIds
import com.unit.platform.web.response.ApiResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.security.core.AuthenticationException
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@WebMvcTest(PlatformDocsController::class)
@AutoConfigureMockMvc(addFilters = true)
@AutoConfigureRestDocs
@Import(
    GlobalExceptionHandler::class,
    SecurityConfig::class,
    TraceIdFilter::class,
)
@DisplayName("Platform API 문서화 테스트")
class PlatformDocsTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @Test
    @DisplayName("공통 성공 응답 예시를 문서화한다")
    fun successResponse() {
        mockMvc.get("/docs/platform/success") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.code") { value("OK") }
            jsonPath("$.data.message") { value("success") }
        }.andDo {
            handle(
                document(
                    "common-response/success",
                    responseFields(
                        fieldWithPath("code")
                            .type(JsonFieldType.STRING)
                            .description("애플리케이션 응답 코드"),
                        fieldWithPath("data")
                            .type(JsonFieldType.OBJECT)
                            .description("API 응답 데이터"),
                        fieldWithPath("data.message")
                            .type(JsonFieldType.STRING)
                            .description("성공 응답 예시 메시지"),
                    ),
                ),
            )
        }
    }

    @Test
    @DisplayName("검증 실패 에러 응답을 문서화한다")
    fun validationFailed() {
        mockMvc.post("/docs/platform/validation") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = """{"name": ""}"""
        }.andExpect {
            status { isBadRequest() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.code") { value("VALIDATION_FAILED") }
            jsonPath("$.message") { value("요청 값이 올바르지 않습니다.") }
            jsonPath("$.traceId") { exists() }
            jsonPath("$.fieldErrors[0].field") { value("name") }
        }.andDo {
            handle(
                document(
                    "common-error/validation-failed",
                    requestFields(
                        fieldWithPath("name")
                            .type(JsonFieldType.STRING)
                            .description("검증 실패를 발생시키기 위한 이름 필드"),
                    ),
                    responseFields(
                        fieldWithPath("code")
                            .type(JsonFieldType.STRING)
                            .description("애플리케이션 에러 코드"),
                        fieldWithPath("message")
                            .type(JsonFieldType.STRING)
                            .description("에러 메시지"),
                        fieldWithPath("traceId")
                            .type(JsonFieldType.STRING)
                            .description("요청 추적 ID"),
                        fieldWithPath("fieldErrors")
                            .type(JsonFieldType.ARRAY)
                            .description("필드 검증 실패 목록"),
                        fieldWithPath("fieldErrors[].field")
                            .type(JsonFieldType.STRING)
                            .description("검증에 실패한 필드명"),
                        fieldWithPath("fieldErrors[].reason")
                            .type(JsonFieldType.STRING)
                            .description("검증 실패 사유"),
                    ),
                ),
            )
        }
    }

    @Test
    @DisplayName("인증 실패 에러 응답을 문서화한다")
    fun authRequired() {
        mockMvc.get("/docs/platform/auth-required") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isUnauthorized() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.code") { value("AUTH_REQUIRED") }
            jsonPath("$.message") { value("로그인이 필요합니다.") }
            jsonPath("$.traceId") { exists() }
            jsonPath("$.fieldErrors") { doesNotExist() }
        }.andDo {
            handle(
                document(
                    "common-error/auth-required",
                    responseFields(
                        fieldWithPath("code")
                            .type(JsonFieldType.STRING)
                            .description("애플리케이션 에러 코드"),
                        fieldWithPath("message")
                            .type(JsonFieldType.STRING)
                            .description("에러 메시지"),
                        fieldWithPath("traceId")
                            .type(JsonFieldType.STRING)
                            .description("요청 추적 ID"),
                        fieldWithPath("fieldErrors")
                            .type(JsonFieldType.ARRAY)
                            .optional()
                            .description("필드 검증 실패 목록. 인증 실패 응답에서는 내려가지 않습니다."),
                    ),
                ),
            )
        }
    }

    @Test
    @DisplayName("Trace ID 응답 헤더를 문서화한다")
    fun traceIdHeader() {
        mockMvc.get("/docs/platform/trace-id") {
            header(TraceIds.HEADER_NAME, "docs-trace-id")
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            header { string(TraceIds.HEADER_NAME, "docs-trace-id") }
        }.andDo {
            handle(
                document(
                    "platform/trace-id",
                    responseHeaders(
                        headerWithName(TraceIds.HEADER_NAME)
                            .description("요청 추적 ID. 요청 헤더에 값이 있으면 같은 값을 응답하고, 없으면 서버가 새로 생성합니다."),
                    ),
                ),
            )
        }
    }
}

@RestController
private class PlatformDocsController {

    @GetMapping("/docs/platform/success")
    fun success(): ApiResponse<Map<String, String>> {
        return ApiResponse.ok(mapOf("message" to "success"))
    }

    @PostMapping("/docs/platform/validation")
    fun validation(
        @Valid @RequestBody request: PlatformDocsRequest,
    ): ApiResponse<Unit> {
        return ApiResponse.ok()
    }

    @GetMapping("/docs/platform/auth-required")
    fun authRequired(): ApiResponse<Unit> {
        throw object : AuthenticationException("인증 실패") {}
    }

    @GetMapping("/docs/platform/trace-id")
    fun traceId(): ApiResponse<Unit> {
        return ApiResponse.ok()
    }
}

private data class PlatformDocsRequest(
    @field:NotBlank
    val name: String,
)
