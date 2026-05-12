package com.unit.member.util

import com.unit.member.repository.SchoolEmailVerificationCodeRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class SchoolEmailVerificationFailureRecorder(
    private val repository: SchoolEmailVerificationCodeRepository
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun expire(id: Long) {
        val code = repository.findById(id).orElseThrow()
        code.expire()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun increaseAttempt(id: Long) {
        val code = repository.findById(id).orElseThrow()
        code.increaseAttempt()
    }
}