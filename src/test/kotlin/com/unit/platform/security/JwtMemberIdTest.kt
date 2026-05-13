package com.unit.platform.security

import com.unit.platform.error.BusinessException
import com.unit.platform.error.CommonErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.springframework.security.oauth2.jwt.Jwt
import kotlin.test.Test

@DisplayName("Jwt 인증 회원 ID 추출 테스트")
class JwtMemberIdTest {

    @Test
    @DisplayName("JWT subject에서 회원 ID를 추출한다")
    fun memberId() {
        val jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("1")
            .build()

        assertThat(jwt.memberId()).isEqualTo(1L)
    }

    @Test
    @DisplayName("JWT subject가 숫자가 아니면 INVALID_TOKEN 예외가 발생한다")
    fun invalidSubject() {
        val jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("invalid")
            .build()

        assertThatThrownBy { jwt.memberId() }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_TOKEN)
    }
}
