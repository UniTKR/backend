package com.unit.member.controller

import com.unit.member.dto.SchoolResponse
import com.unit.member.service.SchoolQueryUseCase
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(SchoolController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("학교 Controller 테스트")
class SchoolControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockitoBean
    private lateinit var schoolQueryUseCase: SchoolQueryUseCase

    @Test
    @DisplayName("학교 목록을 조회한다")
    fun getSchools() {
        given(schoolQueryUseCase.getSchools(null)).willReturn(
            listOf(
                SchoolResponse(
                    id = 1L,
                    name = "서울대학교",
                    region = "서울",
                ),
            ),
        )

        mockMvc.get("/api/v1/schools") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.code") { value("OK") }
            jsonPath("$.data[0].id") { value(1) }
            jsonPath("$.data[0].name") { value("서울대학교") }
            jsonPath("$.data[0].region") { value("서울") }
        }

        then(schoolQueryUseCase).should().getSchools(null)
        then(schoolQueryUseCase).shouldHaveNoMoreInteractions()
    }

    @Test
    @DisplayName("키워드로 학교 목록을 조회한다")
    fun getSchoolsWithKeyword() {
        given(schoolQueryUseCase.getSchools("서울")).willReturn(
            listOf(
                SchoolResponse(
                    id = 1L,
                    name = "서울대학교",
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
        }

        then(schoolQueryUseCase).should().getSchools("서울")
        then(schoolQueryUseCase).shouldHaveNoMoreInteractions()
    }
}
