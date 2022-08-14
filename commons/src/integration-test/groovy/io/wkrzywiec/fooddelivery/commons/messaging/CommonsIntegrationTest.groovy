package io.wkrzywiec.fooddelivery.commons.messaging


import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.spock.Testcontainers
import org.testcontainers.utility.DockerImageName
import spock.lang.Specification

@Testcontainers
abstract class CommonsIntegrationTest extends Specification {

    private static final GenericContainer REDIS
    protected static final String REDIS_HOST = "localhost"
    protected static final Integer REDIS_PORT = 6379

    static {

        REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
        REDIS.start()
//        REDIS_HOST = REDIS.getHost()
//        REDIS_PORT = REDIS.getMappedPort(6379)
    }
}
