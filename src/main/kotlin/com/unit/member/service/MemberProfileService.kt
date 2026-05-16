package com.unit.member.service

import com.unit.member.dto.MemberProfileUpdateRequest
import com.unit.member.dto.MemberProfileUpdateResponse
import com.unit.member.exception.MemberErrorCode
import com.unit.member.repository.MemberRepository
import com.unit.platform.error.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MemberProfileService(
    private val memberRepository: MemberRepository
) : MemberProfileUseCase {

    override fun updateProfile(
        memberId: Long,
        request: MemberProfileUpdateRequest
    ): MemberProfileUpdateResponse {

        val member = memberRepository.findByIdAndStatusAndDeletedAtIsNull(memberId)
            ?: throw BusinessException(MemberErrorCode.MEMBER_LOGIN_FORBIDDEN)

        val normalizedNickname = request.nickname.trim()
        val normalizedProfileImageUrl = request.profileImageUrl?.trim()?.takeIf { it.isNotBlank() }

        val currentMemberId = requireNotNull(member.id)

        if (
            member.nickname != normalizedNickname &&
            memberRepository.existsByNicknameAndIdNotAndDeletedAtIsNull(
                nickname = normalizedNickname,
                id = currentMemberId,
            )
        ) {
            throw BusinessException(MemberErrorCode.NICKNAME_ALREADY_EXISTS)
        }

        member.updateProfile(
            nickname = normalizedNickname,
            profileImageUrl = normalizedProfileImageUrl,
        )

        return MemberProfileUpdateResponse(
            memberId = currentMemberId,
            nickname = member.nickname,
            profileImageUrl = member.profileImageUrl,
        )
    }
}