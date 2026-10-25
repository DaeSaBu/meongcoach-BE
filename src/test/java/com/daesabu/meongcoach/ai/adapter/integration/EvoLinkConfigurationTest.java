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
	void 기존_설정_키로_통신_설정과_모델_설정을_분리해_바인딩하고_클라이언트를_생성한다() {
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
						"meongcoach.ai.evolink.thinking=disabled",
						"meongcoach.ai.evolink.video-fps=1.0")
				.run(context -> {
					assertThat(context).hasNotFailed().hasSingleBean(EvoLinkChatClient.class);
					EvoLinkConnectionProperties connection = context.getBean(EvoLinkConnectionProperties.class);
					assertThat(connection.baseUrl()).isEqualTo("https://api.evolink.test");
					assertThat(connection.apiKey()).isEqualTo("test-key");
					assertThat(connection.responseTimeout()).isEqualTo(Duration.ofMinutes(5));
					EvoLinkProperties model = context.getBean(EvoLinkProperties.class);
					assertThat(model).isEqualTo(new EvoLinkProperties("test-model", 4096, 0.0, "disabled", 1.0));
				});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties({EvoLinkConnectionProperties.class, EvoLinkProperties.class})
	static class TestConfiguration {
	}
}
