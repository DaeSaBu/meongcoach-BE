package com.daesabu.meongcoach.ai.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PromptLoaderTest {

	@Test
	void 클래스패스의_프롬프트_파일_내용을_반환한다() {
		String content = PromptLoader.load("prompts/video-analysis/system.md");

		assertThat(content).contains("반려견");
	}

	@Test
	void 파일이_없으면_로딩에_실패한다() {
		assertThatThrownBy(() -> PromptLoader.load("prompts/video-analysis/nonexistent.md"))
				.isInstanceOf(IllegalStateException.class);
	}
}
