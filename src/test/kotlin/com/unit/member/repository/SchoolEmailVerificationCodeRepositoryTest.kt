package com.unit.member.repository

import com.unit.member.entity.SchoolEmailVerificationCode
import com.unit.member.enums.SchoolEmailVerificationStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.test.context.TestPropertySource
import java.time.LocalDateTime

@DataJpaTest
@TestPropertySource(
    properties = [
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
@DisplayName("SchoolEmailVerificationCode Repository 테스트")
class SchoolEmailVerificationCodeRepositoryTest @Autowired constructor(
    private val schoolEmailVerificationCodeRepository: SchoolEmailVerificationCodeRepository,
) {

    @Test
    @DisplayName("학교 ID, 이메일 해시, 상태로 가장 최근 인증 코드를 조회한다")
    fun findTopBySchoolIdAndEmailHashAndStatusOrderByCreatedAtDesc() {
        val schoolId = 1L
        val emailHash = ByteArray(32) { 1 }
        val old = schoolEmailVerificationCodeRepository.saveAndFlush(
            createSchoolEmailVerificationCode(
                schoolId = schoolId,
                emailHash = emailHash,
                codeHash = ByteArray(32) { 1 },
            ),
        )
        Thread.sleep(5)
        val latest = schoolEmailVerificationCodeRepository.saveAndFlush(
            createSchoolEmailVerificationCode(
                schoolId = schoolId,
                emailHash = emailHash,
                codeHash = ByteArray(32) { 2 },
            ),
        )
        schoolEmailVerificationCodeRepository.saveAndFlush(
            createSchoolEmailVerificationCode(
                schoolId = schoolId,
                emailHash = emailHash,
                codeHash = ByteArray(32) { 3 },
                status = SchoolEmailVerificationStatus.VERIFIED,
            ),
        )

        val found = schoolEmailVerificationCodeRepository
            .findTopBySchoolIdAndEmailHashAndStatusOrderByCreatedAtDesc(
                schoolId = schoolId,
                emailHash = emailHash,
            )

        assertThat(found).isNotNull
        assertThat(found?.id).isEqualTo(latest.id)
        assertThat(found?.id).isNotEqualTo(old.id)
    }

    @Test
    @DisplayName("상태가 다른 인증 코드는 학교와 이메일 해시 조회에서 제외한다")
    fun findTopBySchoolIdAndEmailHashAndStatusOrderByCreatedAtDesc_differentStatus() {
        val schoolId = 1L
        val emailHash = ByteArray(32) { 1 }
        schoolEmailVerificationCodeRepository.save(
            createSchoolEmailVerificationCode(
                schoolId = schoolId,
                emailHash = emailHash,
                status = SchoolEmailVerificationStatus.VERIFIED,
            ),
        )

        val found = schoolEmailVerificationCodeRepository
            .findTopBySchoolIdAndEmailHashAndStatusOrderByCreatedAtDesc(
                schoolId = schoolId,
                emailHash = emailHash,
            )

        assertThat(found).isNull()
    }

    @Test
    @DisplayName("회원 ID와 상태로 가장 최근 인증 코드를 조회한다")
    fun findTopByMemberIdAndStatusOrderByCreatedAtDesc() {
        val memberId = 1L
        val old = schoolEmailVerificationCodeRepository.saveAndFlush(
            createSchoolEmailVerificationCode(
                memberId = memberId,
                codeHash = ByteArray(32) { 1 },
            ),
        )
        Thread.sleep(5)
        val latest = schoolEmailVerificationCodeRepository.saveAndFlush(
            createSchoolEmailVerificationCode(
                memberId = memberId,
                emailHash = ByteArray(32) { 2 },
                codeHash = ByteArray(32) { 2 },
            ),
        )
        schoolEmailVerificationCodeRepository.saveAndFlush(
            createSchoolEmailVerificationCode(
                memberId = memberId,
                emailHash = ByteArray(32) { 3 },
                codeHash = ByteArray(32) { 3 },
                status = SchoolEmailVerificationStatus.VERIFIED,
            ),
        )

        val found = schoolEmailVerificationCodeRepository.findTopByMemberIdAndStatusOrderByCreatedAtDesc(memberId)

        assertThat(found).isNotNull
        assertThat(found?.id).isEqualTo(latest.id)
        assertThat(found?.id).isNotEqualTo(old.id)
    }

    @Test
    @DisplayName("상태가 다른 인증 코드는 회원 ID 조회에서 제외한다")
    fun findTopByMemberIdAndStatusOrderByCreatedAtDesc_differentStatus() {
        val memberId = 1L
        schoolEmailVerificationCodeRepository.save(
            createSchoolEmailVerificationCode(
                memberId = memberId,
                status = SchoolEmailVerificationStatus.VERIFIED,
            ),
        )

        val found = schoolEmailVerificationCodeRepository.findTopByMemberIdAndStatusOrderByCreatedAtDesc(memberId)

        assertThat(found).isNull()
    }

    private fun createSchoolEmailVerificationCode(
        memberId: Long? = null,
        schoolId: Long = 1L,
        emailHash: ByteArray = ByteArray(32) { 1 },
        codeHash: ByteArray = ByteArray(32) { 1 },
        status: SchoolEmailVerificationStatus = SchoolEmailVerificationStatus.PENDING,
        expiresAt: LocalDateTime = LocalDateTime.of(2026, 5, 8, 12, 0),
    ): SchoolEmailVerificationCode {
        return SchoolEmailVerificationCode(
            memberId = memberId,
            schoolId = schoolId,
            emailHash = emailHash,
            codeHash = codeHash,
            status = status,
            expiresAt = expiresAt,
        )
    }
}
