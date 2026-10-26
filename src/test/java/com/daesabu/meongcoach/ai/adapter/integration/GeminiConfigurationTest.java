package com.daesabu.meongcoach.ai.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;

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
						"meongcoach.ai.gemini.api-key=test-key",
						"meongcoach.ai.gemini.model=gemini-test",
						"meongcoach.ai.gemini.response-timeout=5m",
						"meongcoach.ai.gemini.max-output-tokens=4096",
						"meongcoach.ai.gemini.temperature=0",
						"meongcoach.ai.gemini.thinking-level=low",
						"meongcoach.ai.gemini.video-fps=1")
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
						"meongcoach.ai.gemini.api-key=test-key",
						"meongcoach.ai.gemini.model=gemini-test",
						"meongcoach.ai.gemini.response-timeout=5m",
						"meongcoach.ai.gemini.max-output-tokens=4096",
						"meongcoach.ai.gemini.temperature=0",
						"meongcoach.ai.gemini.thinking-level=low",
						"meongcoach.ai.gemini.video-fps=6")
				.run(context -> assertThat(context).hasFailed());
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(GeminiProperties.class)
	static class TestConfiguration {
	}
}
