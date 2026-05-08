package com.unit.member.repository

import com.unit.member.entity.UserSchoolVerification
import com.unit.member.enums.UserSchoolVerificationMethod
import com.unit.member.enums.UserSchoolVerificationStatus
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
@DisplayName("UserSchoolVerification Repository 테스트")
class UserSchoolVerificationRepositoryTest @Autowired constructor(
    private val userSchoolVerificationRepository: UserSchoolVerificationRepository,
) {

    @Test
    @DisplayName("회원 ID와 학교 ID로 학교 인증 정보를 조회한다")
    fun findByMemberIdAndSchoolId() {
        val saved = userSchoolVerificationRepository.save(
            createUserSchoolVerification(memberId = 1L, schoolId = 1L),
        )

        val found = userSchoolVerificationRepository.findByMemberIdAndSchoolId(
            memberId = 1L,
            schoolId = 1L,
        )

        assertThat(found).isNotNull
        assertThat(found?.id).isEqualTo(saved.id)
    }

    @Test
    @DisplayName("회원 ID와 학교 ID가 다른 학교 인증 정보는 조회하지 않는다")
    fun findByMemberIdAndSchoolId_notFound() {
        userSchoolVerificationRepository.save(
            createUserSchoolVerification(memberId = 1L, schoolId = 1L),
        )

        val found = userSchoolVerificationRepository.findByMemberIdAndSchoolId(
            memberId = 1L,
            schoolId = 2L,
        )

        assertThat(found).isNull()
    }

    @Test
    @DisplayName("회원 ID와 상태로 학교 인증 정보를 조회한다")
    fun findByMemberIdAndStatus() {
        val memberId = 1L
        val verified = userSchoolVerificationRepository.save(
            createUserSchoolVerification(memberId = memberId, schoolId = 1L),
        )
        userSchoolVerificationRepository.save(
            createUserSchoolVerification(
                memberId = memberId,
                schoolId = 2L,
                status = UserSchoolVerificationStatus.PENDING,
            ),
        )

        val found = userSchoolVerificationRepository.findByMemberIdAndStatus(memberId)

        assertThat(found).isNotNull
        assertThat(found?.id).isEqualTo(verified.id)
    }

    @Test
    @DisplayName("상태가 다른 학교 인증 정보는 회원 ID 조회에서 제외한다")
    fun findByMemberIdAndStatus_differentStatus() {
        val memberId = 1L
        userSchoolVerificationRepository.save(
            createUserSchoolVerification(
                memberId = memberId,
                schoolId = 1L,
                status = UserSchoolVerificationStatus.PENDING,
            ),
        )

        val found = userSchoolVerificationRepository.findByMemberIdAndStatus(memberId)

        assertThat(found).isNull()
    }

    @Test
    @DisplayName("회원 ID, 학교 ID, 상태로 학교 인증 존재 여부를 확인한다")
    fun existsByMemberIdAndSchoolIdAndStatus() {
        val memberId = 1L
        val schoolId = 1L
        userSchoolVerificationRepository.save(
            createUserSchoolVerification(memberId = memberId, schoolId = schoolId),
        )

        val exists = userSchoolVerificationRepository.existsByMemberIdAndSchoolIdAndStatus(
            memberId = memberId,
            schoolId = schoolId,
        )

        assertThat(exists).isTrue()
    }

    @Test
    @DisplayName("상태가 다른 학교 인증 정보는 존재 여부 조회에서 제외한다")
    fun existsByMemberIdAndSchoolIdAndStatus_differentStatus() {
        val memberId = 1L
        val schoolId = 1L
        userSchoolVerificationRepository.save(
            createUserSchoolVerification(
                memberId = memberId,
                schoolId = schoolId,
                status = UserSchoolVerificationStatus.PENDING,
            ),
        )

        val exists = userSchoolVerificationRepository.existsByMemberIdAndSchoolIdAndStatus(
            memberId = memberId,
            schoolId = schoolId,
        )

        assertThat(exists).isFalse()
    }

    private fun createUserSchoolVerification(
        memberId: Long = 1L,
        schoolId: Long = 1L,
        method: UserSchoolVerificationMethod = UserSchoolVerificationMethod.EMAIL,
        status: UserSchoolVerificationStatus = UserSchoolVerificationStatus.VERIFIED,
    ): UserSchoolVerification {
        return UserSchoolVerification(
            memberId = memberId,
            schoolId = schoolId,
            method = method,
            status = status,
        )
    }
}
