package com.daesabu.meongcoach.benchmark.domain;

import org.springframework.test.util.ReflectionTestUtils;

public final class BenchmarkFixture {

	private BenchmarkFixture() {
	}

	public static Benchmark create(String title, String videoObjectKey) {
		Benchmark benchmark = new Benchmark();
		ReflectionTestUtils.setField(benchmark, "title", title);
		ReflectionTestUtils.setField(benchmark, "videoObjectKey", videoObjectKey);
		return benchmark;
	}
}
