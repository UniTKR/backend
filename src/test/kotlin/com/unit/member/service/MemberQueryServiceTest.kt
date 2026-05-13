package com.unit.member.service

import com.unit.member.entity.Member
import com.unit.member.entity.School
import com.unit.member.entity.UserSchoolVerification
import com.unit.member.enums.MemberStatus
import com.unit.member.enums.SchoolStatus
import com.unit.member.enums.UserSchoolVerificationMethod
import com.unit.member.enums.UserSchoolVerificationStatus
import com.unit.member.exception.MemberErrorCode
import com.unit.member.repository.MemberRepository
import com.unit.member.repository.SchoolRepository
import com.unit.member.repository.UserSchoolVerificationRepository
import com.unit.platform.error.BusinessException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import java.util.*
import kotlin.test.Test

@DisplayName("MemberQueryService 테스트")
class MemberQueryServiceTest {

    private val memberRepository = mockk<MemberRepository>()
    private val userSchoolVerificationRepository = mockk<UserSchoolVerificationRepository>()
    private val schoolRepository = mockk<SchoolRepository>()

    private val memberQueryService = MemberQueryService(
        memberRepository = memberRepository,
        userSchoolVerificationRepository = userSchoolVerificationRepository,
        schoolRepository = schoolRepository,
    )

    @Test
    @DisplayName("내 정보 조회 정상 응답")
    fun getMe() {

        val member = createMember()
        val schoolVerification = createUserSchoolVerification()
        val school = createSchool()

        every { memberRepository.findByIdAndStatusInAndDeletedAtIsNull(id = 1L, statuses = listOf(MemberStatus.PENDING, MemberStatus.ACTIVE)) } returns member
        every { userSchoolVerificationRepository.findByMemberId(1L) } returns schoolVerification
        every { schoolRepository.findById(1L) } returns Optional.of(school)

        val response = memberQueryService.getMe(1L)

        assertThat(response.memberId).isEqualTo(1L)
        assertThat(response.nickname).isEqualTo("unit_user")
        assertThat(response.profileImageUrl).isEqualTo("profile_image_url")
        assertThat(response.status).isEqualTo(MemberStatus.ACTIVE)
        assertThat(response.trustScore).isEqualTo(100)
        assertThat(response.school!!.schoolId).isEqualTo(1L)
        assertThat(response.school.name).isEqualTo("Unit_University")
        assertThat(response.school.verificationStatus).isEqualTo(UserSchoolVerificationStatus.VERIFIED)

        verify(exactly = 1) {
            memberRepository.findByIdAndStatusInAndDeletedAtIsNull(
                id = 1L,
                statuses = listOf(MemberStatus.PENDING, MemberStatus.ACTIVE),
            )
        }
        verify(exactly = 1) { userSchoolVerificationRepository.findByMemberId(1L) }
        verify(exactly = 1) { schoolRepository.findById(1L) }
    }

    @Test
    @DisplayName("대기, 활성 중이 아니면 예외 발생")
    fun getMeInvalidStatus() {
        val schoolVerification = createUserSchoolVerification()
        val school = createSchool()

        every { memberRepository.findByIdAndStatusInAndDeletedAtIsNull(id = 1L, statuses = listOf(MemberStatus.PENDING, MemberStatus.ACTIVE)) } returns null
        every { userSchoolVerificationRepository.findByMemberId(1L) } returns schoolVerification
        every { schoolRepository.findById(1L) } returns Optional.of(school)

        assertThatThrownBy {
            memberQueryService.getMe(1L)
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(MemberErrorCode.MEMBER_LOGIN_FORBIDDEN)

        verify(exactly = 1) {
            memberRepository.findByIdAndStatusInAndDeletedAtIsNull(
                id = 1L,
                statuses = listOf(MemberStatus.PENDING, MemberStatus.ACTIVE),
            )
        }
        verify(exactly = 0) { userSchoolVerificationRepository.findByMemberId(1L) }
        verify(exactly = 0) { schoolRepository.findById(1L) }
    }

    @Test
    @DisplayName("학교 인증이 안되면 학교 정보는 응답에 포함되지 않는다.")
    fun notVerifiedUser() {
        val member = createMember()

        every { memberRepository.findByIdAndStatusInAndDeletedAtIsNull(id = 1L, statuses = listOf(MemberStatus.PENDING, MemberStatus.ACTIVE)) } returns member
        every { userSchoolVerificationRepository.findByMemberId(1L) } returns null

        val response = memberQueryService.getMe(1L)

        assertThat(response.memberId).isEqualTo(1L)
        assertThat(response.nickname).isEqualTo("unit_user")
        assertThat(response.profileImageUrl).isEqualTo("profile_image_url")
        assertThat(response.status).isEqualTo(MemberStatus.ACTIVE)
        assertThat(response.trustScore).isEqualTo(100)
        assertThat(response.school).isNull()

        verify(exactly = 1) {
            memberRepository.findByIdAndStatusInAndDeletedAtIsNull(
                id = 1L,
                statuses = listOf(MemberStatus.PENDING, MemberStatus.ACTIVE),
            )
        }
        verify(exactly = 1) { userSchoolVerificationRepository.findByMemberId(1L) }
        verify(exactly = 0) { schoolRepository.findById(1L) }
    }

    @Test
    @DisplayName("학교 정보가 없으면 예외가 발생한다.")
    fun noSchoolInfo() {
        val member = createMember()
        val schoolVerification = createUserSchoolVerification()

        every { memberRepository.findByIdAndStatusInAndDeletedAtIsNull(id = 1L, statuses = listOf(MemberStatus.PENDING, MemberStatus.ACTIVE)) } returns member
        every { userSchoolVerificationRepository.findByMemberId(1L) } returns schoolVerification
        every { schoolRepository.findById(1L) } returns Optional.empty()

        assertThatThrownBy {
            memberQueryService.getMe(1L)
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(MemberErrorCode.SCHOOL_NOT_FOUND)

        verify(exactly = 1) {
            memberRepository.findByIdAndStatusInAndDeletedAtIsNull(
                id = 1L,
                statuses = listOf(MemberStatus.PENDING, MemberStatus.ACTIVE),
            )
        }
        verify(exactly = 1) { userSchoolVerificationRepository.findByMemberId(1L) }
        verify(exactly = 1) { schoolRepository.findById(1L) }
    }

    @Test
    @DisplayName("조회된 회원 ID가 없으면 예외가 발생한다")
    fun getMeWithNullMemberId() {
        val member = createMember(id = null)

        every {
            memberRepository.findByIdAndStatusInAndDeletedAtIsNull(
                id = 1L,
                statuses = listOf(MemberStatus.PENDING, MemberStatus.ACTIVE),
            )
        } returns member
        every { userSchoolVerificationRepository.findByMemberId(1L) } returns null

        assertThatThrownBy {
            memberQueryService.getMe(1L)
        }.isInstanceOf(IllegalArgumentException::class.java)

        verify(exactly = 1) {
            memberRepository.findByIdAndStatusInAndDeletedAtIsNull(
                id = 1L,
                statuses = listOf(MemberStatus.PENDING, MemberStatus.ACTIVE),
            )
        }
        verify(exactly = 1) { userSchoolVerificationRepository.findByMemberId(1L) }
        verify(exactly = 0) { schoolRepository.findById(any()) }
    }

    @Test
    @DisplayName("조회된 학교 ID가 없으면 예외가 발생한다")
    fun getMeWithNullSchoolId() {
        val member = createMember()
        val schoolVerification = createUserSchoolVerification()
        val school = createSchool(id = null)

        every {
            memberRepository.findByIdAndStatusInAndDeletedAtIsNull(
                id = 1L,
                statuses = listOf(MemberStatus.PENDING, MemberStatus.ACTIVE),
            )
        } returns member
        every { userSchoolVerificationRepository.findByMemberId(1L) } returns schoolVerification
        every { schoolRepository.findById(1L) } returns Optional.of(school)

        assertThatThrownBy {
            memberQueryService.getMe(1L)
        }.isInstanceOf(IllegalArgumentException::class.java)

        verify(exactly = 1) {
            memberRepository.findByIdAndStatusInAndDeletedAtIsNull(
                id = 1L,
                statuses = listOf(MemberStatus.PENDING, MemberStatus.ACTIVE),
            )
        }
        verify(exactly = 1) { userSchoolVerificationRepository.findByMemberId(1L) }
        verify(exactly = 1) { schoolRepository.findById(1L) }
    }



    private fun createMember(
        id: Long? = 1L,
        nickname: String = "unit_user",
        profileImageUrl: String = "profile_image_url",
        trustScore: Int = 100,
        status: MemberStatus = MemberStatus.ACTIVE
    ): Member {
        return Member(
            id = id,
            nickname = nickname,
            profileImageUrl = profileImageUrl,
            trustScore = trustScore,
            status = status
        )
    }

    private fun createUserSchoolVerification(
        memberId: Long = 1L,
        schoolId: Long = 1L,
        method: UserSchoolVerificationMethod = UserSchoolVerificationMethod.EMAIL,
        status: UserSchoolVerificationStatus = UserSchoolVerificationStatus.VERIFIED,
    ): UserSchoolVerification {
        return UserSchoolVerification(
            memberId = memberId,
            schoolId = schoolId,
            method = method,
            status = status,
        )
    }

    private fun createSchool(
        id: Long? = 1L,
        name: String = "Unit_University",
        status: SchoolStatus = SchoolStatus.ACTIVE,
    ): School {
        return School(
            id = id,
            name = name,
            status = status,
        )
    }
}