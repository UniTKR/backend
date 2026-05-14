package com.unit.member.service

import com.unit.member.repository.MemberRepository
import com.unit.member.util.EmailHasher
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test

@DisplayName("회원 사용 가능 여부 서비스 테스트")
class MemberAvailabilityServiceTest {

    private val memberRepository = mockk<MemberRepository>()
    private val emailHasher = mockk<EmailHasher>()

    private val memberAvailabilityService = MemberAvailabilityService(
        memberRepository = memberRepository,
        emailHasher = emailHasher,
    )

    @Test
    @DisplayName("가입된 이메일이 없으면 사용 가능하다")
    fun checkEmailAvailabilityWithAvailableEmail() {
        val emailHash = ByteArray(32) { 1 }

        every { emailHasher.hash("test@unit.com") } returns emailHash
        every { memberRepository.existsByEmailHashAndDeletedAtIsNull(emailHash) } returns false

        val response = memberAvailabilityService.checkEmailAvailability(" Test@Unit.COM ")

        assertThat(response.available).isTrue()

        verify(exactly = 1) { emailHasher.hash("test@unit.com") }
        verify(exactly = 1) { memberRepository.existsByEmailHashAndDeletedAtIsNull(emailHash) }
    }

    @Test
    @DisplayName("가입된 이메일이 있으면 사용 불가능하다")
    fun checkEmailAvailabilityWithDuplicatedEmail() {
        val emailHash = ByteArray(32) { 1 }

        every { emailHasher.hash("test@unit.com") } returns emailHash
        every { memberRepository.existsByEmailHashAndDeletedAtIsNull(emailHash) } returns true

        val response = memberAvailabilityService.checkEmailAvailability("test@unit.com")

        assertThat(response.available).isFalse()

        verify(exactly = 1) { emailHasher.hash("test@unit.com") }
        verify(exactly = 1) { memberRepository.existsByEmailHashAndDeletedAtIsNull(emailHash) }
    }

    @Test
    @DisplayName("가입된 닉네임이 없으면 사용 가능하다")
    fun checkNicknameAvailabilityWithAvailableNickname() {
        every { memberRepository.existsByNicknameAndDeletedAtIsNull("unit_user") } returns false

        val response = memberAvailabilityService.checkNicknameAvailability(" unit_user ")

        assertThat(response.available).isTrue()

        verify(exactly = 1) { memberRepository.existsByNicknameAndDeletedAtIsNull("unit_user") }
    }

    @Test
    @DisplayName("가입된 닉네임이 있으면 사용 불가능하다")
    fun checkNicknameAvailabilityWithDuplicatedNickname() {
        every { memberRepository.existsByNicknameAndDeletedAtIsNull("unit_user") } returns true

        val response = memberAvailabilityService.checkNicknameAvailability("unit_user")

        assertThat(response.available).isFalse()

        verify(exactly = 1) { memberRepository.existsByNicknameAndDeletedAtIsNull("unit_user") }
    }
}
