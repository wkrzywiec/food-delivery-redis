package io.wkrzywiec.fooddelivery.bff.controller

import io.wkrzywiec.fooddelivery.bff.controller.model.ChangeDeliveryManDTO
import io.wkrzywiec.fooddelivery.bff.controller.model.UpdateDeliveryDTO
import io.wkrzywiec.fooddelivery.bff.inbox.InMemoryInboxPublisher
import io.wkrzywiec.fooddelivery.bff.inbox.InboxPublisher
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification
import spock.lang.Subject

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@Subject(DeliveryController)
@SpringBootTest
@AutoConfigureMockMvc
class DeliveryControllerSpec extends Specification {

    @SpringBean
    private InboxPublisher inboxPublisher = new InMemoryInboxPublisher()

    @Autowired
    private MockMvc mockMvc

    def setup() {
        inboxPublisher.inboxes.clear()
    }

    def "Change status of an order"() {
        given:
        def requestBody = """
            {
              "status": "prepareFood"
            }
        """

        when: "Change status of an order"
        def result = mockMvc.perform(
                patch("/deliveries/any-order-id")
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())

        then:
        result.andExpect(status().isAccepted())
                .andExpect(jsonPath("orderId").value("any-order-id"))

        and: "Message was sent to inbox"
        def inbox = inboxPublisher.inboxes.get("delivery-inbox:update")
        with(inbox.peek() as UpdateDeliveryDTO) { it ->
            it.orderId == "any-order-id"
            it.status == "prepareFood"
        }
    }

    def "Assign delivery man"() {
        given:
        def requestBody = """
            {
              "deliveryManId": "any-delivery-man"
            }
        """

        when: "Assign delivery man"
        def result = mockMvc.perform(
                post("/deliveries/any-order-id/delivery-man")
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())

        then:
        result.andExpect(status().isAccepted())
                .andExpect(jsonPath("orderId").value("any-order-id"))

        and: "Message was sent to inbox"
        def inbox = inboxPublisher.inboxes.get("delivery-inbox:delivery-man")
        with(inbox.peek() as ChangeDeliveryManDTO) { it ->
            it.orderId == "any-order-id"
            it.deliveryManId == "any-delivery-man"
        }
    }
}
