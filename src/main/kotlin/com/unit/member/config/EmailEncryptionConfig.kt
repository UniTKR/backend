package com.unit.member.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(EmailEncryptionProperties::class)
class EmailEncryptionConfig