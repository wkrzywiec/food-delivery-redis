package io.wkrzywiec.fooddelivery.bff.controller

import io.wkrzywiec.fooddelivery.bff.controller.model.AddTipDTO
import io.wkrzywiec.fooddelivery.bff.controller.model.CancelOrderDTO
import io.wkrzywiec.fooddelivery.bff.controller.model.CreateOrderDTO
import io.wkrzywiec.fooddelivery.bff.controller.model.ItemDTO
import io.wkrzywiec.fooddelivery.bff.inbox.InMemoryInboxPublisher
import io.wkrzywiec.fooddelivery.bff.inbox.InboxPublisher
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification
import spock.lang.Subject

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print

@Subject(OrdersController)
@WebMvcTest(OrdersController)
@AutoConfigureMockMvc
class OrdersControllerSpec extends Specification {

    @SpringBean
    private InboxPublisher inboxPublisher = new InMemoryInboxPublisher()

    @Autowired
    private MockMvc mockMvc

    def setup() {
        inboxPublisher.inboxes.clear()
    }

    def "Create an order"() {
        given:
        def requestBody = """
            {
              "customerId": "any-customer",
              "restaurantId": "good-restaurant",
              "items": [
                {
                  "name": "pizza",
                  "amount": 2,
                  "pricePerItem": 7.99
                }
              ],
              "address": "main road",
              "deliveryCharge": 5.25
            }
        """

        when: "Create an order"
        def result = mockMvc.perform(
                post("/orders")
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                        .andDo(print())

        then: "OrderId is generated"
        result.andExpect(status().isAccepted())
                .andExpect(jsonPath("orderId").isNotEmpty())

        and: "Message was sent to inbox"
        def inbox = inboxPublisher.inboxes.get("ordering-inbox:create")
        with(inbox.peek() as CreateOrderDTO) { it ->
            it.customerId == "any-customer"
            it.restaurantId == "good-restaurant"
            it.address == "main road"
            it.deliveryCharge == 5.25
            it.items == [ new ItemDTO("pizza", 2, 7.99)]
        }
    }

    def "Create an order and use provided id"() {
        given:
        def id = "this-id"
        def requestBody = """
            {
              "id": "$id",
              "customerId": "any-customer",
              "restaurantId": "good-restaurant",
              "items": [
                {
                  "name": "pizza",
                  "amount": 2,
                  "pricePerItem": 7.99
                }
              ],
              "address": "main road",
              "deliveryCharge": 5.25
            }
        """

        when: "Create an order"
        def result = mockMvc.perform(
                post("/orders")
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())

        then: "OrderId is generated"
        result.andExpect(status().isAccepted())
                .andExpect(jsonPath("orderId").value(id))

        and: "Message was sent to inbox"
        def inbox = inboxPublisher.inboxes.get("ordering-inbox:create")
        with(inbox.peek() as CreateOrderDTO) { it ->
            it.id == id
        }
    }

    def "Cancel an order"() {
        given:
        def requestBody = """
            {
              "reason": "not hungry"
            }
        """

        when: "Cancel an order"
        def result = mockMvc.perform(
                patch("/orders/any-order-id/status/cancel")
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())

        then:
        result.andExpect(status().isAccepted())
                .andExpect(jsonPath("orderId").value("any-order-id"))

        and: "Message was sent to inbox"
        def inbox = inboxPublisher.inboxes.get("ordering-inbox:cancel")
        with(inbox.peek() as CancelOrderDTO) { it ->
            it.orderId == "any-order-id"
            it.reason == "not hungry"
        }
    }

    def "Add tip to an order"() {
        given:
        def requestBody = """
            {
              "tip": 10
            }
        """

        when: "Add tip"
        def result = mockMvc.perform(
                post("/orders/any-order-id/tip")
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())

        then:
        result.andExpect(status().isAccepted())
                .andExpect(jsonPath("orderId").value("any-order-id"))

        and: "Message was sent to inbox"
        def inbox = inboxPublisher.inboxes.get("ordering-inbox:tip")
        with(inbox.peek() as AddTipDTO) { it ->
            it.orderId == "any-order-id"
            it.tip == 10
        }
    }
}
