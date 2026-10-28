package com.daesabu.meongcoach.ai.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

class EvoLinkConfigurationTest {

	@Test
	void 기존_설정_키로_연결과_모델_설정을_함께_바인딩하고_클라이언트를_생성한다() {
		new ApplicationContextRunner()
				.withUserConfiguration(TestConfiguration.class)
				.withBean(RestClient.Builder.class, RestClient::builder)
				.withBean(EvoLinkChatClient.class)
				.withPropertyValues(
						"meongcoach.ai.evolink.base-url=https://api.evolink.test",
						"meongcoach.ai.evolink.api-key=test-key",
						"meongcoach.ai.evolink.response-timeout=5m",
						"meongcoach.ai.evolink.model=test-model",
						"meongcoach.ai.evolink.max-tokens=4096",
						"meongcoach.ai.evolink.temperature=0.0",
						"meongcoach.ai.evolink.thinking=disabled")
				.run(context -> {
					assertThat(context).hasNotFailed().hasSingleBean(EvoLinkChatClient.class);
					EvoLinkProperties properties = context.getBean(EvoLinkProperties.class);
					assertThat(properties).isEqualTo(new EvoLinkProperties(
							"https://api.evolink.test", "test-key", Duration.ofMinutes(5),
							"test-model", 4096, 0.0, "disabled"));
				});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(EvoLinkProperties.class)
	static class TestConfiguration {
	}
}
