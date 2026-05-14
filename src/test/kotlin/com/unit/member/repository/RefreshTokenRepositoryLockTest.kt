package com.unit.member.repository

import com.unit.member.entity.RefreshToken
import com.unit.member.enums.RefreshTokenStatus
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@DataJpaTest
@TestPropertySource(
    properties = [
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("RefreshToken Repository lock 테스트")
class RefreshTokenRepositoryLockTest @Autowired constructor(
    private val refreshTokenRepository: RefreshTokenRepository,
    transactionManager: PlatformTransactionManager
) {

    private val transactionTemplate = TransactionTemplate(transactionManager)

    @AfterEach
    fun tearDown() {
        refreshTokenRepository.deleteAll()
    }

    @Test
    @DisplayName("ACTIVE Refresh Token 조회는 트랜잭션 종료 전까지 다른 rotation 조회를 대기시킨다")
    fun lockTest() {

        val tokenHash = ByteArray(32) { 1 }
        val savedToken = refreshTokenRepository.saveAndFlush(
            createRefreshToken(tokenHash = tokenHash),
        )

        val firstTransactionLocked = CountDownLatch(1)
        val releaseFirstTransaction = CountDownLatch(1)
        val secondTransactionStarted = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)

        try {
            val first = executor.submit {
                transactionTemplate.executeWithoutResult {
                    val token = requireNotNull(
                        refreshTokenRepository.findByTokenHashAndStatus(tokenHash),
                    )

                    firstTransactionLocked.countDown()
                    assertThat(releaseFirstTransaction.await(3, TimeUnit.SECONDS)).isTrue()

                    token.rotate(LocalDateTime.now())
                }
            }

            assertThat(firstTransactionLocked.await(3, TimeUnit.SECONDS)).isTrue()

            val second = executor.submit<RefreshToken?> {
                secondTransactionStarted.countDown()

                transactionTemplate.execute {
                    refreshTokenRepository.findByTokenHashAndStatus(tokenHash)
                }
            }

            assertThat(secondTransactionStarted.await(3, TimeUnit.SECONDS)).isTrue()

            Thread.sleep(200)
            assertThat(second.isDone).isFalse()

            releaseFirstTransaction.countDown()

            first.get(3, TimeUnit.SECONDS)
            val secondResult = second.get(3, TimeUnit.SECONDS)

            assertThat(secondResult).isNull()

            val foundToken = refreshTokenRepository.findById(requireNotNull(savedToken.id)).orElseThrow()
            assertThat(foundToken.status).isEqualTo(RefreshTokenStatus.ROTATED)
        } finally {
            releaseFirstTransaction.countDown()
            executor.shutdownNow()
        }

    }

    private fun createRefreshToken(
        memberId: Long = 1L,
        tokenHash: ByteArray = ByteArray(32) { 1 },
        status: RefreshTokenStatus = RefreshTokenStatus.ACTIVE,
        expiresAt: LocalDateTime = LocalDateTime.of(2026, 5, 15, 12, 0),
    ): RefreshToken {
        return RefreshToken(
            memberId = memberId,
            tokenHash = tokenHash,
            status = status,
            expiresAt = expiresAt,
        )
    }
}