package io.wkrzywiec.fooddelivery.commons.messaging


import org.testcontainers.containers.GenericContainer
import org.testcontainers.spock.Testcontainers
import org.testcontainers.utility.DockerImageName
import spock.lang.Specification

@Testcontainers
abstract class CommonsIntegrationTest extends Specification {

    private static final GenericContainer REDIS
    protected static final String REDIS_HOST
    protected static final Integer REDIS_PORT

    static {

        if (useLocalInfrastructure()) {
            REDIS_HOST = "localhost"
            REDIS_PORT = 6379
            return
        }

        REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
        REDIS.start()
        REDIS_HOST = REDIS.getHost()
        REDIS_PORT = REDIS.getMappedPort(6379)
    }

    static boolean useLocalInfrastructure() {
    // change it to `true` in order to use it with infra from docker-compose.yaml
        false
    }
}
