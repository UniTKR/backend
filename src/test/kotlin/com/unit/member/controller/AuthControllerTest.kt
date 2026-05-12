package com.unit.member.controller

import com.unit.member.dto.AuthLoginRequest
import com.unit.member.dto.AuthLoginResponse
import com.unit.member.dto.AuthLogoutRequest
import com.unit.member.dto.AuthTokenRefreshRequest
import com.unit.member.dto.AuthTokenRefreshResponse
import com.unit.member.dto.AuthenticatedMemberResponse
import com.unit.member.enums.MemberStatus
import com.unit.member.exception.MemberErrorCode
import com.unit.member.service.AuthLoginUseCase
import com.unit.member.service.RefreshTokenUseCase
import com.unit.platform.error.BusinessException
import com.unit.platform.error.GlobalExceptionHandler
import org.junit.jupiter.api.DisplayName
import org.mockito.BDDMockito
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

@WebMvcTest(AuthController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler::class)
@DisplayName("AuthController 테스트")
class AuthControllerTest @Autowired constructor(
    private val mockMvc: MockMvc
) {

    @MockitoBean
    private lateinit var authLoginUseCase: AuthLoginUseCase

    @MockitoBean
    private lateinit var refreshTokenUseCase: RefreshTokenUseCase

    @Test
    @DisplayName("로그인에 성공하면 Access Token, Refresh Token을 반환한다")
    fun loginSuccess() {
        val request = AuthLoginRequest(
            email = "test@unit.com",
            password = "password123!"
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
            )
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
        }

        then(authLoginUseCase).should().login(request)
        then(authLoginUseCase).shouldHaveNoMoreInteractions()

    }

    @Test
    @DisplayName("로그인 요청 값이 유효하지 않으면 400을 반환한다")
    fun loginWithInvalidRequest() {
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
        }

        then(authLoginUseCase).shouldHaveNoInteractions()
    }

    @Test
    @DisplayName("이메일 또는 비밀번호가 올바르지 않으면 401을 반환한다")
    fun loginWithInvalidCredentials() {
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
        }

        then(authLoginUseCase).should().login(request)
        then(authLoginUseCase).shouldHaveNoMoreInteractions()
    }

    @Test
    @DisplayName("로그인할 수 없는 회원 상태이면 403을 반환한다")
    fun loginWithForbiddenMemberStatus() {
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
        }

        then(authLoginUseCase).should().login(request)
        then(authLoginUseCase).shouldHaveNoMoreInteractions()
    }

    @Test
    @DisplayName("Refresh Token으로 토큰을 재발급한다")
    fun refresh() {
        val request = AuthTokenRefreshRequest(refreshToken = "old-refresh-token")

        given(refreshTokenUseCase.refresh(request)).willReturn(
            AuthTokenRefreshResponse(
                accessToken = "new-access-token",
                refreshToken = "new-refresh-token",
                expiresIn = 1800L,
            ),
        )

        mockMvc.post("/api/v1/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = """
            {
              "refreshToken": "old-refresh-token"
            }
        """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("OK") }
            jsonPath("$.data.accessToken") { value("new-access-token") }
            jsonPath("$.data.refreshToken") { value("new-refresh-token") }
            jsonPath("$.data.tokenType") { value("Bearer") }
            jsonPath("$.data.expiresIn") { value(1800) }
        }

        then(refreshTokenUseCase).should().refresh(request)
        then(refreshTokenUseCase).shouldHaveNoMoreInteractions()
    }

    @Test
    @DisplayName("Refresh Token 요청 값이 유효하지 않으면 400을 반환한다")
    fun refreshWithInvalidRequest() {
        mockMvc.post("/api/v1/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = """
            {
              "refreshToken": ""
            }
        """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("VALIDATION_FAILED") }
            jsonPath("$.traceId") { exists() }
            jsonPath("$.fieldErrors") { isArray() }
        }

        then(refreshTokenUseCase).shouldHaveNoInteractions()
    }

    @Test
    @DisplayName("로그아웃에 성공한다")
    fun logout() {
        val request = AuthLogoutRequest(refreshToken = "refresh-token")

        mockMvc.post("/api/v1/auth/logout") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = """
            {
              "refreshToken": "refresh-token"
            }
        """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value("OK") }
        }

        then(refreshTokenUseCase).should().logout(request)
        then(refreshTokenUseCase).shouldHaveNoMoreInteractions()
    }
}