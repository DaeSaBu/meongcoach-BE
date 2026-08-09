package com.daesabu.meongcoach.ai.adapter.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
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
				Float.parseFloat(env.getOrDefault("BEDROCK_TEMPERATURE", "0.2")),
				env.getOrDefault("BEDROCK_PROMPT_VERSION", "v1"));

		try (BedrockRuntimeClient client = BedrockRuntimeClient.builder()
				.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
						properties.accessKeyId(), properties.secretAccessKey())))
				.region(Region.of(properties.region()))
				.overrideConfiguration(override -> override
						.apiCallTimeout(properties.responseTimeout())
						.apiCallAttemptTimeout(properties.responseTimeout()))
				.build()) {

			String content = new BedrockVideoAnalyzer(client, properties).analyze(VIDEO_S3_URI);

			System.out.println("===== 영상 분석 결과 =====");
			System.out.println(content);
		}
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
