package com.daesabu.meongcoach.llm.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.genai.Client;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class GeminiConfigurationTest {

	@Test
	void 설정_키로_연결과_모델_설정을_함께_바인딩한다() {
		new ApplicationContextRunner()
				.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues(
						"meongcoach.llm.gemini.api-key=test-key",
						"meongcoach.llm.gemini.model=gemini-test",
						"meongcoach.llm.gemini.response-timeout=5m",
						"meongcoach.llm.gemini.max-output-tokens=4096",
						"meongcoach.llm.gemini.temperature=0",
						"meongcoach.llm.gemini.thinking-level=low",
						"meongcoach.llm.gemini.video-fps=1")
				.run(context -> {
					assertThat(context).hasNotFailed();
					GeminiProperties properties = context.getBean(GeminiProperties.class);
					assertThat(properties).isEqualTo(new GeminiProperties(
							"test-key", "gemini-test", Duration.ofMinutes(5), 4096, 0.0f, "low", 1.0));
				});
	}

	@Test
	void 프레임_추출_빈도가_허용_범위를_벗어나면_기동에_실패한다() {
		new ApplicationContextRunner()
				.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues(
						"meongcoach.llm.gemini.api-key=test-key",
						"meongcoach.llm.gemini.model=gemini-test",
						"meongcoach.llm.gemini.response-timeout=5m",
						"meongcoach.llm.gemini.max-output-tokens=4096",
						"meongcoach.llm.gemini.temperature=0",
						"meongcoach.llm.gemini.thinking-level=low",
						"meongcoach.llm.gemini.video-fps=6")
				.run(context -> assertThat(context).hasFailed());
	}

	@Test
	void 설정으로_Gemini_클라이언트를_생성한다() {
		new ApplicationContextRunner()
				.withUserConfiguration(TestConfiguration.class, GeminiClientConfiguration.class)
				.withPropertyValues(
						"meongcoach.llm.gemini.api-key=test-key",
						"meongcoach.llm.gemini.model=gemini-test",
						"meongcoach.llm.gemini.response-timeout=5m",
						"meongcoach.llm.gemini.max-output-tokens=4096",
						"meongcoach.llm.gemini.temperature=0",
						"meongcoach.llm.gemini.thinking-level=low",
						"meongcoach.llm.gemini.video-fps=1")
				.run(context -> assertThat(context).hasNotFailed().hasSingleBean(Client.class));
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(GeminiProperties.class)
	static class TestConfiguration {
	}
}
