package com.daesabu.meongcoach.shared.prompt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;

public final class PromptLoader {

	private PromptLoader() {
	}

	public static String load(String location) {
		try {
			String content = new ClassPathResource(location).getContentAsString(StandardCharsets.UTF_8);
			if (content.isBlank()) {
				throw new IllegalStateException("프롬프트 파일이 비어 있습니다: " + location);
			}
			return content.strip();
		} catch (IOException e) {
			throw new IllegalStateException("프롬프트 파일을 읽을 수 없습니다: " + location, e);
		}
	}
}
