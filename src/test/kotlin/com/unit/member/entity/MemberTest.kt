package com.unit.member.entity

import com.unit.member.enums.MemberStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("회원 엔티티 테스트")
class MemberTest {

    private lateinit var member: Member

    @BeforeEach
    fun setUp() {
        member = Member(
            emailHash = ByteArray(32) { 1 },
            emailEncrypted = ByteArray(64) { 2 },
            nickname = "unit-user",
        )
    }

    @Test
    @DisplayName("닉네임 변경")
    fun changeNickname() {
        member.changeNickname("new-nickname")

        assertThat(member.nickname).isEqualTo("new-nickname")
    }

    @Test
    @DisplayName("회원을 정지 상태로 변경한다")
    fun suspend() {
        member.suspend()

        assertThat(member.status).isEqualTo(MemberStatus.SUSPENDED)
    }

    @Test
    @DisplayName("회원을 삭제 상태로 변경하고 삭제 시각을 기록한다")
    fun delete() {
        val now = LocalDateTime.of(2026, 5, 8, 12, 0)

        member.delete(now)

        assertThat(member.status).isEqualTo(MemberStatus.DELETED)
        assertThat(member.deletedAt).isEqualTo(now)
    }

}