package io.wkrzywiec.fooddelivery.ordering

import io.wkrzywiec.fooddelivery.commons.IntegrationTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("redis")
class OrderingApplicationSpec extends IntegrationTest {

    @Autowired
    ApplicationContext context

    def "should load full Spring context"() {
        expect: "Spring context is loaded correctly"
        context
    }
}
