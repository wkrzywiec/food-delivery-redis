package io.wkrzywiec.fooddelivery.food;

import com.redislabs.modules.rejson.JReJSON;
import io.wkrzywiec.fooddelivery.food.controller.FoodItemDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Component
@Profile("redis")
public class FoodItemRepository {

    private final JReJSON redisJson;
    public List<FoodItemDTO> saveAll(List<FoodItemDTO> foodItemDTOs) {
        List<FoodItemDTO> result = new ArrayList<>();

        for (FoodItemDTO food: foodItemDTOs) {
            if (food.getId() == null) {
                food.setId(UUID.randomUUID().toString());
            }

            String key = getKey(food);
            redisJson.set(key, food);
            result.add(food);
        }

        return result;
    }

    private String getKey(FoodItemDTO foodItemDTO) {
        String id = foodItemDTO.getId() == null ? UUID.randomUUID().toString() : foodItemDTO.getId();
        return String.format("%s:%s", "food", id);
    }
}
