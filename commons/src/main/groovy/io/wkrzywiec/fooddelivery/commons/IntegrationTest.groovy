package io.wkrzywiec.fooddelivery.commons

import io.restassured.RestAssured
import io.restassured.module.mockmvc.RestAssuredMockMvc
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.context.WebApplicationContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.spock.Testcontainers
import org.testcontainers.utility.DockerImageName
import spock.lang.Specification

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = IntegrationTestContainerInitializer)
abstract class IntegrationTest extends Specification {

    private static final PostgreSQLContainer POSTGRES_DB
    protected static final String POSTGRES_URL
    protected static final String DB_NAME = "food_delivery"
    protected static final String DB_USERNAME = "food_delivery"
    protected static final String DB_PASSWORD = "food_delivery"

    private static final GenericContainer REDIS
    protected static final String REDIS_HOST
    protected static final Integer REDIS_PORT

    static {

        if (useLocalInfrastructure()) {
            POSTGRES_URL = "jdbc:postgresql://localhost:5432/food_delivery?loggerLevel=INFO"
            REDIS_HOST = "localhost"
            REDIS_PORT = 6379
            return
        }
        POSTGRES_DB = new PostgreSQLContainer("postgres:14-alpine")
                .withDatabaseName(DB_NAME)
                .withUsername(DB_USERNAME)
                .withPassword(DB_PASSWORD)
        POSTGRES_DB.start()
        POSTGRES_URL = POSTGRES_DB.getJdbcUrl()

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
    }

    static class IntegrationTestContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues values = TestPropertyValues.of(
                    "spring.datasource.url=" + POSTGRES_URL,
                    "spring.datasource.username=" + DB_USERNAME,
                    "spring.datasource.password=" + DB_PASSWORD,
                    "spring.redis.host=" + REDIS_HOST,
                    "spring.redis.port=" + REDIS_PORT
            )

            values.applyTo(applicationContext)
        }
    }
}
