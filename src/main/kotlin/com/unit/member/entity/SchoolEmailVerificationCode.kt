package com.unit.member.entity

import com.unit.member.enums.SchoolEmailVerificationPurpose
import com.unit.member.enums.SchoolEmailVerificationStatus
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "school_email_verification_codes")
class SchoolEmailVerificationCode(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id")
    val memberId: Long? = null,

    @Column(name = "school_id", nullable = false)
    val schoolId: Long,

    @Column(name = "email_hash", nullable = false, columnDefinition = "BINARY(32)")
    val emailHash: ByteArray,

    @Column(name = "code_hash", nullable = false, columnDefinition = "BINARY(32)")
    val codeHash: ByteArray,

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 30)
    val purpose: SchoolEmailVerificationPurpose = SchoolEmailVerificationPurpose.SCHOOL_SIGNUP,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: SchoolEmailVerificationStatus = SchoolEmailVerificationStatus.PENDING,

    @Column(name = "attempt_count", nullable = false)
    var attemptCount: Int = 0
) {
    @Column(name = "verified_at")
    var verifiedAt: LocalDateTime? = null
        protected set

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null
        protected set

    fun increaseAttempt() {
        this.attemptCount += 1
    }

    fun verify(now: LocalDateTime) {
        this.status = SchoolEmailVerificationStatus.VERIFIED
        this.verifiedAt = now
    }

    fun expire() {
        this.status = SchoolEmailVerificationStatus.EXPIRED
    }

    fun cancel() {
        this.status = SchoolEmailVerificationStatus.CANCELED
    }

    fun isInCooldown(now: LocalDateTime, cooldownSeconds: Long): Boolean {
        val requestedAt = createdAt ?: return false
        return requestedAt.plusSeconds(cooldownSeconds).isAfter(now)
    }
}
