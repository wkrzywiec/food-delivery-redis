package io.wkrzywiec.fooddelivery.bff.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.redislabs.lettusearch.Document;
import com.redislabs.lettusearch.SearchResults;
import io.wkrzywiec.fooddelivery.bff.controller.model.ItemDTO;
import io.wkrzywiec.fooddelivery.bff.repository.FoodItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class FoodController {

    private final FoodItemRepository repository;

    @GetMapping("/foods")
    List<JsonNode> findFoodItems(@RequestParam(name="q") String query) {
        log.info("Received request to find food items by query: {}", query);
        return repository.findByQuery(query);
    }
}
