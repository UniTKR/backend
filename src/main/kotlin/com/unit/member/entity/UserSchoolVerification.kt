package com.unit.member.entity

import com.unit.member.enums.UserSchoolVerificationMethod
import com.unit.member.enums.UserSchoolVerificationStatus
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_school_verifications",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_user_school_verifications_user",
            columnNames = ["user_id"],
        ),
    ],
)
class UserSchoolVerification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val memberId: Long,

    @Column(name = "school_id", nullable = false)
    val schoolId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    val method: UserSchoolVerificationMethod,

    @Column(name = "verified_email_hash", columnDefinition = "BINARY(32)")
    var verifiedEmailHash: ByteArray? = null,

    @Column(name = "verified_email_encrypted", columnDefinition = "VARBINARY(512)")
    var verifiedEmailEncrypted: ByteArray? = null,

    @Column(name = "student_number_hash", columnDefinition = "BINARY(32)")
    val studentNumberHash: ByteArray? = null,

    @Column(name = "admission_year")
    val admissionYear: Short? = null,

    @Column(name = "department_name", length = 100)
    val departmentName: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: UserSchoolVerificationStatus = UserSchoolVerificationStatus.VERIFIED,

    @Column(name = "verified_at")
    var verifiedAt: LocalDateTime? = null,

    @Column(name = "expires_at")
    var expiresAt: LocalDateTime? = null
) {
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null
        protected set

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null
        protected set

    fun revoke() {
        this.status = UserSchoolVerificationStatus.REVOKED
    }

    fun expire() {
        this.status = UserSchoolVerificationStatus.EXPIRED
    }

    fun verifyByEmail(
        now: LocalDateTime,
        emailHash: ByteArray,
        emailEncrypted: ByteArray? = null,
    ) {
        this.status = UserSchoolVerificationStatus.VERIFIED
        this.verifiedAt = now
        this.verifiedEmailHash = emailHash
        this.verifiedEmailEncrypted = emailEncrypted
    }
}
