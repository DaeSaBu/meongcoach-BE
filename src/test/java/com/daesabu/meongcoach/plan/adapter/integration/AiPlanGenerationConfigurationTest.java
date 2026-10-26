package com.daesabu.meongcoach.plan.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.daesabu.meongcoach.ai.application.provided.AiContentGenerator;
import com.daesabu.meongcoach.plan.application.required.AiPlanGenerator;
import java.net.URL;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

class AiPlanGenerationConfigurationTest {

	private final AiContentGenerator contentGenerator = mock(AiContentGenerator.class);
	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestConfiguration.class)
			.withBean(AiContentGenerator.class, () -> contentGenerator)
			.withBean(ObjectMapper.class, ObjectMapper::new);

	@Test
	void 실제_프롬프트를_읽고_생성기를_등록하되_AI를_호출하지_않는다() {
		contextRunner.run(context -> {
			assertThat(context).hasNotFailed().hasSingleBean(AiPlanGenerator.class);
			verifyNoInteractions(contentGenerator);
		});
	}

	@Test
	void 프롬프트_파일이_없으면_기동에_실패한다() {
		ClassLoader missingPromptClassLoader = new ClassLoader(getClass().getClassLoader()) {
			@Override
			public URL getResource(String name) {
				if (name.equals("prompts/plan-generation/system.md")) {
					return null;
				}
				return super.getResource(name);
			}
		};

		contextRunner.withClassLoader(missingPromptClassLoader).run(context -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(java.io.FileNotFoundException.class);
			verifyNoInteractions(contentGenerator);
		});
	}

	@Configuration(proxyBeanMethods = false)
	@ComponentScan(basePackageClasses = AiPlanGenerationAdapter.class)
	static class TestConfiguration {
	}
}
