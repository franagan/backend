package com.inversionlibre.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableAsync
@org.springframework.cache.annotation.EnableCaching
public class InversionLibreBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(InversionLibreBackendApplication.class, args);
	}

}
