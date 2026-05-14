package com.unit.member.withdrawal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import java.time.LocalDateTime
import kotlin.test.Test

@DisplayName("회원 탈퇴 컨텍스트 테스트")
class MemberWithdrawalContextTest {

    @Test
    @DisplayName("회원 ID와 탈퇴 요청 시각을 가진다")
    fun create() {
        val requestedAt = LocalDateTime.of(2026, 5, 14, 12, 0)

        val context = MemberWithdrawalContext(
            memberId = 1L,
            requestedAt = requestedAt,
        )

        assertThat(context.memberId).isEqualTo(1L)
        assertThat(context.requestedAt).isEqualTo(requestedAt)
    }
}
