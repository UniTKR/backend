package com.unit.member.security

import com.unit.member.enums.MemberStatus
import com.unit.member.repository.MemberRepository
import com.unit.platform.security.JwtAuthenticationValidator
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
class MemberStatusJwtAuthenticationValidator(
    private val memberRepository: MemberRepository,
) : JwtAuthenticationValidator {

    override fun isValid(jwt: Jwt): Boolean {
        val memberId = jwt.subject.toLongOrNull() ?: return false

        return memberRepository.existsByIdAndStatusInAndDeletedAtIsNull(
            id = memberId,
            statuses = listOf(MemberStatus.PENDING, MemberStatus.ACTIVE),
        )
    }
}
