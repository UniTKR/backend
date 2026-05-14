package com.unit.member.controller

import com.unit.member.dto.MemberMeResponse
import com.unit.member.dto.MemberSignupRequest
import com.unit.member.dto.MemberSignupResponse
import com.unit.member.service.MemberQueryUseCase
import com.unit.member.service.MemberSignupUseCase
import com.unit.member.service.MemberWithdrawalUseCase
import com.unit.platform.security.memberId
import com.unit.platform.web.response.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/members")
class MemberController(
    private val memberSignupUseCase: MemberSignupUseCase,
    private val memberQueryUseCase: MemberQueryUseCase,
    private val memberWithdrawalUseCase: MemberWithdrawalUseCase,

    ) {

    @PostMapping("/signup")
    fun signup(
        @Valid @RequestBody request: MemberSignupRequest,
    ): ResponseEntity<ApiResponse<MemberSignupResponse>> {
        val response = memberSignupUseCase.signup(request)

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.created(response))
    }

    @GetMapping("/me")
    fun getMyProfile(
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<ApiResponse<MemberMeResponse>> {

        val response = memberQueryUseCase.getMe(jwt.memberId())

        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @DeleteMapping("/me")
    fun withdraw(
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<ApiResponse<Unit>> {
        memberWithdrawalUseCase.withdraw(jwt.memberId())

        return ResponseEntity.ok(ApiResponse.ok())
    }
}