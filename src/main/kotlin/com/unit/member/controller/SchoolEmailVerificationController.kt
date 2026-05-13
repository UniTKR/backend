package com.unit.member.controller

import com.unit.member.dto.SchoolAuthDto.*
import com.unit.member.service.SchoolEmailVerificationUseCase
import com.unit.platform.security.memberId
import com.unit.platform.web.response.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/school-email-verifications")
class SchoolEmailVerificationController(
    private val schoolEmailVerificationUseCase: SchoolEmailVerificationUseCase
) {

    @PostMapping("/request")
    fun request(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: SchoolEmailVerificationRequest,
    ): ResponseEntity<ApiResponse<SchoolEmailVerificationResponse>> {
        val response = schoolEmailVerificationUseCase.request(
            memberId = jwt.memberId(),
            request = request,
        )

        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @PostMapping("/confirm")
    fun confirm(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: SchoolEmailVerificationConfirmRequest,
    ): ResponseEntity<ApiResponse<SchoolEmailVerificationConfirmResponse>> {
        val response = schoolEmailVerificationUseCase.confirm(
            memberId = jwt.memberId(),
            request = request,
        )

        return ResponseEntity.ok(ApiResponse.ok(response))
    }
}