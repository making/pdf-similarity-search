package com.example.pdfsimilaritysearch;

import am.ik.accesslogger.AccessLogger;

import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.embedding.PostgresMlEmbeddingClient;
import org.springframework.ai.embedding.PostgresMlEmbeddingClient.VectorType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class AppConfig {

	@Bean
	public EmbeddingClient embeddingClient(JdbcTemplate jdbcTemplate) {
		return new PostgresMlEmbeddingClient(jdbcTemplate, "intfloat/multilingual-e5-large", VectorType.PG_VECTOR);
	}

	@Bean
	public AccessLogger accessLogger() {
		return new AccessLogger(httpExchange -> {
			final String uri = httpExchange.getRequest().getUri().getPath();
			return uri != null && !(uri.equals("/readyz") || uri.equals("/livez") || uri.startsWith("/actuator"));
		});
	}

}
