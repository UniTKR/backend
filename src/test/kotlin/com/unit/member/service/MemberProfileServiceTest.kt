package com.unit.member.service

import com.unit.member.dto.MemberProfileUpdateRequest
import com.unit.member.entity.Member
import com.unit.member.enums.MemberStatus
import com.unit.member.exception.MemberErrorCode
import com.unit.member.repository.MemberRepository
import com.unit.platform.error.BusinessException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test

@DisplayName("MemberProfileService 테스트")
class MemberProfileServiceTest {

    private val memberRepository = mockk<MemberRepository>()

    private val memberProfileService = MemberProfileService(
        memberRepository = memberRepository
    )

    @Test
    @DisplayName("정상 수정")
    fun updateProfile() {

        val member = createMember(
            id = 1L,
            nickname = "old_nickname",
            profileImageUrl = "old_profile_img",
        )
        val request = createRequest()

        every { memberRepository.findByIdAndStatusAndDeletedAtIsNull(1L) } returns member
        every { memberRepository.existsByNicknameAndIdNotAndDeletedAtIsNull("new_nickname", 1L) } returns false

        val response = memberProfileService.updateProfile(1L, request)

        assertThat(response.memberId).isEqualTo(1L)
        assertThat(response.nickname).isEqualTo("new_nickname")
        assertThat(response.profileImageUrl).isEqualTo("new_profile_img")

        assertThat(member.nickname).isEqualTo("new_nickname")
        assertThat(member.profileImageUrl).isEqualTo("new_profile_img")

        verify(exactly = 1) {
            memberRepository.existsByNicknameAndIdNotAndDeletedAtIsNull("new_nickname", 1L)
        }
    }

    @Test
    @DisplayName("기존 닉네임과 같으면 닉네임 중복 검사를 하지 않는다")
    fun updateProfileWithSameNickname() {
        val member = createMember(
            id = 1L,
            nickname = "unit_user",
            profileImageUrl = "old_profile_img",
        )
        val request = createRequest(
            nickname = " unit_user ",
            profileImageUrl = null,
        )

        every { memberRepository.findByIdAndStatusAndDeletedAtIsNull(1L) } returns member

        val response = memberProfileService.updateProfile(1L, request)

        assertThat(response.nickname).isEqualTo("unit_user")
        assertThat(response.profileImageUrl).isNull()
        assertThat(member.profileImageUrl).isNull()

        verify(exactly = 0) {
            memberRepository.existsByNicknameAndIdNotAndDeletedAtIsNull(any(), any())
        }

    }

    @Test
    @DisplayName("빈 프로필 이미지 URL은 null로 저장한다")
    fun updateProfileWithBlankProfileImageUrl() {
        val member = createMember(
            id = 1L,
            nickname = "old_nickname",
            profileImageUrl = "old_profile_img",
        )
        val request = createRequest(
            nickname = "new_nickname",
            profileImageUrl = "   ",
        )

        every { memberRepository.findByIdAndStatusAndDeletedAtIsNull(1L) } returns member
        every { memberRepository.existsByNicknameAndIdNotAndDeletedAtIsNull("new_nickname", 1L) } returns false

        val response = memberProfileService.updateProfile(1L, request)

        assertThat(response.profileImageUrl).isNull()
        assertThat(member.profileImageUrl).isNull()

    }

    @Test
    @DisplayName("다른 회원이 사용 중인 닉네임이면 예외가 발생한다")
    fun updateProfileWithDuplicatedNickname() {
        val member = createMember(
            id = 1L,
            nickname = "old_nickname",
            profileImageUrl = "old_profile_img",
        )
        val request = createRequest(nickname = "duplicated")

        every { memberRepository.findByIdAndStatusAndDeletedAtIsNull(1L) } returns member
        every { memberRepository.existsByNicknameAndIdNotAndDeletedAtIsNull("duplicated", 1L) } returns true

        assertThatThrownBy {
            memberProfileService.updateProfile(1L, request)
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(MemberErrorCode.NICKNAME_ALREADY_EXISTS)

        assertThat(member.nickname).isEqualTo("old_nickname")
        assertThat(member.profileImageUrl).isEqualTo("old_profile_img")
    }

    @Test
    @DisplayName("수정 가능한 회원이 아니면 예외가 발생한다")
    fun updateProfileWithForbiddenMember() {
        val request = createRequest()

        every { memberRepository.findByIdAndStatusAndDeletedAtIsNull(1L)} returns null

        assertThatThrownBy {
            memberProfileService.updateProfile(1L, request)
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(MemberErrorCode.MEMBER_LOGIN_FORBIDDEN)

        verify(exactly = 0) {
            memberRepository.existsByNicknameAndIdNotAndDeletedAtIsNull(any(), any())
        }
    }

    @Test
    @DisplayName("조회된 회원 ID가 없으면 예외가 발생한다")
    fun updateProfileWithMemberWithoutId() {
        val member = createMember(
            id = null,
            nickname = "old_nickname",
            profileImageUrl = "old_profile_img",
        )
        val request = createRequest()

        every { memberRepository.findByIdAndStatusAndDeletedAtIsNull(1L) } returns member

        assertThatThrownBy {
            memberProfileService.updateProfile(1L, request)
        }.isInstanceOf(IllegalArgumentException::class.java)

        assertThat(member.nickname).isEqualTo("old_nickname")
        assertThat(member.profileImageUrl).isEqualTo("old_profile_img")

        verify(exactly = 0) {
            memberRepository.existsByNicknameAndIdNotAndDeletedAtIsNull(any(), any())
        }
    }

    private fun createRequest(
        nickname: String = "new_nickname",
        profileImageUrl: String? = "new_profile_img"
    ): MemberProfileUpdateRequest {
        return MemberProfileUpdateRequest(
            nickname, profileImageUrl
        )
    }

    private fun createMember(
        id: Long? = 1L,
        nickname: String = "unit_user",
        profileImageUrl: String? = "profile_img",
        status: MemberStatus = MemberStatus.ACTIVE,
    ): Member {
        return Member(
            id = id,
            emailHash = ByteArray(32) { 1 },
            passwordHash = "encoded-password",
            nickname = nickname,
            profileImageUrl = profileImageUrl,
            status = status,
        )
    }
}
