package com.unit.platform.security

import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class JwtTokenProvider(
    private val jwtEncoder: JwtEncoder,
    private val properties: JwtProperties
) {

    fun createAccessToken(memberId: Long): String {
        val now = Instant.now()

        val claims = JwtClaimsSet.builder()
            .issuer(properties.issuer)
            .subject(memberId.toString())
            .issuedAt(now)
            .expiresAt(now.plusSeconds(properties.accessTokenExpirationSeconds))
            .claim("memberId", memberId)
            .build()

        val headers = JwsHeader.with(MacAlgorithm.HS256).build()

        return jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).tokenValue
    }

    fun accessTokenExpiresIn(): Long {
        return properties.accessTokenExpirationSeconds
    }
}