package com.unit.member.service

import com.unit.member.entity.Member
import com.unit.member.enums.MemberStatus
import com.unit.member.exception.MemberErrorCode
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
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test

@DisplayName("회원 탈퇴 서비스 테스트")
class MemberWithdrawalServiceTest {

    private val memberRepository = mockk<MemberRepository>()
    private val refreshTokenUseCase = mockk<RefreshTokenUseCase>()
    private val withdrawalPolicy = mockk<MemberWithdrawalPolicy>()

    private val memberWithdrawalService = MemberWithdrawalService(
        memberRepository = memberRepository,
        refreshTokenUseCase = refreshTokenUseCase,
        withdrawalPolicies = listOf(withdrawalPolicy),
    )

    @Test
    @DisplayName("탈퇴 가능 회원이면 정책 검증 후 회원을 탈퇴 처리하고 Refresh Token을 모두 폐기한다")
    fun withdraw() {
        val member = createMember()
        val validateContext = slot<MemberWithdrawalContext>()
        val applyContext = slot<MemberWithdrawalContext>()

        every {
            memberRepository.findByIdAndStatusInAndDeletedAtIsNull(
                id = 1L,
                statuses = listOf(MemberStatus.PENDING, MemberStatus.ACTIVE),
            )
        } returns member
        every { withdrawalPolicy.validate(capture(validateContext)) } just Runs
        every { refreshTokenUseCase.revokeAll(1L) } just Runs
        every { withdrawalPolicy.apply(capture(applyContext)) } just Runs

        memberWithdrawalService.withdraw(1L)

        assertThat(member.status).isEqualTo(MemberStatus.DELETED)
        assertThat(member.deletedAt).isNotNull()
        assertThat(member.nickname).isEqualTo("탈퇴한 사용자")
        assertThat(member.profileImageUrl).isNull()
        assertThat(member.passwordHash).isNull()
        assertThat(member.emailEncrypted).isNull()
        assertThat(member.phoneHash).isNull()
        assertThat(member.emailHash).isNotNull()

        assertThat(validateContext.captured.memberId).isEqualTo(1L)
        assertThat(applyContext.captured.memberId).isEqualTo(1L)
        assertThat(applyContext.captured.requestedAt).isEqualTo(validateContext.captured.requestedAt)

        verify(exactly = 1) { withdrawalPolicy.validate(any()) }
        verify(exactly = 1) { refreshTokenUseCase.revokeAll(1L) }
        verify(exactly = 1) { withdrawalPolicy.apply(any()) }
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
}