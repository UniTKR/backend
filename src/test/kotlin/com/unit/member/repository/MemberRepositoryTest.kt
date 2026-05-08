package com.unit.member.repository

import com.unit.member.entity.Member
import com.unit.member.enums.MemberStatus
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
@DisplayName("회원 Repository 테스트")
class MemberRepositoryTest @Autowired constructor(
    private val memberRepository: MemberRepository
) {

    @Test
    @DisplayName("이메일 해시로 활성 회원 존재 여부를 확인한다")
    fun existsByEmailHashAndDeletedAtIsNull() {
        val emailHash = ByteArray(32) { 1 }

        memberRepository.save(createMember(emailHash))

        val exists = memberRepository.existsByEmailHashAndDeletedAtIsNull(emailHash)

        assertThat(exists).isTrue()

    }

    @Test
    @DisplayName("삭제된 회원은 이메일 해시 존재 여부 조회에서 제외된다")
    fun existsByEmailHashAndDeletedAtIsNull_deletedMember() {
        val emailHash = ByteArray(32) { 1 }
        val member = createMember(emailHash = emailHash)
        member.delete(LocalDateTime.of(2026, 5, 8, 12, 0))
        memberRepository.save(member)

        val exists = memberRepository.existsByEmailHashAndDeletedAtIsNull(emailHash)

        assertThat(exists).isFalse()
    }

    @Test
    @DisplayName("이메일 해시로 활성 회원을 조회한다")
    fun findByEmailHashAndDeletedAtIsNull() {
        val emailHash = ByteArray(32) { 1 }
        memberRepository.save(
            createMember(emailHash = emailHash, nickname = "unit-user"),
        )

        val found = memberRepository.findByEmailHashAndDeletedAtIsNull(emailHash)

        assertThat(found).isNotNull
        assertThat(found?.nickname).isEqualTo("unit-user")
    }

    @Test
    @DisplayName("회원 ID와 상태로 삭제되지 않은 회원을 조회한다")
    fun findByIdAndStatusAndDeletedAtIsNull() {
        val saved = memberRepository.save(
            createMember(status = MemberStatus.ACTIVE),
        )

        val found = memberRepository.findByIdAndStatusAndDeletedAtIsNull(
            id = saved.id!!,
            status = MemberStatus.ACTIVE,
        )

        assertThat(found).isNotNull
        assertThat(found?.id).isEqualTo(saved.id)
    }

    private fun createMember(
        emailHash: ByteArray = ByteArray(32) { 1 },
        emailEncrypted: ByteArray = ByteArray(64) { 2 },
        nickname: String = "unit-user",
        status: MemberStatus = MemberStatus.ACTIVE,
    ): Member {
        return Member(
            emailHash = emailHash,
            emailEncrypted = emailEncrypted,
            nickname = nickname,
            status = status,
        )
    }
}