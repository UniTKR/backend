package com.unit.platform.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler

@Configuration
class SecurityConfig(
    private val authenticationEntryPoint: AuthenticationEntryPoint,
    private val accessDeniedHandler: AccessDeniedHandler
) {

    private val actuatorEndpoints = arrayOf(
        "/actuator/health",
        "/actuator/health/**",
        "/actuator/metrics",
        "/actuator/metrics/**",
        "/actuator/prometheus"
    )

    private val permitAllEndpoints = arrayOf(
        "/api/v1/schools",
        "/api/v1/members/signup",
        "/api/v1/auth/**"
    )

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .logout { it.disable() }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .exceptionHandling {
                it.authenticationEntryPoint(authenticationEntryPoint)
                it.accessDeniedHandler(accessDeniedHandler)
            }
            .oauth2ResourceServer {
                it.jwt {}
            }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(*actuatorEndpoints).permitAll()
                    .requestMatchers(*permitAllEndpoints).permitAll()
                    .anyRequest().permitAll()
            }
        return http.build()
    }
}