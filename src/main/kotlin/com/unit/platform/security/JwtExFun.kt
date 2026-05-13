package com.unit.platform.security

import com.unit.platform.error.BusinessException
import com.unit.platform.error.CommonErrorCode
import org.springframework.security.oauth2.jwt.Jwt

fun Jwt.memberId(): Long {
    return subject.toLongOrNull()
        ?: throw BusinessException(CommonErrorCode.INVALID_TOKEN)
}