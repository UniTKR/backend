package com.unit.member.service

import com.unit.member.dto.MemberAvailabilityResponse
import com.unit.member.repository.MemberRepository
import com.unit.member.util.EmailHasher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberAvailabilityService(
    private val memberRepository: MemberRepository,
    private val emailHasher: EmailHasher,
) : MemberAvailabilityUseCase {

    override fun checkEmailAvailability(email: String): MemberAvailabilityResponse {
        val normalizedEmail = email.trim().lowercase()
        val emailHash = emailHasher.hash(normalizedEmail)

        return MemberAvailabilityResponse(
            available = !memberRepository.existsByEmailHashAndDeletedAtIsNull(emailHash),
        )
    }

    override fun checkNicknameAvailability(nickname: String): MemberAvailabilityResponse {
        val normalizedNickname = nickname.trim()

        return MemberAvailabilityResponse(
            available = !memberRepository.existsByNicknameAndDeletedAtIsNull(normalizedNickname),
        )
    }
}
