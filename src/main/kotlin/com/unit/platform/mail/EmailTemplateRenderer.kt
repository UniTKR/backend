package com.unit.platform.mail

interface EmailTemplateRenderer {

    fun render(
        templatePath: String,
        variables: Map<String, String>,
    ): String
}