package com.unit.member.repository

import com.unit.member.entity.School
import com.unit.member.enums.SchoolStatus
import org.springframework.data.jpa.repository.JpaRepository

interface SchoolRepository : JpaRepository<School, Long> {

    fun findAllByStatusOrderByNameAsc(status: SchoolStatus = SchoolStatus.ACTIVE): List<School>

    fun findByIdAndStatus(
        id: Long,
        status: SchoolStatus = SchoolStatus.ACTIVE,
    ): School?

    fun existsByIdAndStatus(
        id: Long,
        status: SchoolStatus = SchoolStatus.ACTIVE,
    ): Boolean
}