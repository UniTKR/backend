package com.unit.member.withdrawal

import java.time.LocalDateTime

data class MemberWithdrawalContext(
    val memberId: Long,
    val requestedAt: LocalDateTime,
)