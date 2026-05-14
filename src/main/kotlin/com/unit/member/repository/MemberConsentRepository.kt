package com.unit.member.repository

import com.unit.member.entity.MemberConsent
import org.springframework.data.jpa.repository.JpaRepository

interface MemberConsentRepository : JpaRepository<MemberConsent, Long>