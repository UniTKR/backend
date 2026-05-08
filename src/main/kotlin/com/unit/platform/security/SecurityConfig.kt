package com.unit.platform.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {

    private val actuatorEndpoints = arrayOf(
        "/actuator/health",
        "/actuator/health/**",
        "/actuator/metrics",
        "/actuator/metrics/**",
        "/actuator/prometheus"
    )

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(*actuatorEndpoints).permitAll()
                    .anyRequest().permitAll()
            }
        return http.build()
    }
}