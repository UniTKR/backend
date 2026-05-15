package com.unit.member.service

import com.unit.member.config.SchoolEmailVerificationProperties
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
import com.unit.member.util.EmailEncryptor
import com.unit.member.util.EmailHasher
import com.unit.member.util.SchoolEmailVerificationCodeGenerator
import com.unit.member.util.SchoolEmailVerificationFailureRecorder
import com.unit.member.util.TokenHasher
import com.unit.platform.error.BusinessException
import com.unit.platform.mail.EmailMessage
import com.unit.platform.mail.EmailSender
import com.unit.platform.mail.EmailTemplateRenderer
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.LocalDateTime
import kotlin.math.max

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
    private val failureRecorder: SchoolEmailVerificationFailureRecorder,
    private val emailSender: EmailSender,
    private val emailTemplateRenderer: EmailTemplateRenderer,
    private val emailEncryptor: EmailEncryptor,
    private val properties: SchoolEmailVerificationProperties,
) : SchoolEmailVerificationUseCase {

    override fun request(
        memberId: Long,
        request: SchoolEmailVerificationRequest
    ): SchoolEmailVerificationResponse {
        val schoolId = requireNotNull(request.schoolId)
        schoolRepository.findByIdAndStatus(schoolId)
            ?: throw BusinessException(MemberErrorCode.SCHOOL_NOT_FOUND)

        val hasPendingSchoolVerification =
            userSchoolVerificationRepository.existsByMemberIdAndSchoolIdAndStatus(
                memberId = memberId,
                schoolId = schoolId,
                status = UserSchoolVerificationStatus.PENDING,
            )

        if (!hasPendingSchoolVerification) {
            throw BusinessException(MemberErrorCode.SCHOOL_VERIFICATION_NOT_FOUND)
        }

        val email = request.email.trim().lowercase()
        val domain = extractDomain(email)

        if (!schoolEmailDomainRepository.existsBySchoolIdAndDomainAndStatus(schoolId, domain)) {
            throw BusinessException(MemberErrorCode.SCHOOL_EMAIL_DOMAIN_NOT_ALLOWED)
        }

        val emailHash = emailHasher.hash(email)
        val now = LocalDateTime.now()

        handlePendingVerificationCodeForResend(
            memberId = memberId,
            now = now,
        )

        val code = codeGenerator.generate()

        schoolEmailVerificationCodeRepository.save(
            SchoolEmailVerificationCode(
                memberId = memberId,
                schoolId = schoolId,
                emailHash = emailHash,
                codeHash = tokenHasher.hash(code),
                purpose = SchoolEmailVerificationPurpose.SCHOOL_SIGNUP,
                expiresAt = now.plusSeconds(properties.codeExpirationSeconds),
                status = SchoolEmailVerificationStatus.PENDING,
            )
        )

        val html = emailTemplateRenderer.render(
            templatePath = "mail/school-email-verification.html",
            variables = mapOf(
                "code" to code,
                "expiresInMinutes" to (properties.codeExpirationSeconds / 60).toString(),
            ),
        )

        val message = EmailMessage(
            to = email,
            subject = "[UniT] 학교 이메일 인증 코드",
            body = html,
            html = true,
        )

        registerAfterCommit {
            emailSender.send(message)
        }

        return SchoolEmailVerificationResponse(
            schoolId = schoolId,
            email = email,
            expiresIn = properties.codeExpirationSeconds,
        )
    }

    private fun handlePendingVerificationCodeForResend(memberId: Long, now: LocalDateTime) {
        val latestPendingCode =
            schoolEmailVerificationCodeRepository.findTopByMemberIdAndStatusOrderByCreatedAtDesc(
                memberId = memberId,
                status = SchoolEmailVerificationStatus.PENDING,
            )

        latestPendingCode?.let { existingCode ->
            if (!existingCode.expiresAt.isAfter(now)) {
                existingCode.expire()
                return
            }

            if (existingCode.isInCooldown(now, properties.resendCooldownSeconds)) {
                throw BusinessException(MemberErrorCode.SCHOOL_EMAIL_VERIFICATION_COOLDOWN)
            }

            existingCode.cancel()
        }
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

        val verificationCodeId = requireNotNull(verificationCode.id)

        if (!verificationCode.expiresAt.isAfter(now)) {
            failureRecorder.expire(verificationCodeId)
            throw BusinessException(MemberErrorCode.SCHOOL_EMAIL_VERIFICATION_CODE_EXPIRED)
        }

        if (!tokenHasher.matches(code, verificationCode.codeHash)) {
            val attemptLimitExceeded = failureRecorder.recordMismatch(
                id = verificationCodeId,
                maxAttemptCount = properties.maxAttemptCount
            )

            if (attemptLimitExceeded) {
                throw BusinessException(MemberErrorCode.SCHOOL_EMAIL_VERIFICATION_ATTEMPT_LIMIT_EXCEEDED)
            }

            throw BusinessException(MemberErrorCode.SCHOOL_EMAIL_VERIFICATION_CODE_MISMATCHED)
        }

        val schoolVerification = userSchoolVerificationRepository.findByMemberIdAndSchoolId(
            memberId = memberId,
            schoolId = schoolId,
        ) ?: throw BusinessException(MemberErrorCode.SCHOOL_VERIFICATION_NOT_FOUND)

        val member = memberRepository.findByIdAndStatusAndDeletedAtIsNull(
            id = memberId,
            status = MemberStatus.PENDING,
        ) ?: throw BusinessException(MemberErrorCode.MEMBER_LOGIN_FORBIDDEN)

        verificationCode.verify(now)
        schoolVerification.verifyByEmail(
            now = now,
            emailHash = emailHash,
            emailEncrypted = emailEncryptor.encrypt(email),
            expiresAt = now.plusDays(properties.verificationExpirationDays),
        )
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

    private fun registerAfterCommit(action: () -> Unit) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action()
            return
        }

        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    action()
                }
            },
        )
    }

}
