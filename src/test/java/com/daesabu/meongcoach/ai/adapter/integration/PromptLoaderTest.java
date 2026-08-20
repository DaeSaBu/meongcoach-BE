package com.daesabu.meongcoach.ai.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("프롬프트 로더")
class PromptLoaderTest {

	@Test
	@DisplayName("클래스패스의 프롬프트 파일 내용을 반환한다")
	void loadReturnsPromptFileContent() {
		String content = PromptLoader.load("prompts/video-analysis/system.md");

		assertThat(content).contains("반려견");
	}

	@Test
	@DisplayName("파일이 없으면 로딩에 실패한다")
	void loadFailsWhenFileDoesNotExist() {
		assertThatThrownBy(() -> PromptLoader.load("prompts/video-analysis/nonexistent.md"))
				.isInstanceOf(IllegalStateException.class);
	}
}
