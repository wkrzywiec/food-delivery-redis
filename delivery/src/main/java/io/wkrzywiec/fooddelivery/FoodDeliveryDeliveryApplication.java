package io.wkrzywiec.fooddelivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication
public class FoodDeliveryDeliveryApplication {

	public static void main(String[] args) {
		SpringApplication.run(FoodDeliveryDeliveryApplication.class, args);
	}

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}
}
