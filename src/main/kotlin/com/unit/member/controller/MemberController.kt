package com.unit.member.controller

import com.unit.member.dto.MemberSignupRequest
import com.unit.member.dto.MemberSignupResponse
import com.unit.member.service.MemberSignupUseCase
import com.unit.platform.web.response.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/members")
class MemberController(
    private val memberSignupUseCase: MemberSignupUseCase
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
}