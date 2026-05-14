package com.unit.member.withdrawal

interface MemberWithdrawalPolicy {

    // 탈퇴 가능 여부 검사
    fun validate(context: MemberWithdrawalContext) {
    }

    // 탈퇴 확정 후 각 도메인 후처리
    fun apply(context: MemberWithdrawalContext) {
    }
}