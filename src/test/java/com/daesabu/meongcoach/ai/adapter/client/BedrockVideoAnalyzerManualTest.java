package com.daesabu.meongcoach.ai.adapter.client;

import com.daesabu.meongcoach.training.application.provided.TopicFinder;
import com.daesabu.meongcoach.training.application.provided.TopicSummary;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ServiceTierType;

/**
 * 프롬프트·영상 분석 고도화를 위한 임시 수동 테스트. 실제 Bedrock을 호출하므로 확인이 끝나면 삭제한다.
 * .env의 BEDROCK_* 값을 읽어 실행하고, 없으면 건너뛴다.
 */
class BedrockVideoAnalyzerManualTest {

	private static final String VIDEO_S3_URI = "s3://local-test-limj/videos/training/7/video2.mp4";

	@Test
	void analyzeRealVideo() throws Exception {
		Map<String, String> env = loadDotEnv();
		Assumptions.assumeTrue(env.containsKey("BEDROCK_ACCESS_KEY_ID"), ".env의 BEDROCK_* 값이 없어 건너뛴다");

		BedrockProperties properties = new BedrockProperties(
				env.get("BEDROCK_REGION"),
				env.get("BEDROCK_ACCESS_KEY_ID"),
				env.get("BEDROCK_SECRET_ACCESS_KEY"),
				env.getOrDefault("BEDROCK_MODEL", "global.amazon.nova-2-lite-v1:0"),
				Duration.ofMinutes(5),
				ServiceTierType.fromValue(env.getOrDefault("BEDROCK_SERVICE_TIER", "flex")),
				Integer.parseInt(env.getOrDefault("BEDROCK_MAX_TOKENS", "4096")),
				Float.parseFloat(env.getOrDefault("BEDROCK_TEMPERATURE", "0")));

		try (BedrockRuntimeClient client = BedrockRuntimeClient.builder()
				.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
						properties.accessKeyId(), properties.secretAccessKey())))
				.region(Region.of(properties.region()))
				.overrideConfiguration(override -> override
						.apiCallTimeout(properties.responseTimeout())
						.apiCallAttemptTimeout(properties.responseTimeout()))
				.build()) {

			String content = new BedrockVideoAnalyzer(client, properties, stubTopicFinder()).analyze(VIDEO_S3_URI);

			System.out.println("===== 영상 분석 결과 =====");
			System.out.println(content);
		}
	}

	// DB 없이 실행하는 수동 테스트라 운영 초기 데이터(training-initial-data.sql)의 토픽을 그대로 stub으로 쓴다
	private static TopicFinder stubTopicFinder() {
		return () -> List.of(
				new TopicSummary(101L, "앉아", "기본 자세부터 차근차근"),
				new TopicSummary(102L, "기다려", "차분하게 기다리는 연습"),
				new TopicSummary(103L, "배변", "편안한 배변 습관 만들기"),
				new TopicSummary(104L, "입질", "무는 습관 교정하기"),
				new TopicSummary(105L, "사회화", "다른 개·사람과 인사"),
				new TopicSummary(106L, "분리불안", "혼자서도 편안하게"),
				new TopicSummary(107L, "산책", "즐겁고 안전한 첫 산책"),
				new TopicSummary(108L, "켄넬", "켄넬을 편안한 공간으로"),
				new TopicSummary(109L, "개인기", "신호로 즐기는 재미있는 개인기"));
	}

	private static Map<String, String> loadDotEnv() throws Exception {
		Map<String, String> env = new HashMap<>();
		Path dotEnv = Path.of(".env");
		if (!Files.exists(dotEnv)) {
			return env;
		}
		for (String line : Files.readAllLines(dotEnv)) {
			int eq = line.indexOf('=');
			if (line.isBlank() || line.startsWith("#") || eq < 0) {
				continue;
			}
			env.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
		}
		return env;
	}
}
