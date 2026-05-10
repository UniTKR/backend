package com.unit.member.dto

import com.unit.member.entity.School

data class SchoolResponse(
    val id: Long,
    val name: String,
    val region: String?
) {
    companion object {
        fun from(school: School): SchoolResponse {
            return SchoolResponse(
                id = school.id!!,
                name = school.name,
                region = school.region
            )
        }
    }
}
