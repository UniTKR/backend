package com.unit.member.controller

import com.unit.member.dto.SchoolAuthDto.*
import com.unit.member.enums.UserSchoolVerificationStatus
import com.unit.member.exception.MemberErrorCode
import com.unit.member.service.SchoolEmailVerificationUseCase
import com.unit.platform.error.BusinessException
import com.unit.platform.error.GlobalExceptionHandler
import com.unit.platform.security.JsonAccessDeniedHandler
import com.unit.platform.security.JsonAuthenticationEntryPoint
import com.unit.platform.security.SecurityConfig
import org.junit.jupiter.api.DisplayName
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.Test

@WebMvcTest(SchoolEmailVerificationController::class)
@AutoConfigureMockMvc
@Import(
    GlobalExceptionHandler::class,
    SecurityConfig::class,
    JsonAuthenticationEntryPoint::class,
    JsonAccessDeniedHandler::class,
)
@DisplayName("SchoolEmailVerificationController 테스트")
class SchoolEmailVerificationControllerTest @Autowired constructor(
    private val mockMvc: MockMvc
) {

    @MockitoBean
    private lateinit var schoolEmailVerificationUseCase: SchoolEmailVerificationUseCase

    @MockitoBean
    private lateinit var jwtDecoder: JwtDecoder

    @Test
    @DisplayName("학교 이메일 인증 요청에 성공하면 200 OK를 반환한다")
    fun requestSuccess() {
        val request = SchoolEmailVerificationRequest(
            schoolId = 1L,
            email = "test@snu.ac.kr",
        )

        given(schoolEmailVerificationUseCase.request(1L, request)).willReturn(
            SchoolEmailVerificationResponse(
                schoolId = 1L,
                email = "test@snu.ac.kr",
                expiresIn = 300L,
            ),
        )

        mockMvc.perform(
            post("/api/v1/school-email-verifications/request")
                .with(jwt().jwt { it.subject("1").claim("memberId", 1L) })
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "schoolId": 1,
                      "email": "test@snu.ac.kr"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data.schoolId").value(1))
            .andExpect(jsonPath("$.data.email").value("test@snu.ac.kr"))
            .andExpect(jsonPath("$.data.expiresIn").value(300))

        then(schoolEmailVerificationUseCase).should().request(1L, request)
        then(schoolEmailVerificationUseCase).shouldHaveNoMoreInteractions()
    }

    @Test
    @DisplayName("학교 이메일 인증 확인에 성공하면 200 OK를 반환한다")
    fun confirmSuccess() {
        val request = SchoolEmailVerificationConfirmRequest(
            schoolId = 1L,
            email = "test@snu.ac.kr",
            code = "123456",
        )

        given(schoolEmailVerificationUseCase.confirm(1L, request)).willReturn(
            SchoolEmailVerificationConfirmResponse(
                schoolId = 1L,
                status = UserSchoolVerificationStatus.VERIFIED,
            ),
        )

        mockMvc.perform(
            post("/api/v1/school-email-verifications/confirm")
                .with(jwt().jwt { it.subject("1").claim("memberId", 1L) })
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "schoolId": 1,
                      "email": "test@snu.ac.kr",
                      "code": "123456"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data.schoolId").value(1))
            .andExpect(jsonPath("$.data.status").value("VERIFIED"))

        then(schoolEmailVerificationUseCase).should().confirm(1L, request)
        then(schoolEmailVerificationUseCase).shouldHaveNoMoreInteractions()
    }

    @Test
    @DisplayName("인증 요청 값이 유효하지 않으면 400을 반환한다")
    fun requestWithInvalidRequest() {
        mockMvc.perform(
            post("/api/v1/school-email-verifications/request")
                .with(jwt().jwt { it.subject("1").claim("memberId", 1L) })
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "schoolId": null,
                      "email": "invalid-email"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fieldErrors").isArray)

        then(schoolEmailVerificationUseCase).shouldHaveNoInteractions()
    }

    @Test
    @DisplayName("학교 이메일 도메인이 아니면 400을 반환한다")
    fun requestWithNotAllowedDomain() {
        val request = SchoolEmailVerificationRequest(
            schoolId = 1L,
            email = "test@gmail.com",
        )

        given(schoolEmailVerificationUseCase.request(1L, request))
            .willThrow(BusinessException(MemberErrorCode.SCHOOL_EMAIL_DOMAIN_NOT_ALLOWED))

        mockMvc.perform(
            post("/api/v1/school-email-verifications/request")
                .with(jwt().jwt { it.subject("1").claim("memberId", 1L) })
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "schoolId": 1,
                      "email": "test@gmail.com"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("SCHOOL_EMAIL_DOMAIN_NOT_ALLOWED"))

        then(schoolEmailVerificationUseCase).should().request(1L, request)
        then(schoolEmailVerificationUseCase).shouldHaveNoMoreInteractions()
    }

    @Test
    @DisplayName("인증 없이 요청하면 401을 반환한다")
    fun requestWithoutAuthentication() {
        mockMvc.perform(
            post("/api/v1/school-email-verifications/request")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "schoolId": 1,
                      "email": "test@snu.ac.kr"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"))

        then(schoolEmailVerificationUseCase).shouldHaveNoInteractions()
    }
}