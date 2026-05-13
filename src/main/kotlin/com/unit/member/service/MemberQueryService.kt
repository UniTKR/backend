package com.unit.member.service

import com.unit.member.dto.MemberMeResponse
import com.unit.member.dto.MemberSchoolResponse
import com.unit.member.enums.MemberStatus
import com.unit.member.exception.MemberErrorCode
import com.unit.member.repository.MemberRepository
import com.unit.member.repository.SchoolRepository
import com.unit.member.repository.UserSchoolVerificationRepository
import com.unit.platform.error.BusinessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberQueryService(
    private val memberRepository: MemberRepository,
    private val userSchoolVerificationRepository: UserSchoolVerificationRepository,
    private val schoolRepository: SchoolRepository,
) : MemberQueryUseCase {

    override fun getMe(memberId: Long): MemberMeResponse {
        val member = memberRepository.findByIdAndStatusInAndDeletedAtIsNull(
            id = memberId,
            statuses = listOf(MemberStatus.PENDING, MemberStatus.ACTIVE)
        ) ?: throw BusinessException(MemberErrorCode.MEMBER_LOGIN_FORBIDDEN)

        val schoolVerification = userSchoolVerificationRepository.findByMemberId(memberId)
        val schoolResponse = schoolVerification?.let {
            val school = schoolRepository.findByIdOrNull(it.schoolId)
                ?: throw BusinessException(MemberErrorCode.SCHOOL_NOT_FOUND)

            MemberSchoolResponse(
                schoolId = requireNotNull(school.id),
                name = school.name,
                verificationStatus = it.status,
            )
        }

        return MemberMeResponse(
            memberId = requireNotNull(member.id),
            nickname = member.nickname,
            profileImageUrl = member.profileImageUrl,
            status = member.status,
            trustScore = member.trustScore,
            school = schoolResponse
        )
    }
}