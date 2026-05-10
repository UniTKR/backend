package com.unit.member.controller

import com.unit.member.dto.SchoolResponse
import com.unit.member.service.SchoolQueryUseCase
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(SchoolController::class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
@DisplayName("학교 API 문서화 테스트")
class SchoolDocsTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockitoBean
    private lateinit var schoolQueryUseCase: SchoolQueryUseCase

    @Test
    @DisplayName("학교 목록 조회 API를 문서화한다")
    fun getSchools() {
        given(schoolQueryUseCase.getSchools("서울")).willReturn(
            listOf(
                SchoolResponse(
                    id = 1L,
                    name = "서울대학교",
                    region = "서울",
                ),
                SchoolResponse(
                    id = 2L,
                    name = "서울시립대학교",
                    region = "서울",
                ),
            ),
        )

        mockMvc.get("/api/v1/schools") {
            param("keyword", "서울")
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.code") { value("OK") }
            jsonPath("$.data[0].id") { value(1) }
            jsonPath("$.data[0].name") { value("서울대학교") }
            jsonPath("$.data[0].region") { value("서울") }
        }.andDo {
            handle(
                document(
                    "member/schools/list",
                    queryParameters(
                        parameterWithName("keyword")
                            .optional()
                            .description("학교명 검색 키워드. 생략하거나 공백이면 활성 학교 전체 목록을 조회합니다."),
                    ),
                    responseFields(
                        fieldWithPath("code")
                            .type(JsonFieldType.STRING)
                            .description("애플리케이션 응답 코드"),
                        fieldWithPath("data")
                            .type(JsonFieldType.ARRAY)
                            .description("학교 목록"),
                        fieldWithPath("data[].id")
                            .type(JsonFieldType.NUMBER)
                            .description("학교 ID"),
                        fieldWithPath("data[].name")
                            .type(JsonFieldType.STRING)
                            .description("학교명"),
                        fieldWithPath("data[].region")
                            .type(JsonFieldType.STRING)
                            .optional()
                            .description("학교 지역"),
                    ),
                ),
            )
        }

        then(schoolQueryUseCase).should().getSchools("서울")
        then(schoolQueryUseCase).shouldHaveNoMoreInteractions()
    }
}
