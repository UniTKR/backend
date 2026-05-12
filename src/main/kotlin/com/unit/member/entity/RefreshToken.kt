package com.unit.member.entity

import com.unit.member.enums.RefreshTokenStatus
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "refresh_tokens")
class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val memberId: Long,

    @Column(name = "token_hash", nullable = false, columnDefinition = "BINARY(32)", unique = true)
    val tokenHash: ByteArray,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: RefreshTokenStatus = RefreshTokenStatus.ACTIVE,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime
) {

    @Column(name = "last_used_at")
    var lastUsedAt: LocalDateTime? = null
        protected set

    @Column(name = "revoked_at")
    var revokedAt: LocalDateTime? = null
        protected set

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null
        protected set

    fun revoke(now: LocalDateTime) {
        this.status = RefreshTokenStatus.REVOKED
        this.revokedAt = now
    }

    fun rotate(now: LocalDateTime) {
        this.status = RefreshTokenStatus.ROTATED
        this.revokedAt = now
    }

    fun markUsed(now: LocalDateTime) {
        this.lastUsedAt = now
    }

    fun expire(now: LocalDateTime) {
        this.status = RefreshTokenStatus.EXPIRED
        this.revokedAt = now
    }

}