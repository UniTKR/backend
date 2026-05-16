package com.unit.member.controller

import com.unit.member.dto.MemberAvailabilityResponse
import com.unit.member.dto.MemberMeResponse
import com.unit.member.dto.MemberProfileUpdateRequest
import com.unit.member.dto.MemberProfileUpdateResponse
import com.unit.member.dto.MemberSchoolResponse
import com.unit.member.dto.MemberSignupRequest
import com.unit.member.dto.MemberSignupResponse
import com.unit.member.enums.MemberStatus
import com.unit.member.enums.UserSchoolVerificationStatus
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import kotlin.test.Test

@WebMvcTest(MemberController::class)
@AutoConfigureMockMvc
@Import(
    GlobalExceptionHandler::class,
    SecurityConfig::class,
    JsonAuthenticationEntryPoint::class,
    JsonAccessDeniedHandler::class,
)
@DisplayName("MemberController 테스트")
class MemberControllerTest @Autowired constructor(
    private val mockMvc: MockMvc
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
    @DisplayName("회원가입에 성공하면 201 Created를 반환한다")
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
        }

        then(memberSignupUseCase).should().signup(request)
        then(memberSignupUseCase).shouldHaveNoMoreInteractions()
    }

    @Test
    @DisplayName("이메일 사용 가능 여부 조회에 성공하면 200 OK를 반환한다")
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
        }

        then(memberAvailabilityUseCase).should().checkEmailAvailability("test@unit.com")
        then(memberAvailabilityUseCase).shouldHaveNoMoreInteractions()
    }

    @Test
    @DisplayName("이메일 형식이 올바르지 않으면 400을 반환한다")
    fun checkEmailAvailabilityWithInvalidEmail() {
        mockMvc.get("/api/v1/members/email-availability") {
            param("email", "invalid-email")
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isBadRequest() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.code") { value("VALIDATION_FAILED") }
            jsonPath("$.fieldErrors") { isArray() }
        }

        then(memberAvailabilityUseCase).shouldHaveNoInteractions()
    }

    @Test
    @DisplayName("닉네임 사용 가능 여부 조회에 성공하면 200 OK를 반환한다")
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
        }

        then(memberAvailabilityUseCase).should().checkNicknameAvailability("unit_user")
        then(memberAvailabilityUseCase).shouldHaveNoMoreInteractions()
    }

    @Test
    @DisplayName("닉네임 형식이 올바르지 않으면 400을 반환한다")
    fun checkNicknameAvailabilityWithInvalidNickname() {
        mockMvc.get("/api/v1/members/nickname-availability") {
            param("nickname", "!")
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isBadRequest() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.code") { value("VALIDATION_FAILED") }
            jsonPath("$.fieldErrors") { isArray() }
        }

        then(memberAvailabilityUseCase).shouldHaveNoInteractions()
    }

    @Test
    @DisplayName("회원가입 요청 값이 유효하지 않으면 400을 반환한다")
    fun signupWithInvalidRequest() {
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
            jsonPath("$.fieldErrors") { isArray() }
        }

        then(memberSignupUseCase).shouldHaveNoInteractions()
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 409를 반환한다")
    fun signupWithDuplicatedEmail() {
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
        }

        then(memberSignupUseCase).should().signup(request)
        then(memberSignupUseCase).shouldHaveNoMoreInteractions()
    }

    @ParameterizedTest(name = "[{index}] password={0}")
    @ValueSource(
        strings = [
            "password123",
            "password!",
            "12345678!",
            "pass 123!",
            "pass1!",
            "abc123가나다라마"
        ],
    )
    @DisplayName("비밀번호 정책을 만족하지 않으면 400을 반환한다")
    fun signupWithInvalidPassword(password: String) {
        mockMvc.post("/api/v1/members/signup") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = """
            {
              "email": "test@unit.com",
              "password": "$password",
              "nickname": "unit_user",
              "schoolId": 1,
              "termsAgreed": true,
              "privacyPolicyAgreed": true
            }
        """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.code") { value("VALIDATION_FAILED") }
            jsonPath("$.fieldErrors") { isArray() }
        }

        then(memberSignupUseCase).shouldHaveNoInteractions()
    }

    @Test
    @DisplayName("내 정보 조회에 성공하면 200 OK를 반환한다")
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
            )
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
        }

        then(memberQueryUseCase).should().getMe(1L)
        then(memberQueryUseCase).shouldHaveNoMoreInteractions()
    }

    @Test
    @DisplayName("내 정보 조회 시 학교 정보가 없으면 school은 null이다")
    fun getMeWithoutSchool() {
        given(memberQueryUseCase.getMe(1L)).willReturn(
            MemberMeResponse(
                memberId = 1L,
                email = "test@unit.com",
                nickname = "unit_user",
                profileImageUrl = null,
                status = MemberStatus.PENDING,
                trustScore = 0,
                school = null,
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
            jsonPath("$.data.profileImageUrl") { doesNotExist() }
            jsonPath("$.data.status") { value("PENDING") }
            jsonPath("$.data.trustScore") { value(0) }
            jsonPath("$.data.school") { doesNotExist() }
        }

        then(memberQueryUseCase).should().getMe(1L)
        then(memberQueryUseCase).shouldHaveNoMoreInteractions()
    }

    @Test
    @DisplayName("회원 탈퇴에 성공하면 200 OK를 반환한다")
    fun withdraw() {
        mockMvc.delete("/api/v1/members/me") {
            header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.code") { value("OK") }
            jsonPath("$.data") { doesNotExist() }
        }

        then(memberWithdrawalUseCase).should().withdraw(1L)
        then(memberWithdrawalUseCase).shouldHaveNoMoreInteractions()
    }

    @Test
    @DisplayName("내 프로필 수정에 성공하면 200 OK를 반환한다")
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
        }

        then(memberProfileUseCase).should().updateProfile(1L, request)
        then(memberProfileUseCase).shouldHaveNoMoreInteractions()
    }

    @Test
    @DisplayName("내 프로필 수정 요청값이 유효하지 않으면 400을 반환한다")
    fun updateMyProfileWithInvalidRequest() {
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
            jsonPath("$.fieldErrors") { isArray() }
        }

        then(memberProfileUseCase).shouldHaveNoInteractions()
    }

    @Test
    @DisplayName("내 프로필 수정 시 닉네임이 중복되면 409를 반환한다")
    fun updateMyProfileWithDuplicatedNickname() {
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
        }

        then(memberProfileUseCase).should().updateProfile(1L, request)
        then(memberProfileUseCase).shouldHaveNoMoreInteractions()
    }

}
