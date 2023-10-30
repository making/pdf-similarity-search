package com.example.pdfsimilaritysearch;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class TestPdfSimilaritySearchApplication {

	@Bean
	@ServiceConnection
	PostgreSQLContainer<?> postgresContainer() {
		return new PostgreSQLContainer<>(
				DockerImageName.parse("ghcr.io/postgresml/postgresml:2.7.3").asCompatibleSubstituteFor("postgres"))
			.withCommand("sleep", "infinity")
			.withLabel("org.springframework.boot.service-connection", "postgres")
			.withUsername("postgresml")
			.withPassword("postgresml")
			.withDatabaseName("postgresml")
			.waitingFor(new LogMessageWaitStrategy().withRegEx(".*Starting dashboard.*\\s")
				.withStartupTimeout(Duration.of(60, ChronoUnit.SECONDS)));
	}

	public static void main(String[] args) {
		SpringApplication.from(PdfSimilaritySearchApplication::main)
			.with(TestPdfSimilaritySearchApplication.class)
			.run(args);
	}

}
