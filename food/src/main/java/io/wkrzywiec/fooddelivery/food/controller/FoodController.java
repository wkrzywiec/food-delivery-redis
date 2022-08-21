package io.wkrzywiec.fooddelivery.food.controller;

import io.wkrzywiec.fooddelivery.food.FoodItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class FoodController {

    private final FoodItemRepository repository;

    @PostMapping("/foods")
    ResponseEntity<List<FoodItemDTO>> addFoodItems(@RequestBody List<FoodItemDTO> foodItemDTOs) {
        log.info("Received request to create food items: {}", foodItemDTOs);
        var savedFoods = repository.saveAll(foodItemDTOs);
        return ResponseEntity.ok().body(savedFoods);
    }
}
