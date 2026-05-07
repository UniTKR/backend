package com.unit.platform.error

import org.springframework.http.HttpStatus

interface UnitErrorCode {
    val code: String
    val message: String
    val status: HttpStatus
}