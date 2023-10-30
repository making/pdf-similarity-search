package com.example.pdfsimilaritysearch;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DocumentController {

	private final VectorStore vectorStore;

	public DocumentController(VectorStore vectorStore) {
		this.vectorStore = vectorStore;
	}

	@GetMapping(path = "/documents")
	public List<Map<String, Object>> search(@RequestParam String query, @RequestParam(defaultValue = "4") int k,
			Model model) {
		List<Map<String, Object>> documents = this.vectorStore.similaritySearch(query, k).stream().map(document -> {
			Map<String, Object> metadata = document.getMetadata();
			return Map.of( //
					"content", document.getContent(), //
					"file_name", metadata.get("file_name"), //
					"page_number", metadata.get("page_number"), //
					"end_page_number", Objects.requireNonNullElse(metadata.get("end_page_number"), ""), //
					"title", Objects.requireNonNullElse(metadata.get("title"), "-"), //
					"similarity", 1 - (float) metadata.get("distance") //
			);
		}).toList();
		return documents;
	}

}
