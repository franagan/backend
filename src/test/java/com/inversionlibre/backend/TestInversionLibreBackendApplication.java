package com.inversionlibre.backend;

import org.springframework.boot.SpringApplication;

public class TestInversionLibreBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(InversionLibreBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
