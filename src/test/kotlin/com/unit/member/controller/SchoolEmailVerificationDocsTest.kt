package com.unit.member.controller

import com.unit.member.dto.SchoolAuthDto.SchoolEmailVerificationConfirmRequest
import com.unit.member.dto.SchoolAuthDto.SchoolEmailVerificationConfirmResponse
import com.unit.member.dto.SchoolAuthDto.SchoolEmailVerificationRequest
import com.unit.member.dto.SchoolAuthDto.SchoolEmailVerificationResponse
import com.unit.member.enums.UserSchoolVerificationStatus
import com.unit.member.exception.MemberErrorCode
import com.unit.member.service.SchoolEmailVerificationUseCase
import com.unit.platform.error.BusinessException
import com.unit.platform.error.GlobalExceptionHandler
import com.unit.platform.security.JsonAccessDeniedHandler
import com.unit.platform.security.JsonAuthenticationEntryPoint
import com.unit.platform.security.SecurityConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(SchoolEmailVerificationController::class)
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Import(
    GlobalExceptionHandler::class,
    SecurityConfig::class,
    JsonAuthenticationEntryPoint::class,
    JsonAccessDeniedHandler::class,
)
@DisplayName("학교 이메일 인증 API 문서화 테스트")
class SchoolEmailVerificationDocsTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockitoBean
    private lateinit var schoolEmailVerificationUseCase: SchoolEmailVerificationUseCase

    @MockitoBean
    private lateinit var jwtDecoder: JwtDecoder

    @BeforeEach
    fun setUp() {
        given(jwtDecoder.decode("access-token")).willReturn(
            Jwt.withTokenValue("access-token")
                .header("alg", "none")
                .subject("1")
                .claim("memberId", 1L)
                .build(),
        )
    }

    @Test
    @DisplayName("학교 이메일 인증 요청 API를 문서화한다")
    fun requestVerification() {
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
                .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
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
            .andDo(
                document(
                    "member/school-email-verifications/request",
                    requestHeaders(
                        headerWithName(HttpHeaders.AUTHORIZATION)
                            .description("Bearer Access Token"),
                    ),
                    requestFields(
                        fieldWithPath("schoolId")
                            .type(JsonFieldType.NUMBER)
                            .description("인증할 학교 ID"),
                        fieldWithPath("email")
                            .type(JsonFieldType.STRING)
                            .description("인증 코드를 받을 학교 이메일"),
                    ),
                    responseFields(
                        fieldWithPath("code")
                            .type(JsonFieldType.STRING)
                            .description("애플리케이션 응답 코드"),
                        fieldWithPath("data")
                            .type(JsonFieldType.OBJECT)
                            .description("학교 이메일 인증 요청 결과"),
                        fieldWithPath("data.schoolId")
                            .type(JsonFieldType.NUMBER)
                            .description("인증 대상 학교 ID"),
                        fieldWithPath("data.email")
                            .type(JsonFieldType.STRING)
                            .description("정규화된 학교 이메일"),
                        fieldWithPath("data.expiresIn")
                            .type(JsonFieldType.NUMBER)
                            .description("인증 코드 만료 시간. 단위는 초입니다."),
                    ),
                ),
            )
    }

    @Test
    @DisplayName("학교 이메일 인증 요청 검증 실패 응답을 문서화한다")
    fun requestVerificationValidationFailed() {
        mockMvc.perform(
            post("/api/v1/school-email-verifications/request")
                .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
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
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.traceId").exists())
            .andExpect(jsonPath("$.fieldErrors").isArray)
            .andDo(
                document(
                    "member/school-email-verifications/request/validation-failed",
                    requestHeaders(
                        headerWithName(HttpHeaders.AUTHORIZATION)
                            .description("Bearer Access Token"),
                    ),
                    requestFields(
                        fieldWithPath("schoolId")
                            .type(JsonFieldType.NULL)
                            .description("검증 실패 예시 학교 ID"),
                        fieldWithPath("email")
                            .type(JsonFieldType.STRING)
                            .description("검증 실패 예시 이메일"),
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

    @Test
    @DisplayName("학교 이메일 도메인 불일치 응답을 문서화한다")
    fun requestVerificationDomainNotAllowed() {
        val request = SchoolEmailVerificationRequest(
            schoolId = 1L,
            email = "test@gmail.com",
        )

        given(schoolEmailVerificationUseCase.request(1L, request))
            .willThrow(BusinessException(MemberErrorCode.SCHOOL_EMAIL_DOMAIN_NOT_ALLOWED))

        mockMvc.perform(
            post("/api/v1/school-email-verifications/request")
                .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
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
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("SCHOOL_EMAIL_DOMAIN_NOT_ALLOWED"))
            .andExpect(jsonPath("$.traceId").exists())
            .andDo(
                document(
                    "member/school-email-verifications/request/domain-not-allowed",
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
                            .description("필드 검증 실패 목록. 도메인 불일치 응답에서는 내려가지 않습니다."),
                    ),
                ),
            )
    }

    @Test
    @DisplayName("학교 이메일 인증 요청 학교 없음 응답을 문서화한다")
    fun requestVerificationSchoolNotFound() {
        val request = SchoolEmailVerificationRequest(
            schoolId = 999L,
            email = "test@snu.ac.kr",
        )

        given(schoolEmailVerificationUseCase.request(1L, request))
            .willThrow(BusinessException(MemberErrorCode.SCHOOL_NOT_FOUND))

        mockMvc.perform(
            post("/api/v1/school-email-verifications/request")
                .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "schoolId": 999,
                      "email": "test@snu.ac.kr"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isNotFound)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("SCHOOL_NOT_FOUND"))
            .andExpect(jsonPath("$.traceId").exists())
            .andDo(
                document(
                    "member/school-email-verifications/request/school-not-found",
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
                            .description("필드 검증 실패 목록. 학교 없음 응답에서는 내려가지 않습니다."),
                    ),
                ),
            )
    }

    @Test
    @DisplayName("학교 이메일 인증 확인 API를 문서화한다")
    fun confirmVerification() {
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
                .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
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
            .andDo(
                document(
                    "member/school-email-verifications/confirm",
                    requestHeaders(
                        headerWithName(HttpHeaders.AUTHORIZATION)
                            .description("Bearer Access Token"),
                    ),
                    requestFields(
                        fieldWithPath("schoolId")
                            .type(JsonFieldType.NUMBER)
                            .description("인증할 학교 ID"),
                        fieldWithPath("email")
                            .type(JsonFieldType.STRING)
                            .description("인증 코드를 요청했던 학교 이메일"),
                        fieldWithPath("code")
                            .type(JsonFieldType.STRING)
                            .description("이메일로 받은 6자리 인증 코드"),
                    ),
                    responseFields(
                        fieldWithPath("code")
                            .type(JsonFieldType.STRING)
                            .description("애플리케이션 응답 코드"),
                        fieldWithPath("data")
                            .type(JsonFieldType.OBJECT)
                            .description("학교 이메일 인증 확인 결과"),
                        fieldWithPath("data.schoolId")
                            .type(JsonFieldType.NUMBER)
                            .description("인증 완료 학교 ID"),
                        fieldWithPath("data.status")
                            .type(JsonFieldType.STRING)
                            .description("사용자 학교 인증 상태"),
                    ),
                ),
            )
    }

    @Test
    @DisplayName("학교 이메일 인증 확인 검증 실패 응답을 문서화한다")
    fun confirmVerificationValidationFailed() {
        mockMvc.perform(
            post("/api/v1/school-email-verifications/confirm")
                .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "schoolId": 1,
                      "email": "test@snu.ac.kr",
                      "code": "12"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.traceId").exists())
            .andExpect(jsonPath("$.fieldErrors").isArray)
            .andDo(
                document(
                    "member/school-email-verifications/confirm/validation-failed",
                    requestHeaders(
                        headerWithName(HttpHeaders.AUTHORIZATION)
                            .description("Bearer Access Token"),
                    ),
                    requestFields(
                        fieldWithPath("schoolId")
                            .type(JsonFieldType.NUMBER)
                            .description("인증할 학교 ID"),
                        fieldWithPath("email")
                            .type(JsonFieldType.STRING)
                            .description("인증 코드를 요청했던 학교 이메일"),
                        fieldWithPath("code")
                            .type(JsonFieldType.STRING)
                            .description("검증 실패 예시 인증 코드"),
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

    @Test
    @DisplayName("학교 이메일 인증 코드 없음 응답을 문서화한다")
    fun confirmVerificationCodeNotFound() {
        val request = SchoolEmailVerificationConfirmRequest(
            schoolId = 1L,
            email = "test@snu.ac.kr",
            code = "123456",
        )

        given(schoolEmailVerificationUseCase.confirm(1L, request))
            .willThrow(BusinessException(MemberErrorCode.SCHOOL_EMAIL_VERIFICATION_CODE_NOT_FOUND))

        mockMvc.perform(
            post("/api/v1/school-email-verifications/confirm")
                .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
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
            .andExpect(status().isNotFound)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("SCHOOL_EMAIL_VERIFICATION_CODE_NOT_FOUND"))
            .andExpect(jsonPath("$.traceId").exists())
            .andDo(
                document(
                    "member/school-email-verifications/confirm/code-not-found",
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
                            .description("필드 검증 실패 목록. 인증 코드 없음 응답에서는 내려가지 않습니다."),
                    ),
                ),
            )
    }

    @Test
    @DisplayName("학교 이메일 인증 코드 만료 응답을 문서화한다")
    fun confirmVerificationCodeExpired() {
        val request = SchoolEmailVerificationConfirmRequest(
            schoolId = 1L,
            email = "test@snu.ac.kr",
            code = "123456",
        )

        given(schoolEmailVerificationUseCase.confirm(1L, request))
            .willThrow(BusinessException(MemberErrorCode.SCHOOL_EMAIL_VERIFICATION_CODE_EXPIRED))

        mockMvc.perform(
            post("/api/v1/school-email-verifications/confirm")
                .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
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
            .andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("SCHOOL_EMAIL_VERIFICATION_CODE_EXPIRED"))
            .andExpect(jsonPath("$.traceId").exists())
            .andDo(
                document(
                    "member/school-email-verifications/confirm/code-expired",
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
                            .description("필드 검증 실패 목록. 인증 코드 만료 응답에서는 내려가지 않습니다."),
                    ),
                ),
            )
    }

    @Test
    @DisplayName("학교 이메일 인증 코드 불일치 응답을 문서화한다")
    fun confirmVerificationCodeMismatched() {
        val request = SchoolEmailVerificationConfirmRequest(
            schoolId = 1L,
            email = "test@snu.ac.kr",
            code = "000000",
        )

        given(schoolEmailVerificationUseCase.confirm(1L, request))
            .willThrow(BusinessException(MemberErrorCode.SCHOOL_EMAIL_VERIFICATION_CODE_MISMATCHED))

        mockMvc.perform(
            post("/api/v1/school-email-verifications/confirm")
                .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "schoolId": 1,
                      "email": "test@snu.ac.kr",
                      "code": "000000"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("SCHOOL_EMAIL_VERIFICATION_CODE_MISMATCHED"))
            .andExpect(jsonPath("$.traceId").exists())
            .andDo(
                document(
                    "member/school-email-verifications/confirm/code-mismatched",
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
                            .description("필드 검증 실패 목록. 코드 불일치 응답에서는 내려가지 않습니다."),
                    ),
                ),
            )
    }

    @Test
    @DisplayName("학교 인증 정보 없음 응답을 문서화한다")
    fun confirmVerificationSchoolVerificationNotFound() {
        val request = SchoolEmailVerificationConfirmRequest(
            schoolId = 1L,
            email = "test@snu.ac.kr",
            code = "123456",
        )

        given(schoolEmailVerificationUseCase.confirm(1L, request))
            .willThrow(BusinessException(MemberErrorCode.SCHOOL_VERIFICATION_NOT_FOUND))

        mockMvc.perform(
            post("/api/v1/school-email-verifications/confirm")
                .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
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
            .andExpect(status().isNotFound)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("SCHOOL_VERIFICATION_NOT_FOUND"))
            .andExpect(jsonPath("$.traceId").exists())
            .andDo(
                document(
                    "member/school-email-verifications/confirm/school-verification-not-found",
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
                            .description("필드 검증 실패 목록. 학교 인증 정보 없음 응답에서는 내려가지 않습니다."),
                    ),
                ),
            )
    }
}
