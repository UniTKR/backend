package com.unit.member.security

import com.unit.member.enums.MemberStatus
import com.unit.member.repository.MemberRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.springframework.security.oauth2.jwt.Jwt
import kotlin.test.Test

@DisplayName("회원 상태 JWT 검증기 테스트")
class MemberStatusJwtAuthenticationValidatorTest {

    private val memberRepository = mockk<MemberRepository>()
    private val validator = MemberStatusJwtAuthenticationValidator(memberRepository)

    @Test
    @DisplayName("JWT subject의 회원이 활성 상태이면 true를 반환한다")
    fun validMember() {
        val jwt = createJwt(subject = "1")

        every {
            memberRepository.existsByIdAndStatusInAndDeletedAtIsNull(
                id = 1L,
                statuses = listOf(MemberStatus.PENDING, MemberStatus.ACTIVE),
            )
        } returns true

        val result = validator.isValid(jwt)

        assertThat(result).isTrue()
    }

    @Test
    @DisplayName("JWT subject의 회원이 유효하지 않으면 false를 반환한다")
    fun invalidMember() {
        val jwt = createJwt(subject = "1")

        every {
            memberRepository.existsByIdAndStatusInAndDeletedAtIsNull(
                id = 1L,
                statuses = listOf(MemberStatus.PENDING, MemberStatus.ACTIVE),
            )
        } returns false

        val result = validator.isValid(jwt)

        assertThat(result).isFalse()
    }

    @Test
    @DisplayName("JWT subject가 숫자가 아니면 false를 반환하고 Repository를 호출하지 않는다")
    fun invalidSubject() {
        val jwt = createJwt(subject = "invalid")

        val result = validator.isValid(jwt)

        assertThat(result).isFalse()
        verify(exactly = 0) {
            memberRepository.existsByIdAndStatusInAndDeletedAtIsNull(any(), any())
        }
    }

    private fun createJwt(subject: String): Jwt {
        return Jwt.withTokenValue("access-token")
            .header("alg", "none")
            .subject(subject)
            .build()
    }

}