package com.unit.member.controller

import com.unit.member.dto.MemberSignupRequest
import com.unit.member.dto.MemberSignupResponse
import com.unit.member.enums.MemberStatus
import com.unit.member.enums.UserSchoolVerificationStatus
import com.unit.member.exception.MemberErrorCode
import com.unit.member.service.MemberSignupUseCase
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

@WebMvcTest(MemberController::class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
@Import(GlobalExceptionHandler::class)
@DisplayName("회원 API 문서화 테스트")
class MemberDocsTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockitoBean
    private lateinit var memberSignupUseCase: MemberSignupUseCase

    @Test
    @DisplayName("회원가입 API를 문서화한다")
    fun signup() {
        val request = MemberSignupRequest(
            email = "test@unit.com",
            password = "password123!",
            nickname = "unit_user",
            schoolId = 1L,
        )

        given(memberSignupUseCase.signup(request)).willReturn(
            MemberSignupResponse(
                memberId = 1L,
                nickname = "unit_user",
                status = MemberStatus.PENDING,
                schoolVerificationStatus = UserSchoolVerificationStatus.PENDING,
            ),
        )

        mockMvc.post("/api/v1/members/signup") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = """
                {
                  "email": "test@unit.com",
                  "password": "password123!",
                  "nickname": "unit_user",
                  "schoolId": 1
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.code") { value("CREATED") }
            jsonPath("$.data.memberId") { value(1) }
            jsonPath("$.data.nickname") { value("unit_user") }
            jsonPath("$.data.status") { value("PENDING") }
            jsonPath("$.data.schoolVerificationStatus") { value("PENDING") }
        }.andDo {
            handle(
                document(
                    "member/signup",
                    requestFields(
                        fieldWithPath("email")
                            .type(JsonFieldType.STRING)
                            .description("회원 이메일. 자체 로그인 식별자로 사용하며 서버에서는 정규화 후 해시로 저장합니다."),
                        fieldWithPath("password")
                            .type(JsonFieldType.STRING)
                            .description("회원 비밀번호. 8자 이상 72자 이하이며 서버에서는 BCrypt 해시로 저장합니다."),
                        fieldWithPath("nickname")
                            .type(JsonFieldType.STRING)
                            .description("서비스에서 사용할 닉네임. 앞뒤 공백은 제거해서 저장합니다."),
                        fieldWithPath("schoolId")
                            .type(JsonFieldType.NUMBER)
                            .description("사용자가 선택한 학교 ID입니다."),
                    ),
                    responseFields(
                        fieldWithPath("code")
                            .type(JsonFieldType.STRING)
                            .description("애플리케이션 응답 코드"),
                        fieldWithPath("data")
                            .type(JsonFieldType.OBJECT)
                            .description("회원가입 결과"),
                        fieldWithPath("data.memberId")
                            .type(JsonFieldType.NUMBER)
                            .description("생성된 회원 ID"),
                        fieldWithPath("data.nickname")
                            .type(JsonFieldType.STRING)
                            .description("생성된 회원 닉네임"),
                        fieldWithPath("data.status")
                            .type(JsonFieldType.STRING)
                            .description("회원 상태. 학교 이메일 인증 전에는 PENDING입니다."),
                        fieldWithPath("data.schoolVerificationStatus")
                            .type(JsonFieldType.STRING)
                            .description("학교 인증 상태. 회원가입 직후에는 PENDING입니다."),
                    ),
                ),
            )
        }
    }

    @Test
    @DisplayName("회원가입 검증 실패 응답을 문서화한다")
    fun signupValidationFailed() {
        mockMvc.post("/api/v1/members/signup") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = """
                {
                  "email": "invalid-email",
                  "password": "123",
                  "nickname": "",
                  "schoolId": null
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
                    "member/signup/validation-failed",
                    requestFields(
                        fieldWithPath("email")
                            .type(JsonFieldType.STRING)
                            .description("검증 실패 예시 이메일"),
                        fieldWithPath("password")
                            .type(JsonFieldType.STRING)
                            .description("검증 실패 예시 비밀번호"),
                        fieldWithPath("nickname")
                            .type(JsonFieldType.STRING)
                            .description("검증 실패 예시 닉네임"),
                        fieldWithPath("schoolId")
                            .type(JsonFieldType.NULL)
                            .description("검증 실패 예시 학교 ID"),
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
    @DisplayName("회원가입 이메일 중복 응답을 문서화한다")
    fun signupDuplicatedEmail() {
        val request = MemberSignupRequest(
            email = "test@unit.com",
            password = "password123!",
            nickname = "unit_user",
            schoolId = 1L,
        )

        given(memberSignupUseCase.signup(request))
            .willThrow(BusinessException(MemberErrorCode.EMAIL_ALREADY_EXISTS))

        mockMvc.post("/api/v1/members/signup") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = """
                {
                  "email": "test@unit.com",
                  "password": "password123!",
                  "nickname": "unit_user",
                  "schoolId": 1
                }
            """.trimIndent()
        }.andExpect {
            status { isConflict() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.code") { value("MEMBER_EMAIL_ALREADY_EXISTS") }
            jsonPath("$.traceId") { exists() }
        }.andDo {
            handle(
                document(
                    "member/signup/email-duplicated",
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
                            .description("필드 검증 실패 목록. 중복 이메일 응답에서는 내려가지 않습니다."),
                    ),
                ),
            )
        }
    }
}
