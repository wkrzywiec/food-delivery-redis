package io.wkrzywiec.fooddelivery.domain.customer

import io.restassured.RestAssured
import io.wkrzywiec.fooddelivery.IntegrationTest

class CustomerControllerIT extends IntegrationTest {

    def "Get customer"() {
        when:
        def response = RestAssured.when().get("/customers")
        then:
        response.then().log().all()
        .statusCode(200)
    }



}
