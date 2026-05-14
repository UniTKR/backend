package com.unit.member.entity

import com.unit.member.enums.*;
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class Member(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "email_hash", columnDefinition = "BINARY(32)")
    var emailHash: ByteArray? = null,

    @Column(name = "email_encrypted", columnDefinition = "VARBINARY(512)")
    var emailEncrypted: ByteArray? = null,

    @Column(name = "phone_hash", columnDefinition = "BINARY(32)")
    var phoneHash: ByteArray? = null,

    @Column(name = "password_hash", length = 100)
    var passwordHash: String? = null,

    @Column(name = "nickname", nullable = false, length = 40)
    var nickname: String,

    @Column(name = "profile_image_url", length = 500)
    var profileImageUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: MemberStatus = MemberStatus.ACTIVE,

    @Column(name = "trust_score", nullable = false)
    var trustScore: Int = 500,

    @Column(name = "trade_completed_count", nullable = false)
    var tradeCompletedCount: Int = 0,

    @Column(name = "no_show_count", nullable = false)
    var noShowCount: Int = 0,

    @Column(name = "report_received_count", nullable = false)
    var reportReceivedCount: Int = 0
) {

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null
        protected set

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
        protected set

    fun changeNickname(nickname: String) {
        this.nickname = nickname
    }

    fun suspend() {
        this.status = MemberStatus.SUSPENDED
    }

    fun delete(now: LocalDateTime) {
        this.status = MemberStatus.DELETED
        this.deletedAt = now
    }

    fun activate() {
        this.status = MemberStatus.ACTIVE
    }

    fun withdraw(now: LocalDateTime) {
        this.status = MemberStatus.DELETED
        this.deletedAt = now
        this.nickname = "탈퇴한 사용자"
        this.profileImageUrl = null
        this.passwordHash = null
        this.emailEncrypted = null
        this.phoneHash = null
    }
}
