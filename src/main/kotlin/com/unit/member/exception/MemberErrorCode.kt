package com.unit.member.exception

import com.unit.platform.error.UnitErrorCode
import org.springframework.http.HttpStatus

enum class MemberErrorCode(
    override val code: String,
    override val message: String,
    override val status: HttpStatus
) : UnitErrorCode {

    // users
    EMAIL_ALREADY_EXISTS("MEMBER_EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다.", HttpStatus.CONFLICT),
    NICKNAME_ALREADY_EXISTS("MEMBER_NICKNAME_ALREADY_EXISTS", "이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT),
    INVALID_LOGIN_CREDENTIALS("AUTH_INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    MEMBER_LOGIN_FORBIDDEN("MEMBER_LOGIN_FORBIDDEN", "로그인할 수 없는 회원 상태입니다.", HttpStatus.FORBIDDEN),
    INVALID_REFRESH_TOKEN("AUTH_INVALID_REFRESH_TOKEN", "Refresh Token이 유효하지 않습니다.", HttpStatus.UNAUTHORIZED),
    EXPIRED_REFRESH_TOKEN("AUTH_EXPIRED_REFRESH_TOKEN", "Refresh Token이 만료되었습니다.", HttpStatus.UNAUTHORIZED),

    // schools
    SCHOOL_NOT_FOUND("SCHOOL_NOT_FOUND", "학교를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    SCHOOL_EMAIL_DOMAIN_NOT_ALLOWED("SCHOOL_EMAIL_DOMAIN_NOT_ALLOWED", "해당 학교 이메일 도메인이 아닙니다.", HttpStatus.BAD_REQUEST),
    SCHOOL_EMAIL_VERIFICATION_CODE_NOT_FOUND("SCHOOL_EMAIL_VERIFICATION_CODE_NOT_FOUND", "학교 이메일 인증 요청을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    SCHOOL_EMAIL_VERIFICATION_CODE_EXPIRED("SCHOOL_EMAIL_VERIFICATION_CODE_EXPIRED", "학교 이메일 인증 코드가 만료되었습니다.", HttpStatus.BAD_REQUEST),
    SCHOOL_EMAIL_VERIFICATION_CODE_MISMATCHED("SCHOOL_EMAIL_VERIFICATION_CODE_MISMATCHED", "학교 이메일 인증 코드가 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
    SCHOOL_VERIFICATION_NOT_FOUND("SCHOOL_VERIFICATION_NOT_FOUND", "학교 인증 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    SCHOOL_EMAIL_VERIFICATION_COOLDOWN("SCHOOL_EMAIL_VERIFICATION_COOLDOWN", "학교 이메일 인증 코드를 다시 요청하려면 잠시 후 시도해주세요.", HttpStatus.TOO_MANY_REQUESTS),
    SCHOOL_EMAIL_VERIFICATION_ATTEMPT_LIMIT_EXCEEDED("SCHOOL_EMAIL_VERIFICATION_ATTEMPT_LIMIT_EXCEEDED", "학교 이메일 인증 코드 입력 횟수를 초과했습니다. 인증 코드를 다시 요청해주세요.", HttpStatus.TOO_MANY_REQUESTS),

}
