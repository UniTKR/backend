package com.unit.platform.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.access.intercept.AuthorizationFilter
import tools.jackson.databind.ObjectMapper

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
        "/docs/**",
        "/api/v1/schools",
        "/api/v1/members/signup",
        "/api/v1/auth/**"
    )

    @Bean
    fun securityFilterChain(http: HttpSecurity, jwtAuthenticationValidationFilter: JwtAuthenticationValidationFilter,): SecurityFilterChain {
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
            .addFilterBefore(jwtAuthenticationValidationFilter, AuthorizationFilter::class.java)
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(*actuatorEndpoints).permitAll()
                    .requestMatchers(*permitAllEndpoints).permitAll()
                    .anyRequest().authenticated()
            }

        return http.build()
    }

    @Bean
    fun jwtAuthenticationValidationFilter(
        validators: List<JwtAuthenticationValidator>,
        objectMapper: ObjectMapper,
    ): JwtAuthenticationValidationFilter {
        return JwtAuthenticationValidationFilter(
            validators = validators,
            objectMapper = objectMapper
        )
    }
}