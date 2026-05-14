package com.unit.member.service

import com.unit.member.config.MemberConsentProperties
import com.unit.member.dto.MemberSignupRequest
import com.unit.member.dto.MemberSignupResponse
import com.unit.member.entity.Member
import com.unit.member.entity.MemberConsent
import com.unit.member.entity.UserSchoolVerification
import com.unit.member.enums.MemberConsentType
import com.unit.member.enums.MemberStatus
import com.unit.member.enums.UserSchoolVerificationMethod
import com.unit.member.enums.UserSchoolVerificationStatus
import com.unit.member.exception.MemberErrorCode
import com.unit.member.repository.MemberConsentRepository
import com.unit.member.repository.MemberRepository
import com.unit.member.repository.SchoolRepository
import com.unit.member.repository.UserSchoolVerificationRepository
import com.unit.member.util.EmailHasher
import com.unit.platform.error.BusinessException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class MemberSignupService(
    private val memberRepository: MemberRepository,
    private val schoolRepository: SchoolRepository,
    private val userSchoolVerificationRepository: UserSchoolVerificationRepository,
    private val passwordEncoder: PasswordEncoder,
    private val emailHasher: EmailHasher,
    private val memberConsentRepository: MemberConsentRepository,
    private val memberConsentProperties: MemberConsentProperties,
) : MemberSignupUseCase {

    override fun signup(request: MemberSignupRequest): MemberSignupResponse {
        val emailHash = emailHasher.hash(request.email)
        val nickname = request.nickname.trim()
        val schoolId = request.schoolId!!

        if (memberRepository.existsByEmailHashAndDeletedAtIsNull(emailHash)) {
            throw BusinessException(MemberErrorCode.EMAIL_ALREADY_EXISTS)
        }

        if (memberRepository.existsByNicknameAndDeletedAtIsNull(nickname)) {
            throw BusinessException(MemberErrorCode.NICKNAME_ALREADY_EXISTS)
        }

        if (!schoolRepository.existsByIdAndStatus(schoolId)) {
            throw BusinessException(MemberErrorCode.SCHOOL_NOT_FOUND)
        }

        val member = memberRepository.save(
            Member(
                emailHash = emailHash,
                passwordHash = passwordEncoder.encode(request.password),
                nickname = nickname,
                status = MemberStatus.PENDING,
            ),
        )

        val now = LocalDateTime.now()
        val memberId = member.id!!

        memberConsentRepository.saveAll(
            listOf(
                MemberConsent(
                    memberId = memberId,
                    consentType = MemberConsentType.TERMS_OF_SERVICE,
                    policyVersion = memberConsentProperties.termsVersion,
                    agreed = true,
                    agreedAt = now,
                ),
                MemberConsent(
                    memberId = memberId,
                    consentType = MemberConsentType.PRIVACY_POLICY,
                    policyVersion = memberConsentProperties.privacyPolicyVersion,
                    agreed = true,
                    agreedAt = now,
                ),
            ),
        )

        userSchoolVerificationRepository.save(
            UserSchoolVerification(
                memberId = memberId,
                schoolId = schoolId,
                method = UserSchoolVerificationMethod.EMAIL,
                status = UserSchoolVerificationStatus.PENDING,
            ),
        )

        return MemberSignupResponse(
            memberId = memberId,
            nickname = member.nickname,
            status = member.status,
            schoolVerificationStatus = UserSchoolVerificationStatus.PENDING,
        )
    }
}
