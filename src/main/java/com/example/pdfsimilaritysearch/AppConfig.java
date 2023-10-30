package com.example.pdfsimilaritysearch;

import am.ik.accesslogger.AccessLogger;

import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.embedding.PostgresMlEmbeddingClient;
import org.springframework.ai.embedding.PostgresMlEmbeddingClient.VectorType;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.PgVectorStore.PgDistanceType;
import org.springframework.ai.vectorstore.PgVectorStore.PgIndexType;
import org.springframework.ai.vectorstore.VectorStore;
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
	public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingClient embeddingClient) {
		return new PgVectorStore(jdbcTemplate, embeddingClient, 1024, PgDistanceType.CosineDistance, false,
				PgIndexType.HNSW);
	}

	@Bean
	public AccessLogger accessLogger() {
		return new AccessLogger(httpExchange -> {
			final String uri = httpExchange.getRequest().getUri().getPath();
			return uri != null && !(uri.equals("/readyz") || uri.equals("/livez") || uri.startsWith("/actuator"));
		});
	}

}
