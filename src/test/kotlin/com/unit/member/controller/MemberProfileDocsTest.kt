package com.unit.member.controller

import com.unit.member.dto.MemberProfileUpdateRequest
import com.unit.member.dto.MemberProfileUpdateResponse
import com.unit.member.exception.MemberErrorCode
import com.unit.member.service.MemberAvailabilityUseCase
import com.unit.member.service.MemberProfileUseCase
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
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.put

@WebMvcTest(MemberController::class)
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Import(
    GlobalExceptionHandler::class,
    SecurityConfig::class,
    JsonAuthenticationEntryPoint::class,
    JsonAccessDeniedHandler::class,
)
@DisplayName("회원 프로필 수정 API 문서화 테스트")
class MemberProfileDocsTest @Autowired constructor(
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
    private lateinit var memberProfileUseCase: MemberProfileUseCase

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
    @DisplayName("내 프로필 수정 API를 문서화한다")
    fun updateMyProfile() {
        val request = MemberProfileUpdateRequest(
            nickname = "new_nickname",
            profileImageUrl = "https://image.unit/profile.png",
        )

        given(memberProfileUseCase.updateProfile(1L, request)).willReturn(
            MemberProfileUpdateResponse(
                memberId = 1L,
                nickname = "new_nickname",
                profileImageUrl = "https://image.unit/profile.png",
            ),
        )

        mockMvc.put("/api/v1/members/me/profile") {
            header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = """
                {
                  "nickname": "new_nickname",
                  "profileImageUrl": "https://image.unit/profile.png"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.code") { value("OK") }
            jsonPath("$.data.memberId") { value(1) }
            jsonPath("$.data.nickname") { value("new_nickname") }
            jsonPath("$.data.profileImageUrl") { value("https://image.unit/profile.png") }
        }.andDo {
            handle(
                document(
                    "member/profile-update",
                    requestHeaders(
                        headerWithName(HttpHeaders.AUTHORIZATION)
                            .description("Bearer Access Token"),
                    ),
                    requestFields(
                        fieldWithPath("nickname")
                            .type(JsonFieldType.STRING)
                            .description("변경할 닉네임입니다. 앞뒤 공백은 제거되어 저장됩니다."),
                        fieldWithPath("profileImageUrl")
                            .type(JsonFieldType.STRING)
                            .optional()
                            .description("변경할 프로필 이미지 URL입니다. null 또는 빈 문자열이면 프로필 이미지가 제거됩니다."),
                    ),
                    responseFields(
                        fieldWithPath("code")
                            .type(JsonFieldType.STRING)
                            .description("애플리케이션 응답 코드"),
                        fieldWithPath("data")
                            .type(JsonFieldType.OBJECT)
                            .description("프로필 수정 결과"),
                        fieldWithPath("data.memberId")
                            .type(JsonFieldType.NUMBER)
                            .description("회원 ID"),
                        fieldWithPath("data.nickname")
                            .type(JsonFieldType.STRING)
                            .description("수정된 닉네임"),
                        fieldWithPath("data.profileImageUrl")
                            .type(JsonFieldType.STRING)
                            .optional()
                            .description("수정된 프로필 이미지 URL. 값이 없으면 응답에서 생략됩니다."),
                    ),
                ),
            )
        }
    }

    @Test
    @DisplayName("내 프로필 수정 검증 실패 응답을 문서화한다")
    fun updateMyProfileValidationFailed() {
        mockMvc.put("/api/v1/members/me/profile") {
            header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = """
                {
                  "nickname": "!",
                  "profileImageUrl": "https://image.unit/profile.png"
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
                    "member/profile-update/validation-failed",
                    requestHeaders(
                        headerWithName(HttpHeaders.AUTHORIZATION)
                            .description("Bearer Access Token"),
                    ),
                    requestFields(
                        fieldWithPath("nickname")
                            .type(JsonFieldType.STRING)
                            .description("검증 실패 예시 닉네임"),
                        fieldWithPath("profileImageUrl")
                            .type(JsonFieldType.STRING)
                            .optional()
                            .description("검증 실패 예시 프로필 이미지 URL"),
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
    @DisplayName("내 프로필 수정 닉네임 중복 응답을 문서화한다")
    fun updateMyProfileDuplicatedNickname() {
        val request = MemberProfileUpdateRequest(
            nickname = "duplicated",
            profileImageUrl = "https://image.unit/profile.png",
        )

        given(memberProfileUseCase.updateProfile(1L, request))
            .willThrow(BusinessException(MemberErrorCode.NICKNAME_ALREADY_EXISTS))

        mockMvc.put("/api/v1/members/me/profile") {
            header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = """
                {
                  "nickname": "duplicated",
                  "profileImageUrl": "https://image.unit/profile.png"
                }
            """.trimIndent()
        }.andExpect {
            status { isConflict() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.code") { value("MEMBER_NICKNAME_ALREADY_EXISTS") }
            jsonPath("$.traceId") { exists() }
        }.andDo {
            handle(
                document(
                    "member/profile-update/nickname-duplicated",
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
                            .description("필드 검증 실패 목록. 닉네임 중복 응답에서는 내려가지 않습니다."),
                    ),
                ),
            )
        }
    }
}
