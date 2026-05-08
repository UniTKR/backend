package com.unit.member.entity

import com.unit.member.enums.SchoolStatus
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "school_email_domains")
class SchoolEmailDomain(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "school_id", nullable = false)
    val schoolId: Long,

    @Column(name = "domain", nullable = false, unique = true, length = 120)
    val domain: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: SchoolStatus = SchoolStatus.ACTIVE
) {
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null
        protected set
}