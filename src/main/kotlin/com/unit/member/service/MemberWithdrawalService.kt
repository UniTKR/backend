package com.unit.member.service

import com.unit.member.enums.MemberStatus
import com.unit.member.exception.MemberErrorCode
import com.unit.member.repository.MemberRepository
import com.unit.member.withdrawal.MemberWithdrawalContext
import com.unit.member.withdrawal.MemberWithdrawalPolicy
import com.unit.platform.error.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class MemberWithdrawalService(
    private val memberRepository: MemberRepository,
    private val refreshTokenUseCase: RefreshTokenUseCase,
    private val withdrawalPolicies: List<MemberWithdrawalPolicy>,
) : MemberWithdrawalUseCase {

    override fun withdraw(memberId: Long) {
        val member = memberRepository.findByIdAndStatusInAndDeletedAtIsNull(
            id = memberId,
            statuses = listOf(MemberStatus.PENDING, MemberStatus.ACTIVE),
        ) ?: throw BusinessException(MemberErrorCode.MEMBER_LOGIN_FORBIDDEN)

        val now = LocalDateTime.now()
        val context = MemberWithdrawalContext(
            memberId = requireNotNull(member.id),
            requestedAt = now,
        )

        withdrawalPolicies.forEach { it.validate(context) }

        member.withdraw(now)
        refreshTokenUseCase.revokeAll(context.memberId)

        withdrawalPolicies.forEach { it.apply(context) }
    }
}