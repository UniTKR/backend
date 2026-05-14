package com.unit.member.util

import com.unit.member.config.EmailEncryptionProperties
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test

@DisplayName("EmailEncryptor 테스트")
class EmailEncryptorTest {

    private val emailEncryptor = EmailEncryptor(
        EmailEncryptionProperties(
            keyBase64 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        ),
    )

    @Test
    @DisplayName("암호화한 이메일은 복호화하면 원문으로 돌아온다")
    fun encryptAndDecrypt() {
        val encrypted = emailEncryptor.encrypt("test@unit.com")

        val decrypted = emailEncryptor.decrypt(encrypted)

        assertThat(decrypted).isEqualTo("test@unit.com")
    }

    @Test
    @DisplayName("같은 이메일을 암호화해도 매번 다른 암호문이 생성된다")
    fun encryptUsesRandomIv() {
        val first = emailEncryptor.encrypt("test@unit.com")
        val second = emailEncryptor.encrypt("test@unit.com")

        assertThat(first).isNotEqualTo(second)
        assertThat(emailEncryptor.decrypt(first)).isEqualTo("test@unit.com")
        assertThat(emailEncryptor.decrypt(second)).isEqualTo("test@unit.com")
    }

    @Test
    @DisplayName("32바이트가 아닌 키는 생성에 실패한다")
    fun invalidKey() {
        assertThatThrownBy {
            EmailEncryptor(EmailEncryptionProperties(keyBase64 = "c2hvcnQ="))
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("IV보다 짧은 암호문은 복호화에 실패한다")
    fun decryptWithTooShortPayload() {
        assertThatThrownBy {
            emailEncryptor.decrypt(ByteArray(12))
        }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
