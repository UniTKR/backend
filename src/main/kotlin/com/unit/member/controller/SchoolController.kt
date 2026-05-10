package com.unit.member.controller

import com.unit.member.dto.SchoolResponse
import com.unit.member.service.SchoolQueryUseCase
import com.unit.platform.web.response.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/schools")
class SchoolController(
    private val schoolQueryUseCase: SchoolQueryUseCase
) {

    @GetMapping
    fun getSchools(@RequestParam(required = false) keyword: String?): ApiResponse<List<SchoolResponse>> {
        return ApiResponse.ok(data = schoolQueryUseCase.getSchools(keyword))
    }
}