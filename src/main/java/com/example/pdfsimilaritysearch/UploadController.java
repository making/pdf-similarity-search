package com.example.pdfsimilaritysearch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UploadController {

	private final VectorStore vectorStore;

	private final Resource uploadDir;

	private final TaskExecutor taskExecutor;

	private final Logger logger = LoggerFactory.getLogger(UploadController.class);

	private final ConcurrentMap<String, UploadTask> uploadTaskMap = new ConcurrentHashMap<>();

	public UploadController(VectorStore vectorStore, @Value("${upload.dir:file:/tmp}") Resource uploadDir,
			TaskExecutor taskExecutor) {
		this.vectorStore = vectorStore;
		this.uploadDir = uploadDir;
		this.taskExecutor = taskExecutor;
	}

	@GetMapping("/upload")
	public String index() {
		return "upload";
	}

	@GetMapping(path = "/upload/{id}/progress")
	public SseEmitter uploadProgress(@PathVariable String id) {
		UploadTask uploadTask = this.uploadTaskMap.get(id);
		if (uploadTask == null) {
			throw new ResponseStatusException(HttpStatus.GONE, "The requested task has gone.");
		}
		if (!uploadTask.isStarted().compareAndSet(false, true)) {
			throw new ResponseStatusException(HttpStatus.GONE, "The requested task has already started.");
		}
		SseEmitter emitter = new SseEmitter(TimeUnit.HOURS.toMillis(1));
		emitter.onTimeout(() -> logger.warn("Timeout! ID: {}", uploadTask.id()));
		emitter.onError(ex -> logger.error("Error! ID: " + uploadTask.id(), ex));
		this.taskExecutor.execute(() -> {
			logger.info("Started the task ID: {}", uploadTask.id());
			try {
				for (Path path : uploadTask.paths()) {
					Resource resource = new FileSystemResource(path);
					DocumentReader documentReader;
					emitter.send(SseEmitter.event()
						.name("message")
						.data("⌛️ Adding %s to the vector store.".formatted(path.getFileName())));
					try {
						documentReader = new ParagraphPdfDocumentReader(resource);
					}
					catch (RuntimeException e) {
						documentReader = new PagePdfDocumentReader(resource, PdfDocumentReaderConfig.builder()
							.withPagesPerDocument(2)
							.withPageExtractedTextFormatter(
									ExtractedTextFormatter.builder().withNumberOfBottomTextLinesToDelete(2).build())
							.build());
					}
					emitter.send(SseEmitter.event()
						.name("message")
						.data("Use %s as the document reader.".formatted(documentReader.getClass())));
					List<Document> documents = documentReader.get();
					for (int i = 0; i < documents.size(); i++) {
						Document document = documents.get(i);
						// TODO Batch processing
						this.vectorStore.add(List.of(document));
						if ((i + 1) % 10 == 0) {
							emitter.send(SseEmitter.event()
								.name("message")
								.data("%d%% done".formatted(100 * (i + 1) / documents.size())));
						}
					}
					emitter.send(SseEmitter.event()
						.name("message")
						.data("✅ Added %s to the vector store.".formatted(path.getFileName())));
				}
				emitter.send("Completed 🎉");
				emitter.complete();
			}
			catch (Exception ex) {
				logger.error("Task failed", ex);
				emitter.completeWithError(ex);
			}
			finally {
				this.uploadTaskMap.remove(id);
			}
			logger.info("Finished the task ID: {}", uploadTask.id());
		});
		return emitter;
	}

	@PostMapping("/upload")
	public String upload(@RequestParam("files") MultipartFile[] files, RedirectAttributes redirectAttributes)
			throws Exception {
		List<Path> paths = new ArrayList<>();
		for (MultipartFile file : files) {
			if (file.isEmpty()) {
				continue;
			}
			byte[] bytes = file.getBytes();
			Path path = this.uploadDir.getFile().toPath().resolve(Objects.requireNonNull(file.getOriginalFilename()));
			Files.write(path, bytes);
			paths.add(path);
		}
		String id = UUID.randomUUID().toString();
		this.uploadTaskMap.computeIfAbsent(id, k -> new UploadTask(id, paths, new AtomicBoolean(false)));
		redirectAttributes
			.addFlashAttribute("message", "Files have been uploaded. Next, add the uploaded files to the vector store.")
			.addFlashAttribute("id", id);
		return "redirect:/upload";
	}

	record UploadTask(String id, List<Path> paths, AtomicBoolean isStarted) {
	}

}
