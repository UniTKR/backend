package com.unit.member.service

import com.unit.member.dto.SchoolAuthDto.*
import com.unit.member.entity.SchoolEmailVerificationCode
import com.unit.member.enums.MemberStatus
import com.unit.member.enums.SchoolEmailVerificationPurpose
import com.unit.member.enums.SchoolEmailVerificationStatus
import com.unit.member.enums.UserSchoolVerificationStatus
import com.unit.member.exception.MemberErrorCode
import com.unit.member.repository.MemberRepository
import com.unit.member.repository.SchoolEmailDomainRepository
import com.unit.member.repository.SchoolEmailVerificationCodeRepository
import com.unit.member.repository.SchoolRepository
import com.unit.member.repository.UserSchoolVerificationRepository
import com.unit.member.util.EmailHasher
import com.unit.member.util.SchoolEmailVerificationCodeGenerator
import com.unit.member.util.TokenHasher
import com.unit.platform.error.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class SchoolEmailVerificationService(
    private val schoolRepository: SchoolRepository,
    private val schoolEmailDomainRepository: SchoolEmailDomainRepository,
    private val schoolEmailVerificationCodeRepository: SchoolEmailVerificationCodeRepository,
    private val userSchoolVerificationRepository: UserSchoolVerificationRepository,
    private val memberRepository: MemberRepository,
    private val emailHasher: EmailHasher,
    private val tokenHasher: TokenHasher,
    private val codeGenerator: SchoolEmailVerificationCodeGenerator,
) : SchoolEmailVerificationUseCase {

    private val verificationExpiresInSeconds = 300L

    override fun request(
        memberId: Long,
        request: SchoolEmailVerificationRequest
    ): SchoolEmailVerificationResponse {
        val schoolId = requireNotNull(request.schoolId)
        schoolRepository.findByIdAndStatus(schoolId)
            ?: throw BusinessException(MemberErrorCode.SCHOOL_NOT_FOUND)

        val email = request.email.trim().lowercase()
        val domain = extractDomain(email)

        if (!schoolEmailDomainRepository.existsBySchoolIdAndDomainAndStatus(schoolId, domain)) {
            throw BusinessException(MemberErrorCode.SCHOOL_EMAIL_DOMAIN_NOT_ALLOWED)
        }

        val emailHash = emailHasher.hash(email)
        val now = LocalDateTime.now()

        schoolEmailVerificationCodeRepository
            .findTopBySchoolIdAndEmailHashAndStatusOrderByCreatedAtDesc(
                schoolId = schoolId,
                emailHash = emailHash,
                status = SchoolEmailVerificationStatus.PENDING,
            )
            ?.cancel()

        val code = codeGenerator.generate()

        schoolEmailVerificationCodeRepository.save(
            SchoolEmailVerificationCode(
                memberId = memberId,
                schoolId = schoolId,
                emailHash = emailHash,
                codeHash = tokenHasher.hash(code),
                purpose = SchoolEmailVerificationPurpose.SCHOOL_SIGNUP,
                expiresAt = now.plusSeconds(verificationExpiresInSeconds),
                status = SchoolEmailVerificationStatus.PENDING,
            )
        )

        // TODO: platform notification/email sender로 교체
        println("[SchoolEmailVerification] email=$email code=$code")

        return SchoolEmailVerificationResponse(
            schoolId = schoolId,
            email = email,
            expiresIn = verificationExpiresInSeconds,
        )
    }

    override fun confirm(
        memberId: Long,
        request: SchoolEmailVerificationConfirmRequest
    ): SchoolEmailVerificationConfirmResponse {
        val schoolId = requireNotNull(request.schoolId)
        val email = request.email.trim().lowercase()
        val code = request.code.trim()
        val now = LocalDateTime.now()
        val emailHash = emailHasher.hash(email)

        val verificationCode = (schoolEmailVerificationCodeRepository
            .findTopBySchoolIdAndEmailHashAndStatusOrderByCreatedAtDesc(
                schoolId = schoolId,
                emailHash = emailHash,
                status = SchoolEmailVerificationStatus.PENDING,
            )
            ?: throw BusinessException(MemberErrorCode.SCHOOL_EMAIL_VERIFICATION_CODE_NOT_FOUND))

        if (!verificationCode.memberIdEquals(memberId)) {
            throw BusinessException(MemberErrorCode.SCHOOL_EMAIL_VERIFICATION_CODE_NOT_FOUND)
        }

        if (!verificationCode.expiresAt.isAfter(now)) {
            verificationCode.expire()
            throw BusinessException(MemberErrorCode.SCHOOL_EMAIL_VERIFICATION_CODE_EXPIRED)
        }

        verificationCode.increaseAttempt()

        if (!tokenHasher.matches(code, verificationCode.codeHash)) {
            throw BusinessException(MemberErrorCode.SCHOOL_EMAIL_VERIFICATION_CODE_MISMATCHED)
        }

        verificationCode.verify(now)

        val schoolVerification = userSchoolVerificationRepository.findByMemberIdAndSchoolId(
            memberId = memberId,
            schoolId = schoolId,
        ) ?: throw BusinessException(MemberErrorCode.SCHOOL_VERIFICATION_NOT_FOUND)

        schoolVerification.status = UserSchoolVerificationStatus.VERIFIED
        schoolVerification.verifiedAt = now

        val member = memberRepository.findByIdAndStatusAndDeletedAtIsNull(
            id = memberId,
            status = MemberStatus.PENDING,
        ) ?: throw BusinessException(MemberErrorCode.MEMBER_LOGIN_FORBIDDEN)

        member.activate()

        return SchoolEmailVerificationConfirmResponse(
            schoolId = schoolId,
            status = schoolVerification.status,
        )
    }

    private fun extractDomain(email: String): String {
        return email.substringAfter("@", missingDelimiterValue = "")
    }

    private fun SchoolEmailVerificationCode.memberIdEquals(memberId: Long): Boolean {
        return this.memberId == memberId
    }
}