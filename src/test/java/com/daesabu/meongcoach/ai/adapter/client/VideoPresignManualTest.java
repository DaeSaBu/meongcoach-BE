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
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * EvoLink 수동 테스트에 쓸 영상 presigned GET URL을 실제로 발급하는 임시 수동 테스트.
 * 기능 확인이 끝나면 EvoLinkVideoAnalyzerManualTest와 함께 삭제한다.
 * 테스트 영상은 운영·개발 버킷이 아니라 전용 버킷(local-test-limj)에 있어 버킷·객체 키를 테스트 값으로
 * 고정하고, .env에서는 S3_* 자격 증명만 읽는다. 없으면 건너뛴다.
 * presign은 로컬 서명 연산이라 네트워크 호출이 없다. 발급 로직은 media 모듈 S3VideoStorage와 같다.
 */
class VideoPresignManualTest {

	private static final Duration DOWNLOAD_URL_VALIDITY = Duration.ofHours(1);
	// 기존 Bedrock 수동 테스트가 쓰던 s3://local-test-limj/videos/training/11/video2.mp4와 같은 영상이다
	static final String TEST_BUCKET = "local-test-limj";
	static final String TEST_VIDEO_KEY = "videos/training/11/video2.mp4";

	@Test
	void issueDownloadUrl() throws Exception {
		Map<String, String> env = loadDotEnv();
		Assumptions.assumeTrue(hasS3Credentials(env), ".env의 S3_* 값이 없어 건너뛴다");

		String objectKey = env.getOrDefault("EVOLINK_TEST_VIDEO_KEY", TEST_VIDEO_KEY);
		String url = presignDownloadUrl(env, objectKey);

		System.out.println("===== presigned GET URL (" + DOWNLOAD_URL_VALIDITY.toHours() + "시간 유효) =====");
		System.out.println(url);
	}

	static boolean hasS3Credentials(Map<String, String> env) {
		return env.containsKey("S3_REGION") && env.containsKey("S3_ACCESS_KEY_ID")
				&& env.containsKey("S3_SECRET_ACCESS_KEY");
	}

	static String presignDownloadUrl(Map<String, String> env, String objectKey) {
		try (S3Presigner presigner = S3Presigner.builder()
				.region(Region.of(env.get("S3_REGION")))
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(env.get("S3_ACCESS_KEY_ID"), env.get("S3_SECRET_ACCESS_KEY"))))
				.build()) {
			return presigner.presignGetObject(GetObjectPresignRequest.builder()
							.signatureDuration(DOWNLOAD_URL_VALIDITY)
							.getObjectRequest(GetObjectRequest.builder()
									.bucket(TEST_BUCKET)
									.key(objectKey)
									.build())
							.build())
					.url().toString();
		}
	}

	static Map<String, String> loadDotEnv() throws Exception {
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
			String value = line.substring(eq + 1).trim();
			// 값이 빈 항목(KEY=)은 미설정으로 취급해야 빈 URL을 실어 보내는 실수를 막는다
			if (value.isEmpty()) {
				continue;
			}
			env.put(line.substring(0, eq).trim(), value);
		}
		return env;
	}
}
