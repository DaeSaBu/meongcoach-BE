package com.daesabu.meongcoach.plan.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class PlanGenerationPropertiesTest {

	private static final String PREFIX = "meongcoach.plan.generation";

	@Test
	void 설정값을_바인딩한다() {
		Binder binder = new Binder(new MapConfigurationPropertySource(Map.of(
				PREFIX + ".call-timeout", "PT20S",
				PREFIX + ".total-budget", "PT50S")));

		PlanGenerationProperties properties = binder.bind(PREFIX, PlanGenerationProperties.class).get();

		assertThat(properties.callTimeout()).isEqualTo(Duration.ofSeconds(20));
		assertThat(properties.totalBudget()).isEqualTo(Duration.ofSeconds(50));
	}

	@Test
	void 값이_없으면_호출_30초_전체_75초가_기본이다() {
		Binder binder = new Binder(new MapConfigurationPropertySource(Map.of()));

		PlanGenerationProperties properties = binder.bindOrCreate(PREFIX, PlanGenerationProperties.class);

		assertThat(properties.callTimeout()).isEqualTo(Duration.ofSeconds(30));
		assertThat(properties.totalBudget()).isEqualTo(Duration.ofSeconds(75));
	}
}
