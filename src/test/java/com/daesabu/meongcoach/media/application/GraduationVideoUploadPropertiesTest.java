package com.daesabu.meongcoach.media.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class GraduationVideoUploadPropertiesTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestConfiguration.class);

	@Test
	void 설정한_실습_인증_상한을_바이트로_읽는다() {
		contextRunner.withPropertyValues("meongcoach.media.graduation.max-file-size-bytes=104857600")
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context.getBean(GraduationVideoUploadProperties.class).maxFileSizeBytes())
							.isEqualTo(104_857_600L);
				});
	}

	@Test
	void 상한이_0이면_기동에_실패한다() {
		contextRunner.withPropertyValues("meongcoach.media.graduation.max-file-size-bytes=0")
				.run(context -> assertThat(context).hasFailed());
	}

	@Test
	void 상한을_설정하지_않으면_기동에_실패한다() {
		contextRunner.run(context -> assertThat(context).hasFailed());
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(GraduationVideoUploadProperties.class)
	static class TestConfiguration {
	}
}
