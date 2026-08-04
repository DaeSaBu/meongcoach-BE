package com.daesabu.meongcoach.shared.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@DisplayName("로컬 OpenAPI 스펙 서빙")
class ApiDocsConfigTest {

	@TempDir
	private Path tempDir;

	@Test
	@DisplayName("빌드 산출물 스펙이 있으면 JSON으로 응답한다")
	void servesSpecWhenBuildArtifactExists() throws Exception {
		Path spec = tempDir.resolve("openapi3.json");
		Files.writeString(spec, "{\"openapi\":\"3.0.1\"}");

		mockMvcFor(spec).perform(get("/openapi3.json"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().json("{\"openapi\":\"3.0.1\"}"));
	}

	@Test
	@DisplayName("스펙 파일이 없으면 404로 응답한다")
	void respondsNotFoundWhenSpecIsMissing() throws Exception {
		mockMvcFor(tempDir.resolve("missing.json")).perform(get("/openapi3.json"))
				.andExpect(status().isNotFound());
	}

	private MockMvc mockMvcFor(Path specFile) {
		return MockMvcBuilders.routerFunctions(new ApiDocsConfig().openApiSpecRoute(specFile.toString())).build();
	}
}
