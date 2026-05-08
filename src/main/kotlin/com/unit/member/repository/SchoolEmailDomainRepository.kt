package com.unit.member.repository

import com.unit.member.entity.SchoolEmailDomain
import com.unit.member.enums.SchoolStatus
import org.springframework.data.jpa.repository.JpaRepository

interface SchoolEmailDomainRepository : JpaRepository<SchoolEmailDomain, Long> {
    fun findAllBySchoolIdAndStatus(
        schoolId: Long,
        status: SchoolStatus = SchoolStatus.ACTIVE,
    ): List<SchoolEmailDomain>

    fun existsBySchoolIdAndDomainAndStatus(
        schoolId: Long,
        domain: String,
        status: SchoolStatus = SchoolStatus.ACTIVE,
    ): Boolean

    fun findByDomainAndStatus(
        domain: String,
        status: SchoolStatus = SchoolStatus.ACTIVE,
    ): SchoolEmailDomain?
}