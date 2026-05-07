package com.unit

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules

@DisplayName("Spring Modulith 모듈 구조")
class ModularityTests {

    @Test
    @DisplayName("애플리케이션 모듈 간 의존성 규칙을 검증한다")
    fun verifiesModularStructure() {
        val modules = ApplicationModules.of(UnitBackendApplication::class.java)

        modules.verify()
    }
}