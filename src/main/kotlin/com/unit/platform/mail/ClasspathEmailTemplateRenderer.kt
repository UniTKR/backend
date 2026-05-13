package com.unit.platform.mail

import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Component
import org.springframework.web.util.HtmlUtils

@Component
class ClasspathEmailTemplateRenderer(
    private val resourceLoader: ResourceLoader,
) : EmailTemplateRenderer {

    override fun render(
        templatePath: String,
        variables: Map<String, String>,
    ): String {
        val resource = resourceLoader.getResource("classpath:$templatePath")

        require(resource.exists()) {
            "Email template not found: $templatePath"
        }

        var html = resource.getContentAsString(Charsets.UTF_8)

        variables.forEach { (key, value) ->
            html = html.replace(
                "{{$key}}",
                HtmlUtils.htmlEscape(value),
            )
        }

        return html
    }
}
