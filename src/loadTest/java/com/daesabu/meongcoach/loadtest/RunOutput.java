package com.daesabu.meongcoach.loadtest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 실행 한 번의 산출물 위치. JMeter HTML 리포트 생성기는 출력 폴더가 비어 있어야 하므로 실행마다 타임스탬프 폴더를 새로 만든다.
 * 계정 자격(accounts.csv)만 실행 간에 재사용하도록 시나리오 폴더 밖에 둔다.
 */
public record RunOutput(Path root, Path dir) {

	private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

	public static RunOutput create(LoadTestConfig config, String scenario) {
		Path root = config.outputDir().toAbsolutePath();
		Path dir = root.resolve(scenario).resolve(LocalDateTime.now().format(STAMP));
		try {
			Files.createDirectories(dir);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return new RunOutput(root, dir);
	}

	public Path accountsCsv() {
		return root.resolve("accounts.csv");
	}

	public Path tokensCsv() {
		return dir.resolve("tokens.csv");
	}

	public Path jmx() {
		return dir.resolve("plan.jmx");
	}

	public Path reportDir() {
		return dir.resolve("report");
	}
}
