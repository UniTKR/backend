package com.unit.member.service

import com.unit.member.dto.SchoolResponse

interface SchoolQueryUseCase {
    fun getSchools(keyword: String?): List<SchoolResponse>
}