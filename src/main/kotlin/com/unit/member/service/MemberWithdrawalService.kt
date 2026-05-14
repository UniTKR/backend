package com.unit.member.service

import com.unit.member.enums.MemberStatus
import com.unit.member.exception.MemberErrorCode
import com.unit.member.repository.MemberConsentRepository
import com.unit.member.repository.MemberRepository
import com.unit.member.withdrawal.MemberWithdrawalContext
import com.unit.member.withdrawal.MemberWithdrawalPolicy
import com.unit.platform.error.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.LocalDateTime

@Service
@Transactional
class MemberWithdrawalService(
    private val memberRepository: MemberRepository,
    private val memberConsentRepository: MemberConsentRepository,
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
        memberConsentRepository.findAllByMemberId(context.memberId)
            .forEach { it.withdraw(now) }
        refreshTokenUseCase.revokeAll(context.memberId)

        registerAfterCommit {
            applyWithdrawalPoliciesAfterCommit(context)
        }
    }

    private fun registerAfterCommit(action: () -> Unit) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action()
            return
        }

        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    action()
                }
            },
        )
    }

    private fun applyWithdrawalPoliciesAfterCommit(context: MemberWithdrawalContext) {
        withdrawalPolicies.forEach { policy ->
            try {
                policy.apply(context)
            } catch (e: Exception) {
                log.error(
                    "Member withdrawal post-processing failed. memberId=${context.memberId}, policy=${policy.javaClass.name}",
                    e,
                )
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(MemberWithdrawalService::class.java)
    }

}
