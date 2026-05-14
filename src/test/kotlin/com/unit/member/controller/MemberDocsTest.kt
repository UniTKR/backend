package com.unit.member.controller

import com.unit.member.dto.MemberAvailabilityResponse
import com.unit.member.dto.MemberMeResponse
import com.unit.member.dto.MemberSchoolResponse
import com.unit.member.dto.MemberSignupRequest
import com.unit.member.dto.MemberSignupResponse
import com.unit.member.enums.MemberStatus
import com.unit.member.enums.UserSchoolVerificationStatus
import com.unit.member.exception.MemberErrorCode
import com.unit.member.service.MemberAvailabilityUseCase
import com.unit.member.service.MemberQueryUseCase
import com.unit.member.service.MemberSignupUseCase
import com.unit.member.service.MemberWithdrawalUseCase
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
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@WebMvcTest(MemberController::class)
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Import(
    GlobalExceptionHandler::class,
    SecurityConfig::class,
    JsonAuthenticationEntryPoint::class,
    JsonAccessDeniedHandler::class,
)
@DisplayName("회원 API 문서화 테스트")
class MemberDocsTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @MockitoBean
    private lateinit var memberSignupUseCase: MemberSignupUseCase

    @MockitoBean
    private lateinit var memberQueryUseCase: MemberQueryUseCase

    @MockitoBean
    private lateinit var memberWithdrawalUseCase: MemberWithdrawalUseCase

    @MockitoBean
    private lateinit var memberAvailabilityUseCase: MemberAvailabilityUseCase

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
    @DisplayName("회원가입 API를 문서화한다")
    fun signup() {
        val request = MemberSignupRequest(
            email = "test@unit.com",
            password = "password123!",
            nickname = "unit_user",
            schoolId = 1L,
            termsAgreed = true,
            privacyPolicyAgreed = true,
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
                  "schoolId": 1,
                  "termsAgreed": true,
                  "privacyPolicyAgreed": true
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
                            .description("회원 이메일입니다. 서버에서 정규화 후 해시로 저장합니다."),
                        fieldWithPath("password")
                            .type(JsonFieldType.STRING)
                            .description("회원 비밀번호입니다. 8자 이상 72자 이하이며 영문, 숫자, 특수문자를 포함해야 합니다."),
                        fieldWithPath("nickname")
                            .type(JsonFieldType.STRING)
                            .description("서비스에서 사용할 닉네임입니다. 앞뒤 공백은 제거해서 저장합니다."),
                        fieldWithPath("schoolId")
                            .type(JsonFieldType.NUMBER)
                            .description("사용자가 선택한 학교 ID입니다."),
                        fieldWithPath("termsAgreed")
                            .type(JsonFieldType.BOOLEAN)
                            .description("이용약관 필수 동의 여부입니다. true여야 회원가입할 수 있습니다."),
                        fieldWithPath("privacyPolicyAgreed")
                            .type(JsonFieldType.BOOLEAN)
                            .description("개인정보 처리방침 필수 동의 여부입니다. true여야 회원가입할 수 있습니다."),
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
                            .description("회원 상태입니다. 학교 이메일 인증 전에는 PENDING입니다."),
                        fieldWithPath("data.schoolVerificationStatus")
                            .type(JsonFieldType.STRING)
                            .description("학교 인증 상태입니다. 회원가입 직후에는 PENDING입니다."),
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
                  "schoolId": null,
                  "termsAgreed": false,
                  "privacyPolicyAgreed": false
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
                        fieldWithPath("termsAgreed")
                            .type(JsonFieldType.BOOLEAN)
                            .description("검증 실패 예시 이용약관 동의 여부"),
                        fieldWithPath("privacyPolicyAgreed")
                            .type(JsonFieldType.BOOLEAN)
                            .description("검증 실패 예시 개인정보 처리방침 동의 여부"),
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
            termsAgreed = true,
            privacyPolicyAgreed = true,
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
                  "schoolId": 1,
                  "termsAgreed": true,
                  "privacyPolicyAgreed": true
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

    @Test
    @DisplayName("이메일 사용 가능 여부 조회 API를 문서화한다")
    fun checkEmailAvailability() {
        given(memberAvailabilityUseCase.checkEmailAvailability("test@unit.com"))
            .willReturn(MemberAvailabilityResponse(available = true))

        mockMvc.get("/api/v1/members/email-availability") {
            param("email", "test@unit.com")
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.code") { value("OK") }
            jsonPath("$.data.available") { value(true) }
        }.andDo {
            handle(
                document(
                    "member/email-availability",
                    queryParameters(
                        parameterWithName("email")
                            .description("사용 가능 여부를 확인할 이메일입니다. 서버에서 정규화 후 해시로 비교합니다."),
                    ),
                    responseFields(
                        fieldWithPath("code")
                            .type(JsonFieldType.STRING)
                            .description("애플리케이션 응답 코드"),
                        fieldWithPath("data")
                            .type(JsonFieldType.OBJECT)
                            .description("이메일 사용 가능 여부 조회 결과"),
                        fieldWithPath("data.available")
                            .type(JsonFieldType.BOOLEAN)
                            .description("true이면 가입에 사용할 수 있고, false이면 이미 사용 중입니다."),
                    ),
                ),
            )
        }
    }

    @Test
    @DisplayName("닉네임 사용 가능 여부 조회 API를 문서화한다")
    fun checkNicknameAvailability() {
        given(memberAvailabilityUseCase.checkNicknameAvailability("unit_user"))
            .willReturn(MemberAvailabilityResponse(available = false))

        mockMvc.get("/api/v1/members/nickname-availability") {
            param("nickname", "unit_user")
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.code") { value("OK") }
            jsonPath("$.data.available") { value(false) }
        }.andDo {
            handle(
                document(
                    "member/nickname-availability",
                    queryParameters(
                        parameterWithName("nickname")
                            .description("사용 가능 여부를 확인할 닉네임입니다. 앞뒤 공백은 제거한 뒤 비교합니다."),
                    ),
                    responseFields(
                        fieldWithPath("code")
                            .type(JsonFieldType.STRING)
                            .description("애플리케이션 응답 코드"),
                        fieldWithPath("data")
                            .type(JsonFieldType.OBJECT)
                            .description("닉네임 사용 가능 여부 조회 결과"),
                        fieldWithPath("data.available")
                            .type(JsonFieldType.BOOLEAN)
                            .description("true이면 가입에 사용할 수 있고, false이면 이미 사용 중입니다."),
                    ),
                ),
            )
        }
    }

    @Test
    @DisplayName("내 정보 조회 API를 문서화한다")
    fun getMe() {
        given(memberQueryUseCase.getMe(1L)).willReturn(
            MemberMeResponse(
                memberId = 1L,
                email = "test@unit.com",
                nickname = "unit_user",
                profileImageUrl = "profile_image_url",
                status = MemberStatus.ACTIVE,
                trustScore = 100,
                school = MemberSchoolResponse(
                    schoolId = 1L,
                    name = "Unit_University",
                    verificationStatus = UserSchoolVerificationStatus.VERIFIED,
                    verifiedEmail = "test@snu.ac.kr",
                ),
            ),
        )

        mockMvc.get("/api/v1/members/me") {
            header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.code") { value("OK") }
            jsonPath("$.data.memberId") { value(1) }
            jsonPath("$.data.email") { value("test@unit.com") }
            jsonPath("$.data.nickname") { value("unit_user") }
            jsonPath("$.data.profileImageUrl") { value("profile_image_url") }
            jsonPath("$.data.status") { value("ACTIVE") }
            jsonPath("$.data.trustScore") { value(100) }
            jsonPath("$.data.school.schoolId") { value(1) }
            jsonPath("$.data.school.name") { value("Unit_University") }
            jsonPath("$.data.school.verificationStatus") { value("VERIFIED") }
            jsonPath("$.data.school.verifiedEmail") { value("test@snu.ac.kr") }
        }.andDo {
            handle(
                document(
                    "member/profile-get",
                    requestHeaders(
                        headerWithName(HttpHeaders.AUTHORIZATION)
                            .description("Bearer Access Token"),
                    ),
                    responseFields(
                        fieldWithPath("code")
                            .type(JsonFieldType.STRING)
                            .description("애플리케이션 응답 코드"),
                        fieldWithPath("data")
                            .type(JsonFieldType.OBJECT)
                            .description("내 정보 조회 결과"),
                        fieldWithPath("data.memberId")
                            .type(JsonFieldType.NUMBER)
                            .description("회원 ID"),
                        fieldWithPath("data.nickname")
                            .type(JsonFieldType.STRING)
                            .description("회원 닉네임"),
                        fieldWithPath("data.profileImageUrl")
                            .type(JsonFieldType.STRING)
                            .optional()
                            .description("프로필 이미지 URL. 값이 없으면 응답에서 생략합니다."),
                        fieldWithPath("data.status")
                            .type(JsonFieldType.STRING)
                            .description("회원 상태"),
                        fieldWithPath("data.trustScore")
                            .type(JsonFieldType.NUMBER)
                            .description("회원 신뢰 점수"),
                        fieldWithPath("data.email")
                            .type(JsonFieldType.STRING)
                            .optional()
                            .description("회원 이메일"),
                        fieldWithPath("data.school")
                            .type(JsonFieldType.OBJECT)
                            .optional()
                            .description("학교 인증 정보. 인증 정보가 없으면 응답에서 생략합니다."),
                        fieldWithPath("data.school.schoolId")
                            .type(JsonFieldType.NUMBER)
                            .optional()
                            .description("인증된 학교 ID"),
                        fieldWithPath("data.school.name")
                            .type(JsonFieldType.STRING)
                            .optional()
                            .description("인증된 학교 이름"),
                        fieldWithPath("data.school.verifiedEmail")
                            .type(JsonFieldType.STRING)
                            .optional()
                            .description("인증된 학교 이메일"),
                        fieldWithPath("data.school.verificationStatus")
                            .type(JsonFieldType.STRING)
                            .optional()
                            .description("학교 인증 상태"),
                    ),
                ),
            )
        }
    }

    @Test
    @DisplayName("회원 탈퇴 API를 문서화한다")
    fun withdraw() {
        mockMvc.delete("/api/v1/members/me") {
            header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.code") { value("OK") }
            jsonPath("$.data") { doesNotExist() }
        }.andDo {
            handle(
                document(
                    "member/withdraw",
                    requestHeaders(
                        headerWithName(HttpHeaders.AUTHORIZATION)
                            .description("Bearer Access Token"),
                    ),
                    responseFields(
                        fieldWithPath("code")
                            .type(JsonFieldType.STRING)
                            .description("애플리케이션 응답 코드"),
                    ),
                ),
            )
        }
    }
}
