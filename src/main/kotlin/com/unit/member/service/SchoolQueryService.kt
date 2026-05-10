package com.unit.member.service

import com.unit.member.dto.SchoolResponse
import com.unit.member.enums.SchoolStatus
import com.unit.member.repository.SchoolRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SchoolQueryService(
    private val schoolRepository: SchoolRepository,
) : SchoolQueryUseCase {

    override fun getSchools(keyword: String?): List<SchoolResponse> {
        val trimmedKeyword = keyword?.trim()

        val schools = if (trimmedKeyword.isNullOrBlank()) {
            schoolRepository.findAllByStatusOrderByNameAsc(SchoolStatus.ACTIVE)
        } else {
            schoolRepository.findAllByStatusAndNameContainingOrderByNameAsc(
                status = SchoolStatus.ACTIVE,
                name = trimmedKeyword
            )
        }

        return schools.map(SchoolResponse::from)
    }
}