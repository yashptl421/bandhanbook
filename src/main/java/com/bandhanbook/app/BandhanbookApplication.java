package com.bandhanbook.app;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@OpenAPIDefinition(
		info = @Info(
				title = "Bandhan Book API",
				version = "1.0",
				description = "API documentation for Bandhan Book application"
		)
)
@SpringBootApplication
public class BandhanbookApplication {

	public static void main(String[] args) {
		SpringApplication.run(BandhanbookApplication.class, args);
	}

}
