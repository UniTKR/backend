package com.unit.member.repository

import com.unit.member.entity.School
import com.unit.member.enums.SchoolStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.test.context.TestPropertySource

@DataJpaTest
@TestPropertySource(
    properties = [
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
@DisplayName("School Repository 테스트")
class SchoolRepositoryTest @Autowired constructor(
    private val schoolRepository: SchoolRepository,
) {

    @Test
    @DisplayName("상태가 ACTIVE인 학교 목록을 이름 오름차순으로 조회한다")
    fun findAllByStatusOrderByNameAsc() {
        schoolRepository.save(createSchool(name = "Beta University"))
        schoolRepository.save(createSchool(name = "Alpha University"))
        schoolRepository.save(
            createSchool(
                name = "Inactive University",
                status = SchoolStatus.INACTIVE,
            ),
        )

        val schools = schoolRepository.findAllByStatusOrderByNameAsc()

        assertThat(schools.map { it.name }).containsExactly("Alpha University", "Beta University")
    }

    @Test
    @DisplayName("학교 ID와 상태로 학교를 조회한다")
    fun findByIdAndStatus() {
        val saved = schoolRepository.save(createSchool(name = "Unit University"))

        val found = schoolRepository.findByIdAndStatus(saved.id!!)

        assertThat(found).isNotNull
        assertThat(found?.id).isEqualTo(saved.id)
    }

    @Test
    @DisplayName("상태가 다른 학교는 ID 조회에서 제외한다")
    fun findByIdAndStatus_differentStatus() {
        val saved = schoolRepository.save(
            createSchool(
                name = "Inactive University",
                status = SchoolStatus.INACTIVE,
            ),
        )

        val found = schoolRepository.findByIdAndStatus(saved.id!!)

        assertThat(found).isNull()
    }

    @Test
    @DisplayName("학교 ID와 상태로 학교 존재 여부를 확인한다")
    fun existsByIdAndStatus() {
        val saved = schoolRepository.save(createSchool(name = "Unit University"))

        val exists = schoolRepository.existsByIdAndStatus(saved.id!!)

        assertThat(exists).isTrue()
    }

    @Test
    @DisplayName("상태가 다른 학교는 존재 여부 조회에서 제외한다")
    fun existsByIdAndStatus_differentStatus() {
        val saved = schoolRepository.save(
            createSchool(
                name = "Inactive University",
                status = SchoolStatus.INACTIVE,
            ),
        )

        val exists = schoolRepository.existsByIdAndStatus(saved.id!!)

        assertThat(exists).isFalse()
    }

    private fun createSchool(
        name: String = "Unit University",
        region: String = "Seoul",
        status: SchoolStatus = SchoolStatus.ACTIVE,
    ): School {
        return School(
            name = name,
            region = region,
            status = status,
        )
    }
}
