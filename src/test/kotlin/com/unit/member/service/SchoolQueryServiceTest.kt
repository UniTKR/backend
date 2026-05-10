package com.unit.member.service

import com.unit.member.entity.School
import com.unit.member.enums.SchoolStatus
import com.unit.member.repository.SchoolRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("학교 조회 서비스 테스트")
class SchoolQueryServiceTest {

    private val schoolRepository = mockk<SchoolRepository>()
    private val schoolQueryService = SchoolQueryService(schoolRepository)

    @Test
    @DisplayName("키워드가 없으면 활성 학교 전체 목록을 조회한다")
    fun getSchoolWithoutKeyword() {
        val school = createSchool(id = 1L, name = "서울대학교")
        every {
            schoolRepository.findAllByStatusOrderByNameAsc(SchoolStatus.ACTIVE)
        } returns listOf(school)

        val result = schoolQueryService.getSchools(null)

        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo(1L)
        assertThat(result[0].name).isEqualTo("서울대학교")
        assertThat(result[0].region).isEqualTo("서울")

        verify(exactly = 1) {
            schoolRepository.findAllByStatusOrderByNameAsc(SchoolStatus.ACTIVE)
        }
        verify(exactly = 0) {
            schoolRepository.findAllByStatusAndNameContainingOrderByNameAsc(any(), any())
        }

    }

    @Test
    @DisplayName("키워드가 빈 문자열/공백이면 활성 학교 전체 목록을 조회한다")
    fun getSchoolWithSpaceKeyword() {
        val school = createSchool(id = 1L, name = "서울대학교")
        every {
            schoolRepository.findAllByStatusOrderByNameAsc(SchoolStatus.ACTIVE)
        } returns listOf(school)

        val result = schoolQueryService.getSchools("  ")

        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo(1L)
        assertThat(result[0].name).isEqualTo("서울대학교")
        assertThat(result[0].region).isEqualTo("서울")

        verify(exactly = 1) {
            schoolRepository.findAllByStatusOrderByNameAsc(SchoolStatus.ACTIVE)
        }
        verify(exactly = 0) {
            schoolRepository.findAllByStatusAndNameContainingOrderByNameAsc(any(), any())
        }

    }

    @Test
    @DisplayName("키워드 앞 뒤에 공백이 있으면 제거 후 학교를 조회한다")
    fun getSchoolWithKeywordSpace() {
        val school = createSchool(id = 1L, name = "서울대학교")
        every {
            schoolRepository.findAllByStatusAndNameContainingOrderByNameAsc(SchoolStatus.ACTIVE, "서울대학교")
        } returns listOf(school)

        val result = schoolQueryService.getSchools(" 서울대학교 ")

        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo(1L)
        assertThat(result[0].name).isEqualTo("서울대학교")
        assertThat(result[0].region).isEqualTo("서울")

        verify(exactly = 0) {
            schoolRepository.findAllByStatusOrderByNameAsc(any())
        }
        verify(exactly = 1) {
            schoolRepository.findAllByStatusAndNameContainingOrderByNameAsc(SchoolStatus.ACTIVE, "서울대학교")
        }

    }

    private fun createSchool(
        id: Long = 1L,
        name: String = "서울대학교",
        region: String? = "서울",
        status: SchoolStatus = SchoolStatus.ACTIVE,
    ): School {
        return School(
            id = id,
            name = name,
            region = region,
            status = status,
        )
    }

}