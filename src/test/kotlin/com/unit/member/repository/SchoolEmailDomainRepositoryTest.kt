package com.unit.member.repository

import com.unit.member.entity.SchoolEmailDomain
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
@DisplayName("SchoolEmailDomain Repository 테스트")
class SchoolEmailDomainRepositoryTest @Autowired constructor(
    private val schoolEmailDomainRepository: SchoolEmailDomainRepository,
) {

    @Test
    @DisplayName("학교 ID와 상태로 이메일 도메인 목록을 조회한다")
    fun findAllBySchoolIdAndStatus() {
        val schoolId = 1L
        val active = schoolEmailDomainRepository.save(
            createSchoolEmailDomain(schoolId = schoolId, domain = "unit.ac.kr"),
        )
        schoolEmailDomainRepository.save(
            createSchoolEmailDomain(
                schoolId = schoolId,
                domain = "inactive.unit.ac.kr",
                status = SchoolStatus.INACTIVE,
            ),
        )
        schoolEmailDomainRepository.save(
            createSchoolEmailDomain(schoolId = 2L, domain = "other.ac.kr"),
        )

        val domains = schoolEmailDomainRepository.findAllBySchoolIdAndStatus(schoolId)

        assertThat(domains.map { it.id }).containsExactly(active.id)
    }

    @Test
    @DisplayName("학교 ID, 도메인, 상태로 이메일 도메인 존재 여부를 확인한다")
    fun existsBySchoolIdAndDomainAndStatus() {
        val schoolId = 1L
        schoolEmailDomainRepository.save(
            createSchoolEmailDomain(schoolId = schoolId, domain = "unit.ac.kr"),
        )

        val exists = schoolEmailDomainRepository.existsBySchoolIdAndDomainAndStatus(
            schoolId = schoolId,
            domain = "unit.ac.kr",
        )

        assertThat(exists).isTrue()
    }

    @Test
    @DisplayName("상태가 다른 이메일 도메인은 존재 여부 조회에서 제외한다")
    fun existsBySchoolIdAndDomainAndStatus_differentStatus() {
        val schoolId = 1L
        schoolEmailDomainRepository.save(
            createSchoolEmailDomain(
                schoolId = schoolId,
                domain = "unit.ac.kr",
                status = SchoolStatus.INACTIVE,
            ),
        )

        val exists = schoolEmailDomainRepository.existsBySchoolIdAndDomainAndStatus(
            schoolId = schoolId,
            domain = "unit.ac.kr",
        )

        assertThat(exists).isFalse()
    }

    @Test
    @DisplayName("도메인과 상태로 이메일 도메인을 조회한다")
    fun findByDomainAndStatus() {
        val saved = schoolEmailDomainRepository.save(
            createSchoolEmailDomain(domain = "unit.ac.kr"),
        )

        val found = schoolEmailDomainRepository.findByDomainAndStatus("unit.ac.kr")

        assertThat(found).isNotNull
        assertThat(found?.id).isEqualTo(saved.id)
    }

    @Test
    @DisplayName("상태가 다른 이메일 도메인은 도메인 조회에서 제외한다")
    fun findByDomainAndStatus_differentStatus() {
        schoolEmailDomainRepository.save(
            createSchoolEmailDomain(
                domain = "unit.ac.kr",
                status = SchoolStatus.INACTIVE,
            ),
        )

        val found = schoolEmailDomainRepository.findByDomainAndStatus("unit.ac.kr")

        assertThat(found).isNull()
    }

    private fun createSchoolEmailDomain(
        schoolId: Long = 1L,
        domain: String = "unit.ac.kr",
        status: SchoolStatus = SchoolStatus.ACTIVE,
    ): SchoolEmailDomain {
        return SchoolEmailDomain(
            schoolId = schoolId,
            domain = domain,
            status = status,
        )
    }
}
