package io.wkrzywiec.fooddelivery.commons

import io.restassured.RestAssured
import io.restassured.module.mockmvc.RestAssuredMockMvc
import io.wkrzywiec.fooddelivery.commons.infra.messaging.redis.RedisMessagePublisherConfig
import io.wkrzywiec.fooddelivery.commons.infra.RedisStreamTestClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.context.WebApplicationContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.spock.Testcontainers
import org.testcontainers.utility.DockerImageName
import spock.lang.Specification

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = IntegrationTestContainerInitializer)
abstract class IntegrationTest extends Specification {

    private static final GenericContainer REDIS
    protected static final String REDIS_HOST
    protected static final Integer REDIS_PORT

    protected RedisStreamTestClient redisStreamsClient

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

    @Autowired
    private WebApplicationContext context

    @LocalServerPort
    private Integer port

    def setup() {
        RestAssuredMockMvc.webAppContextSetup(context)
        RestAssured.port = port

        def config = new RedisMessagePublisherConfig()
        def redisStandaloneConfig = new RedisStandaloneConfiguration(REDIS_HOST, REDIS_PORT)
        def connectionFactory = new LettuceConnectionFactory(redisStandaloneConfig)
        connectionFactory.afterPropertiesSet()
        def redisTemplate = config.redisTemplate(connectionFactory)
        redisStreamsClient = new RedisStreamTestClient(redisTemplate)

        System.out.println("Clearing 'orders' stream from old messages")
        redisTemplate.opsForStream().trim("orders", 0)
        redisTemplate.opsForStream().trim("ordering::any-id", 0)
    }

    static class IntegrationTestContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues values = TestPropertyValues.of(
                    "spring.redis.host=" + REDIS_HOST,
                    "spring.redis.port=" + REDIS_PORT
            )

            values.applyTo(applicationContext)
        }
    }
}
