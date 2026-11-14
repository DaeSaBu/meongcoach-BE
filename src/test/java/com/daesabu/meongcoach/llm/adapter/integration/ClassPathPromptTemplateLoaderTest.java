package com.daesabu.meongcoach.llm.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.llm.application.provided.PromptTemplateLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ClassPathPromptTemplateLoaderTest {

	private final PromptTemplateLoader loader = new ClassPathPromptTemplateLoader();

	@Test
	void include_지시문_자리에_조각의_앞뒤_공백을_뺀_내용을_넣는다() {
		String content = loader.load("prompts/template-loader-test/with-include.md");

		assertThat(content).isEqualTo("머리말\n   조각 첫 줄\n조각 둘째 줄\n맺음말");
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"prompts/template-loader-test/nonexistent.md",
			"prompts/template-loader-test/blank.md",
			"prompts/template-loader-test/with-missing-include.md",
			"prompts/template-loader-test/with-nested-include.md"
	})
	void 읽을_수_없거나_비었거나_조각을_풀_수_없으면_로딩에_실패한다(String location) {
		assertThatThrownBy(() -> loader.load(location))
				.isInstanceOf(IllegalStateException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"prompts/video-analysis/system.md",
			"prompts/video-analysis/user.md"
	})
	void 실제_프롬프트는_include_지시문을_남기지_않는다(String location) {
		String content = loader.load(location);

		assertThat(content).doesNotContain("{{include:");
	}

	@Test
	void 영상_분석_프롬프트가_공용_조각의_안전_원칙과_교육_매핑을_담는다() {
		String safetyPrinciples = loader.load("prompts/behavior-analysis/safety-principles.md");
		String topicMatching = loader.load("prompts/behavior-analysis/topic-matching-core.md");

		assertThat(loader.load("prompts/video-analysis/system.md")).contains(safetyPrinciples);
		assertThat(loader.load("prompts/video-analysis/user.md")).contains(topicMatching);
	}
}
