package com.daesabu.meongcoach.benchmark.domain;

import org.springframework.test.util.ReflectionTestUtils;

public final class BenchmarkResultFixture {

	private BenchmarkResultFixture() {
	}

	public static BenchmarkResult create(Benchmark benchmark, String metrics) {
		BenchmarkResult result = new BenchmarkResult();
		ReflectionTestUtils.setField(result, "benchmark", benchmark);
		ReflectionTestUtils.setField(result, "metrics", metrics);
		return result;
	}
}
