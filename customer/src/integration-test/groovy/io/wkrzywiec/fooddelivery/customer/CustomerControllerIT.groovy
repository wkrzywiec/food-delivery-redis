package io.wkrzywiec.fooddelivery.customer

import io.wkrzywiec.fooddelivery.customer.IntegrationTest
import io.wkrzywiec.fooddelivery.customer.Customer
import io.wkrzywiec.fooddelivery.customer.CustomerRepository
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Narrative
import spock.lang.Subject

import static io.restassured.RestAssured.given
import static io.restassured.RestAssured.when
import static org.hamcrest.Matchers.hasItems
import static org.hamcrest.Matchers.notNullValue
import static org.hamcrest.Matchers.is

@Subject(CustomerRepository)
@Narrative("""
    This integration test is only to show that Spring Data REST repository is working,
    and it generated the most important CRUD endpoints.
""")
class CustomerControllerIT extends IntegrationTest {

    @Autowired
    private CustomerRepository repository

    def "GET /customers - Fetch list of customers"() {
        given:
        var customer = new Customer(firstName: "Luke", lastName: "Skywalker", email: "luke.skywalker@jedi.com", phone: "11233435")
        repository.save(customer)

        when:
        def response = when().get("/customers")

        then:
        response.then()
                .log().all()
                .statusCode(200)
                .body("_embedded.customers[0].id", notNullValue())
                .body("_embedded.customers.firstName", hasItems("Luke"))
                .body("_embedded.customers.lastName", hasItems("Skywalker"))
                .body("_embedded.customers.email", hasItems("luke.skywalker@jedi.com"))
                .body("_embedded.customers.phone", hasItems("11233435"))
    }

    def "GET /customers/{id} - Fetch single customer"() {
        given:
        var customer = repository.save(testCustomer())

        when:
        def response = when().get("/customers/{id}", customer.getId())

        then:
        response.then()
                .log().all()
                .statusCode(200)
                .body("id", is(customer.id))
                .body("firstName", is("Luke"))
                .body("lastName", is("Skywalker"))
                .body("email", is("luke.skywalker@jedi.com"))
                .body("phone", is("11233435"))
    }

    def "POST /customers - Create a customer"() {
        given:
        var requestBody = '''
            {
                "firstName": "Darth",
                "lastName": "Vader",
                "email": "darth.vader@sith.com",
                "phone": "1233"
            }
        '''

        when:
        def response = given()
                .header("Content-type", "application/json")
                .and()
                .body(requestBody)
                .when()
                .log().all()
                .post("/customers")

        then:
        response.then()
                .log().all()
                .statusCode(201)
    }

    def "DELETE /customers/{id} - Delete a customer"() {
        given:
        var customer = repository.save(testCustomer())

        when:
        def response = when().delete("/customers/{id}", customer.getId())

        then:
        response.then()
                .log().all()
                .statusCode(204)
    }

    private Customer testCustomer() {
        return new Customer(firstName: "Luke", lastName: "Skywalker", email: "luke.skywalker@jedi.com", phone: "11233435")
    }
}
