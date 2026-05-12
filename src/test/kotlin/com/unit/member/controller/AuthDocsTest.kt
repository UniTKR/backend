package com.unit.member.controller

import com.unit.member.dto.AuthLoginRequest
import com.unit.member.dto.AuthLoginResponse
import com.unit.member.dto.AuthenticatedMemberResponse
import com.unit.member.enums.MemberStatus
import com.unit.member.exception.MemberErrorCode
import com.unit.member.service.AuthLoginUseCase
import com.unit.member.service.RefreshTokenUseCase
import com.unit.platform.error.BusinessException
import com.unit.platform.error.GlobalExceptionHandler
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@WebMvcTest(AuthController::class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
@Import(GlobalExceptionHandler::class)
@DisplayName("인증 API 문서화 테스트")
class AuthDocsTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockitoBean
    private lateinit var authLoginUseCase: AuthLoginUseCase

    @MockitoBean
    private lateinit var refreshTokenUseCase: RefreshTokenUseCase

    @Test
    @DisplayName("로그인 API를 문서화한다")
    fun login() {
        val request = AuthLoginRequest(
            email = "test@unit.com",
            password = "password123!",
        )

        given(authLoginUseCase.login(request)).willReturn(
            AuthLoginResponse(
                accessToken = "access-token",
                refreshToken = "refresh-token",
                tokenType = "Bearer",
                expiresIn = 1800L,
                member = AuthenticatedMemberResponse(
                    memberId = 1L,
                    nickname = "unit_user",
                    status = MemberStatus.ACTIVE,
                ),
            ),
        )

        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = """
                {
                  "email": "test@unit.com",
                  "password": "password123!"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.code") { value("OK") }
            jsonPath("$.data.accessToken") { value("access-token") }
            jsonPath("$.data.refreshToken") { value("refresh-token") }
            jsonPath("$.data.tokenType") { value("Bearer") }
            jsonPath("$.data.expiresIn") { value(1800) }
            jsonPath("$.data.member.memberId") { value(1) }
            jsonPath("$.data.member.nickname") { value("unit_user") }
            jsonPath("$.data.member.status") { value("ACTIVE") }
        }.andDo {
            handle(
                document(
                    "member/login",
                    requestFields(
                        fieldWithPath("email")
                            .type(JsonFieldType.STRING)
                            .description("로그인 이메일"),
                        fieldWithPath("password")
                            .type(JsonFieldType.STRING)
                            .description("로그인 비밀번호"),
                    ),
                    responseFields(
                        fieldWithPath("code")
                            .type(JsonFieldType.STRING)
                            .description("애플리케이션 응답 코드"),
                        fieldWithPath("data")
                            .type(JsonFieldType.OBJECT)
                            .description("로그인 결과"),
                        fieldWithPath("data.accessToken")
                            .type(JsonFieldType.STRING)
                            .description("API 인증에 사용할 JWT Access Token"),
                        fieldWithPath("data.refreshToken")
                            .type(JsonFieldType.STRING)
                            .description("Refresh Token"),
                        fieldWithPath("data.tokenType")
                            .type(JsonFieldType.STRING)
                            .description("토큰 타입. 현재는 Bearer입니다."),
                        fieldWithPath("data.expiresIn")
                            .type(JsonFieldType.NUMBER)
                            .description("Access Token 만료 시간. 단위는 초입니다."),
                        fieldWithPath("data.member")
                            .type(JsonFieldType.OBJECT)
                            .description("로그인한 회원 정보"),
                        fieldWithPath("data.member.memberId")
                            .type(JsonFieldType.NUMBER)
                            .description("회원 ID"),
                        fieldWithPath("data.member.nickname")
                            .type(JsonFieldType.STRING)
                            .description("회원 닉네임"),
                        fieldWithPath("data.member.status")
                            .type(JsonFieldType.STRING)
                            .description("회원 상태"),
                    ),
                ),
            )
        }
    }

    @Test
    @DisplayName("로그인 검증 실패 응답을 문서화한다")
    fun loginValidationFailed() {
        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = """
                {
                  "email": "invalid-email",
                  "password": ""
                }
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.code") { value("VALIDATION_FAILED") }
            jsonPath("$.traceId") { exists() }
            jsonPath("$.fieldErrors") { isArray() }
        }.andDo {
            handle(
                document(
                    "member/login/validation-failed",
                    requestFields(
                        fieldWithPath("email")
                            .type(JsonFieldType.STRING)
                            .description("검증 실패 예시 이메일"),
                        fieldWithPath("password")
                            .type(JsonFieldType.STRING)
                            .description("검증 실패 예시 비밀번호"),
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
    @DisplayName("로그인 인증 실패 응답을 문서화한다")
    fun loginInvalidCredentials() {
        val request = AuthLoginRequest(
            email = "test@unit.com",
            password = "wrong-password",
        )

        given(authLoginUseCase.login(request))
            .willThrow(BusinessException(MemberErrorCode.INVALID_LOGIN_CREDENTIALS))

        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = """
                {
                  "email": "test@unit.com",
                  "password": "wrong-password"
                }
            """.trimIndent()
        }.andExpect {
            status { isUnauthorized() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.code") { value("AUTH_INVALID_CREDENTIALS") }
            jsonPath("$.traceId") { exists() }
        }.andDo {
            handle(
                document(
                    "member/login/invalid-credentials",
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
    @DisplayName("로그인 금지 회원 상태 응답을 문서화한다")
    fun loginForbiddenMemberStatus() {
        val request = AuthLoginRequest(
            email = "test@unit.com",
            password = "password123!",
        )

        given(authLoginUseCase.login(request))
            .willThrow(BusinessException(MemberErrorCode.MEMBER_LOGIN_FORBIDDEN))

        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = """
                {
                  "email": "test@unit.com",
                  "password": "password123!"
                }
            """.trimIndent()
        }.andExpect {
            status { isForbidden() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.code") { value("MEMBER_LOGIN_FORBIDDEN") }
            jsonPath("$.traceId") { exists() }
        }.andDo {
            handle(
                document(
                    "member/login/forbidden",
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
                            .description("필드 검증 실패 목록. 로그인 금지 응답에서는 내려가지 않습니다."),
                    ),
                ),
            )
        }
    }
}
