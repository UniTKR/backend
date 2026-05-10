package com.unit.member.exception

import com.unit.platform.error.UnitErrorCode
import org.springframework.http.HttpStatus

enum class MemberErrorCode(
    override val code: String,
    override val message: String,
    override val status: HttpStatus
) : UnitErrorCode {

    EMAIL_ALREADY_EXISTS("MEMBER_EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다.", HttpStatus.CONFLICT),
    NICKNAME_ALREADY_EXISTS("MEMBER_NICKNAME_ALREADY_EXISTS", "이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT),
    SCHOOL_NOT_FOUND("SCHOOL_NOT_FOUND", "학교를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
}