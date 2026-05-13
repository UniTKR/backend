package com.unit

import com.unit.platform.mail.EmailSender
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean

@ActiveProfiles("test")
@SpringBootTest
class UnitBackendApplicationTests {

    @MockitoBean
    private lateinit var emailSender: EmailSender

    @Test
    fun contextLoads() {
    }

}
