package com.unit.platform.error

class BusinessException(
    val errorCode: UnitErrorCode,
    message: String = errorCode.message,
    cause: Throwable? = null,
) : RuntimeException(message, cause){
}