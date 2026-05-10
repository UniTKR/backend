package com.unit.platform.security

import com.nimbusds.jose.jwk.source.ImmutableSecret
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.nio.charset.StandardCharsets
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableConfigurationProperties(JwtProperties::class)
class JwtConfig(
    private val properties: JwtProperties
) {
    @Bean
    fun jwtEncoder(): JwtEncoder = NimbusJwtEncoder(ImmutableSecret(secretKey()))

    @Bean
    fun jwtDecoder(): JwtDecoder {
        return NimbusJwtDecoder
            .withSecretKey(secretKey())
            .macAlgorithm(MacAlgorithm.HS256)
            .build()
    }

    private fun secretKey(): SecretKey {
        val bytes = properties.secret.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size >= 32) { "JWT secret must be at least 32 bytes." }
        return SecretKeySpec(bytes, "HmacSHA256")
    }


}