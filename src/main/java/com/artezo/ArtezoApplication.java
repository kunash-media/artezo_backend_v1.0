package com.artezo;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import java.util.TimeZone;

@EnableCaching
@SpringBootApplication
public class ArtezoApplication {

	public static void main(String[] args) {

		// Load .env file (if present)
		Dotenv dotenv = Dotenv.configure()
				.directory("./") // Look in root folder
				.ignoreIfMissing() // Avoid errors if .env is missing (e.g., in production)
				.load();

		// Set system properties from .env
		dotenv.entries().forEach(entry ->
				System.setProperty(entry.getKey(), entry.getValue())
		);

		// Set the default JVM timezone to India Standard Time (IST)
		// This ensures LocalDateTime.now(), logging timestamps, etc., use IST
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));

		// Optional: Log to confirm
		System.out.println("Default TimeZone set to: " + TimeZone.getDefault().getID());

		SpringApplication.run(ArtezoApplication.class, args);


	}

}
