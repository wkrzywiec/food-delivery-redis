package io.wkrzywiec.fooddelivery

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import spock.lang.Specification

@SpringBootTest
class FoodDeliveryApplicationSpec extends Specification {

    @Autowired
    ApplicationContext context

    def "should load full Spring context"() {
        expect: "Spring context is loaded correctly"
        context
    }
}
