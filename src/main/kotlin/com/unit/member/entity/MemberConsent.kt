package com.unit.member.entity

import com.unit.member.enums.MemberConsentType
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "member_consents",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_member_consents_user_type_version",
            columnNames = ["user_id", "consent_type", "policy_version"],
        ),
    ],
)
class MemberConsent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val memberId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, length = 40)
    val consentType: MemberConsentType,

    @Column(name = "policy_version", nullable = false, length = 40)
    val policyVersion: String,

    @Column(name = "agreed", nullable = false)
    var agreed: Boolean = true,

    @Column(name = "agreed_at")
    var agreedAt: LocalDateTime? = null,

    @Column(name = "withdrawn_at")
    var withdrawnAt: LocalDateTime? = null,
) {

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null
        protected set

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null
        protected set
}