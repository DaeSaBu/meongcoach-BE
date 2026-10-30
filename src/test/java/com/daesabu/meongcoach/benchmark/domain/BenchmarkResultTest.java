package com.daesabu.meongcoach.benchmark.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class BenchmarkResultTest {

	private static final String METRICS = "{\"accuracy\": {\"score\": 4, \"reason\": \"행동을 정확히 짚었다\"}}";

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 평가지표를_JSON_객체로_저장하고_그대로_읽는다() {
		Benchmark benchmark = entityManager.persist(BenchmarkFixture.create("짖음", "videos/benchmark/0/barking.mp4"));
		Long resultId = entityManager.persistAndFlush(BenchmarkResultFixture.create(benchmark, METRICS)).getId();
		entityManager.clear();

		Object jsonType = entityManager.getEntityManager()
				.createNativeQuery("SELECT jsonb_typeof(metrics) FROM benchmark_results WHERE id = :id")
				.setParameter("id", resultId)
				.getSingleResult();
		BenchmarkResult found = entityManager.find(BenchmarkResult.class, resultId);

		assertThat(jsonType).isEqualTo("object");
		assertThat(found.getMetrics()).contains("\"accuracy\"", "\"score\": 4", "행동을 정확히 짚었다");
	}

	@Test
	void 결과의_벤치마크는_지연_로딩한다() {
		Benchmark benchmark = entityManager.persist(BenchmarkFixture.create("짖음", "videos/benchmark/0/barking.mp4"));
		Long resultId = entityManager.persistAndFlush(BenchmarkResultFixture.create(benchmark, METRICS)).getId();
		entityManager.clear();

		BenchmarkResult found = entityManager.find(BenchmarkResult.class, resultId);

		assertThat(Hibernate.isInitialized(found.getBenchmark())).isFalse();
		assertThat(found.getBenchmark().getVideoObjectKey()).isEqualTo("videos/benchmark/0/barking.mp4");
	}
}
