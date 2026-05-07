package com.unit.platform.web.response

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("공통 성공 응답")
class ApiResponseTest {

    @Test
    @DisplayName("데이터가 있는 OK 응답을 생성한다.")
    fun okWithData() {
        val response = ApiResponse.ok(mapOf("id" to 1L))

        assertThat(response.code).isEqualTo("OK")
        assertThat(response.data).isEqualTo(mapOf("id" to 1L))
    }

    @Test
    @DisplayName("데이터가 없는 OK 응답을 생성한다")
    fun okWithoutData() {
        val response = ApiResponse.ok()

        assertThat(response.code).isEqualTo("OK")
        assertThat(response.data).isNull()
    }

    @Test
    @DisplayName("CREATED 응답을 생성한다")
    fun created() {
        val response = ApiResponse.created(mapOf("id" to 1L))

        assertThat(response.code).isEqualTo("CREATED")
        assertThat(response.data).isEqualTo(mapOf("id" to 1L))
    }


}