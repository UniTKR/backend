package com.unit.member.entity

import com.unit.member.enums.UserSchoolVerificationMethod
import com.unit.member.enums.UserSchoolVerificationStatus
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.BeforeTest

@DisplayName("UserSchoolVerification 엔티티 테스트")
class UserSchoolVerificationTest {

    lateinit var userSchoolVerification: UserSchoolVerification

    @BeforeTest
    fun setUp() {
        userSchoolVerification = UserSchoolVerification(
            memberId = 1L,
            schoolId = 1L,
            method = UserSchoolVerificationMethod.EMAIL
        )
    }

    @Test
    @DisplayName("revoke() 호출")
    fun revoke() {
        userSchoolVerification.revoke()

        Assertions.assertThat(userSchoolVerification.status).isEqualTo(UserSchoolVerificationStatus.REVOKED)
    }

    @Test
    @DisplayName("expire() 호출")
    fun expire() {
        userSchoolVerification.expire()

        Assertions.assertThat(userSchoolVerification.status).isEqualTo(UserSchoolVerificationStatus.EXPIRED)
    }

}