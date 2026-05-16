package com.unit.member.controller

import com.unit.member.dto.MemberAvailabilityResponse
import com.unit.member.dto.MemberMeResponse
import com.unit.member.dto.MemberProfileUpdateRequest
import com.unit.member.dto.MemberProfileUpdateResponse
import com.unit.member.dto.MemberSignupRequest
import com.unit.member.dto.MemberSignupResponse
import com.unit.member.service.MemberAvailabilityUseCase
import com.unit.member.service.MemberProfileUseCase
import com.unit.member.service.MemberQueryUseCase
import com.unit.member.service.MemberSignupUseCase
import com.unit.member.service.MemberWithdrawalUseCase
import com.unit.platform.security.memberId
import com.unit.platform.web.response.ApiResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Validated
@RestController
@RequestMapping("/api/v1/members")
class MemberController(
    private val memberSignupUseCase: MemberSignupUseCase,
    private val memberQueryUseCase: MemberQueryUseCase,
    private val memberWithdrawalUseCase: MemberWithdrawalUseCase,
    private val memberAvailabilityUseCase: MemberAvailabilityUseCase,
    private val memberProfileUseCase: MemberProfileUseCase
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

    @GetMapping("/email-availability")
    fun checkEmailAvailability(
        @RequestParam
        @NotBlank
        @Email
        email: String,
    ): ResponseEntity<ApiResponse<MemberAvailabilityResponse>> {
        val response = memberAvailabilityUseCase.checkEmailAvailability(email)

        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @GetMapping("/nickname-availability")
    fun checkNicknameAvailability(
        @RequestParam
        @NotBlank
        @Size(min = 2, max = 40)
        @Pattern(regexp = "^[가-힣a-zA-Z0-9_-]+$")
        nickname: String,
    ): ResponseEntity<ApiResponse<MemberAvailabilityResponse>> {
        val response = memberAvailabilityUseCase.checkNicknameAvailability(nickname)

        return ResponseEntity.ok(ApiResponse.ok(response))
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

    @PutMapping("/me/profile")
    fun updateMyProfile(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: MemberProfileUpdateRequest
    ): ResponseEntity<ApiResponse<MemberProfileUpdateResponse>> {

        val response = memberProfileUseCase.updateProfile(jwt.memberId(), request)

        return ResponseEntity.ok(ApiResponse.ok(response))
    }
}
