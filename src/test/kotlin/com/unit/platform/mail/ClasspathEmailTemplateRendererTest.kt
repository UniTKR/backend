package com.unit.platform.mail

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.core.io.DefaultResourceLoader
import io.mockk.every
import io.mockk.mockk
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import java.io.IOException

@DisplayName("이메일 템플릿 렌더러 테스트")
class ClasspathEmailTemplateRendererTest {

    private val renderer = ClasspathEmailTemplateRenderer(DefaultResourceLoader())

    @Test
    @DisplayName("classpath HTML 템플릿을 UTF-8로 읽고 변수를 치환한다")
    fun render() {
        val html = renderer.render(
            templatePath = "mail/school-email-verification.html",
            variables = mapOf(
                "code" to "123456",
                "expiresInMinutes" to "5",
            ),
        )

        assertThat(html).contains("이메일 인증")
        assertThat(html).contains("인증번호")
        assertThat(html).contains("123456")
        assertThat(html).contains("5")
        assertThat(html).doesNotContain("{{code}}")
        assertThat(html).doesNotContain("{{expiresInMinutes}}")
        assertThat(html).doesNotContain("{{requestedAt}}")
        assertThat(html).doesNotContain("{{supportEmail}}")
    }

    @Test
    @DisplayName("변수 값은 HTML escape 처리한다")
    fun escapeVariables() {
        val html = renderer.render(
            templatePath = "mail/school-email-verification.html",
            variables = mapOf(
                "code" to "<script>",
                "expiresInMinutes" to "5",
            ),
        )

        assertThat(html).contains("&lt;script&gt;")
        assertThat(html).doesNotContain("<script>")
    }

    @Test
    @DisplayName("템플릿 파일이 없으면 예외가 발생한다")
    fun renderWithNotFoundTemplate() {
        assertThatThrownBy {
            renderer.render(
                templatePath = "mail/not-found.html",
                variables = emptyMap(),
            )
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Email template not found")
    }

    @Test
    @DisplayName("템플릿 읽기 중 예외가 발생하면 예외가 전파된다")
    fun renderWithReadFailure() {
        val resourceLoader = mockk<ResourceLoader>()
        val resource = mockk<Resource>()

        every { resourceLoader.getResource("classpath:mail/broken.html") } returns resource
        every { resource.exists() } returns true
        every { resource.getContentAsString(Charsets.UTF_8) } throws IOException("read failed")

        val renderer = ClasspathEmailTemplateRenderer(resourceLoader)

        assertThatThrownBy {
            renderer.render(
                templatePath = "mail/broken.html",
                variables = emptyMap(),
            )
        }
            .isInstanceOf(IOException::class.java)
            .hasMessageContaining("read failed")
    }


}
