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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import kotlin.test.Test

@WebMvcTest(MemberController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler::class)
@DisplayName("MemberController 테스트")
class MemberControllerTest @Autowired constructor(
    private val mockMvc: MockMvc
) {

    @MockitoBean
    private lateinit var memberSignupUseCase: MemberSignupUseCase

    @Test
    @DisplayName("회원가입에 성공하면 201 Created를 반환한다")
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
        }

        then(memberSignupUseCase).should().signup(request)
        then(memberSignupUseCase).shouldHaveNoMoreInteractions()
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
                  "schoolId": null
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
              "schoolId": 1
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



}