package com.unit.member.service

import com.unit.member.entity.Member
import com.unit.member.entity.MemberConsent
import com.unit.member.enums.MemberConsentType
import com.unit.member.enums.MemberStatus
import com.unit.member.exception.MemberErrorCode
import com.unit.member.repository.MemberConsentRepository
import com.unit.member.repository.MemberRepository
import com.unit.member.withdrawal.MemberWithdrawalContext
import com.unit.member.withdrawal.MemberWithdrawalPolicy
import com.unit.platform.error.BusinessException
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.springframework.transaction.support.TransactionSynchronizationManager
import kotlin.test.Test

@DisplayName("회원 탈퇴 서비스 테스트")
class MemberWithdrawalServiceTest {

    private val memberRepository = mockk<MemberRepository>()
    private val memberConsentRepository = mockk<MemberConsentRepository>()
    private val refreshTokenUseCase = mockk<RefreshTokenUseCase>()
    private val withdrawalPolicy = mockk<MemberWithdrawalPolicy>()

    private val memberWithdrawalService = MemberWithdrawalService(
        memberRepository = memberRepository,
        memberConsentRepository = memberConsentRepository,
        refreshTokenUseCase = refreshTokenUseCase,
        withdrawalPolicies = listOf(withdrawalPolicy),
    )

    @AfterEach
    fun tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }

    @Test
    @DisplayName("탈퇴 가능 회원이면 정책 검증 후 회원을 탈퇴 처리하고 Refresh Token을 모두 폐기한다")
    fun withdraw() {
        val member = createMember()
        val consents = createMemberConsents()
        val validateContext = slot<MemberWithdrawalContext>()
        val applyContext = slot<MemberWithdrawalContext>()

        every {
            memberRepository.findByIdAndStatusInAndDeletedAtIsNull(
                id = 1L,
                statuses = listOf(MemberStatus.PENDING, MemberStatus.ACTIVE),
            )
        } returns member
        every { withdrawalPolicy.validate(capture(validateContext)) } just Runs
        every { memberConsentRepository.findAllByMemberId(1L) } returns consents
        every { refreshTokenUseCase.revokeAll(1L) } just Runs
        every { withdrawalPolicy.apply(capture(applyContext)) } just Runs

        TransactionSynchronizationManager.initSynchronization()
        memberWithdrawalService.withdraw(1L)

        assertThat(member.status).isEqualTo(MemberStatus.DELETED)
        assertThat(member.deletedAt).isNotNull()
        assertThat(member.nickname).isEqualTo("탈퇴한 사용자")
        assertThat(member.profileImageUrl).isNull()
        assertThat(member.passwordHash).isNull()
        assertThat(member.emailEncrypted).isNull()
        assertThat(member.phoneHash).isNull()
        assertThat(member.emailHash).isNotNull()

        assertThat(consents).allSatisfy {
            assertThat(it.withdrawnAt).isEqualTo(validateContext.captured.requestedAt)
        }
        assertThat(validateContext.captured.memberId).isEqualTo(1L)

        verify(exactly = 1) { withdrawalPolicy.validate(any()) }
        verify(exactly = 1) { memberConsentRepository.findAllByMemberId(1L) }
        verify(exactly = 1) { refreshTokenUseCase.revokeAll(1L) }
        verify(exactly = 0) { withdrawalPolicy.apply(any()) }

        val synchronizations = TransactionSynchronizationManager.getSynchronizations()
        assertThat(synchronizations).hasSize(1)

        synchronizations.forEach { it.afterCommit() }

        assertThat(applyContext.captured.memberId).isEqualTo(1L)
        assertThat(applyContext.captured.requestedAt).isEqualTo(validateContext.captured.requestedAt)

        verify(exactly = 1) { withdrawalPolicy.apply(any()) }
    }

    @Test
    @DisplayName("트랜잭션 동기화가 없으면 탈퇴 후처리를 즉시 실행한다")
    fun withdrawWithoutTransactionSynchronization() {
        val member = createMember()
        val consents = createMemberConsents()
        val validateContext = slot<MemberWithdrawalContext>()
        val applyContext = slot<MemberWithdrawalContext>()

        every {
            memberRepository.findByIdAndStatusInAndDeletedAtIsNull(
                id = 1L,
                statuses = listOf(MemberStatus.PENDING, MemberStatus.ACTIVE),
            )
        } returns member
        every { withdrawalPolicy.validate(capture(validateContext)) } just Runs
        every { memberConsentRepository.findAllByMemberId(1L) } returns consents
        every { refreshTokenUseCase.revokeAll(1L) } just Runs
        every { withdrawalPolicy.apply(capture(applyContext)) } just Runs

        memberWithdrawalService.withdraw(1L)

        assertThat(member.status).isEqualTo(MemberStatus.DELETED)
        assertThat(consents).allSatisfy {
            assertThat(it.withdrawnAt).isEqualTo(validateContext.captured.requestedAt)
        }
        assertThat(applyContext.captured.memberId).isEqualTo(1L)
        assertThat(applyContext.captured.requestedAt).isEqualTo(validateContext.captured.requestedAt)

        verify(exactly = 1) { withdrawalPolicy.validate(any()) }
        verify(exactly = 1) { memberConsentRepository.findAllByMemberId(1L) }
        verify(exactly = 1) { refreshTokenUseCase.revokeAll(1L) }
        verify(exactly = 1) { withdrawalPolicy.apply(any()) }
    }

    @Test
    @DisplayName("탈퇴 후처리 정책이 실패해도 다음 후처리 정책을 계속 실행한다")
    fun withdrawWithFailedPostProcessingPolicy() {
        val member = createMember()
        val consents = createMemberConsents()
        val failedPolicy = mockk<MemberWithdrawalPolicy>()
        val nextPolicy = mockk<MemberWithdrawalPolicy>()
        val service = MemberWithdrawalService(
            memberRepository = memberRepository,
            memberConsentRepository = memberConsentRepository,
            refreshTokenUseCase = refreshTokenUseCase,
            withdrawalPolicies = listOf(failedPolicy, nextPolicy),
        )

        every {
            memberRepository.findByIdAndStatusInAndDeletedAtIsNull(
                id = 1L,
                statuses = listOf(MemberStatus.PENDING, MemberStatus.ACTIVE),
            )
        } returns member
        every { failedPolicy.validate(any()) } just Runs
        every { nextPolicy.validate(any()) } just Runs
        every { memberConsentRepository.findAllByMemberId(1L) } returns consents
        every { refreshTokenUseCase.revokeAll(1L) } just Runs
        every { failedPolicy.apply(any()) } throws RuntimeException("post-processing failed")
        every { nextPolicy.apply(any()) } just Runs

        service.withdraw(1L)

        verify(exactly = 1) { failedPolicy.validate(any()) }
        verify(exactly = 1) { nextPolicy.validate(any()) }
        verify(exactly = 1) { failedPolicy.apply(any()) }
        verify(exactly = 1) { nextPolicy.apply(any()) }
    }

    @Test
    @DisplayName("탈퇴 가능한 회원을 찾을 수 없으면 예외가 발생한다")
    fun withdrawWithNotFoundMember() {
        every {
            memberRepository.findByIdAndStatusInAndDeletedAtIsNull(
                id = 1L,
                statuses = listOf(MemberStatus.PENDING, MemberStatus.ACTIVE),
            )
        } returns null

        assertThatThrownBy {
            memberWithdrawalService.withdraw(1L)
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(MemberErrorCode.MEMBER_LOGIN_FORBIDDEN)

        verify(exactly = 0) { withdrawalPolicy.validate(any()) }
        verify(exactly = 0) { memberConsentRepository.findAllByMemberId(any()) }
        verify(exactly = 0) { refreshTokenUseCase.revokeAll(any()) }
        verify(exactly = 0) { withdrawalPolicy.apply(any()) }
    }

    @Test
    @DisplayName("조회된 회원 ID가 없으면 예외가 발생한다")
    fun withdrawWithNullMemberId() {
        val member = createMember(id = null)

        every {
            memberRepository.findByIdAndStatusInAndDeletedAtIsNull(
                id = 1L,
                statuses = listOf(MemberStatus.PENDING, MemberStatus.ACTIVE),
            )
        } returns member

        assertThatThrownBy {
            memberWithdrawalService.withdraw(1L)
        }.isInstanceOf(IllegalArgumentException::class.java)

        verify(exactly = 0) { withdrawalPolicy.validate(any()) }
        verify(exactly = 0) { memberConsentRepository.findAllByMemberId(any()) }
        verify(exactly = 0) { refreshTokenUseCase.revokeAll(any()) }
        verify(exactly = 0) { withdrawalPolicy.apply(any()) }
    }

    private fun createMember(
        id: Long? = 1L,
        emailHash: ByteArray? = ByteArray(32) { 1 },
        emailEncrypted: ByteArray? = ByteArray(64) { 2 },
        phoneHash: ByteArray? = ByteArray(32) { 3 },
        passwordHash: String? = "encoded-password",
        nickname: String = "unit_user",
        profileImageUrl: String? = "profile_image_url",
        status: MemberStatus = MemberStatus.ACTIVE,
    ): Member {
        return Member(
            id = id,
            emailHash = emailHash,
            emailEncrypted = emailEncrypted,
            phoneHash = phoneHash,
            passwordHash = passwordHash,
            nickname = nickname,
            profileImageUrl = profileImageUrl,
            status = status,
        )
    }

    private fun createMemberConsents(): List<MemberConsent> {
        return listOf(
            MemberConsent(
                memberId = 1L,
                consentType = MemberConsentType.TERMS_OF_SERVICE,
                policyVersion = "terms-v1",
                agreed = true,
                agreedAt = null,
            ),
            MemberConsent(
                memberId = 1L,
                consentType = MemberConsentType.PRIVACY_POLICY,
                policyVersion = "privacy-v1",
                agreed = true,
                agreedAt = null,
            ),
        )
    }
}
