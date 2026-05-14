package com.unit.member.withdrawal

import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.DisplayName
import java.time.LocalDateTime
import kotlin.test.Test

@DisplayName("회원 탈퇴 정책 테스트")
class MemberWithdrawalPolicyTest {

    @Test
    @DisplayName("기본 validate와 apply는 아무 작업도 하지 않는다")
    fun defaultMethods() {
        val policy = object : MemberWithdrawalPolicy {}
        val context = MemberWithdrawalContext(
            memberId = 1L,
            requestedAt = LocalDateTime.of(2026, 5, 14, 12, 0),
        )

        assertThatCode {
            policy.validate(context)
            policy.apply(context)
        }.doesNotThrowAnyException()
    }
}
