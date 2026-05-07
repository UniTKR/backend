package com.unit.platform.error

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@WebMvcTest(TestController::class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
@Import(GlobalExceptionHandler::class)
@DisplayName("공통 에러 응답 문서화 테스트")
class CommonErrorDocsTest @Autowired constructor(
    private val mockMvc: MockMvc
) {

    @Test
    @DisplayName("비즈니스 예외 응답을 문서화한다")
    fun businessException() {
        mockMvc.get("/test/business-error")
            .andExpect {
                status { isNotFound() }
            }
            .andDo {
                handle(
                    document(
                        "common-error/business-exception",
                        responseFields(
                            fieldWithPath("code").description("애플리케이션 응답 코드"),
                            fieldWithPath("message").description("에러 메시지"),
                            fieldWithPath("traceId").description("요청 추적 ID"),
                            fieldWithPath("fieldErrors")
                                .type(JsonFieldType.ARRAY)
                                .optional()
                                .description("필드 검증 실패 목록")
                        )
                    )
                )
            }

    }
}

@RestController
private class TestController {

    @GetMapping("/test/business-error")
    fun businessException() {
        throw BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND)
    }
}