package com.unit.platform.web.response

data class ApiResponse<T>(
    val code: String,
    val data: T? = null
) {
    companion object {
        fun ok(): ApiResponse<Unit> = ApiResponse(code = "OK")
        fun <T> ok(data: T): ApiResponse<T> = ApiResponse(code = "OK", data = data)
        fun <T> created(data: T): ApiResponse<T> = ApiResponse(code = "CREATED", data = data)
    }
}

data class CursorPageResponse(
    val nextCursor: String?,
    val hasNext: Boolean,
    val size: Int
)

data class CursorListResponse<T>(
    val data: List<T>,
    val page: CursorPageResponse,
)
