package com.daesabu.meongcoach.ai.adapter.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;

/**
 * 클래스패스의 프롬프트 파일을 읽는 유틸. 프롬프트 설정 오류는 호출 시점이 아니라 기동 시점에 드러나야 하므로
 * 파일이 없거나 비어 있으면 예외를 던져 컨텍스트 기동을 실패시킨다.
 */
final class PromptLoader {

	private PromptLoader() {
	}

	static String load(String location) {
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
