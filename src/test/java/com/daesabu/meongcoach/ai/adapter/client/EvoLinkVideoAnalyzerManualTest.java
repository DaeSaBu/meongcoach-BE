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
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

/**
 * 프롬프트·영상 분석 고도화를 위한 임시 수동 테스트. 실제 EvoLink를 호출하므로 확인이 끝나면 삭제한다.
 * .env의 EVOLINK_API_KEY와 EVOLINK_TEST_VIDEO_URL(영상의 presigned GET URL)을 읽어 실행하고, 없으면 건너뛴다.
 */
class EvoLinkVideoAnalyzerManualTest {

	@Test
	void analyzeRealVideo() throws Exception {
		Map<String, String> env = loadDotEnv();
		Assumptions.assumeTrue(env.containsKey("EVOLINK_API_KEY"), ".env의 EVOLINK_API_KEY가 없어 건너뛴다");
		Assumptions.assumeTrue(env.containsKey("EVOLINK_TEST_VIDEO_URL"),
				".env의 EVOLINK_TEST_VIDEO_URL(presigned GET URL)이 없어 건너뛴다");

		EvoLinkProperties properties = new EvoLinkProperties(
				env.getOrDefault("EVOLINK_BASE_URL", "https://api.evolink.ai"),
				env.get("EVOLINK_API_KEY"),
				env.getOrDefault("EVOLINK_MODEL", "doubao-seed-2.0-pro"),
				Duration.ofMinutes(5),
				Integer.parseInt(env.getOrDefault("EVOLINK_MAX_TOKENS", "4096")),
				Double.parseDouble(env.getOrDefault("EVOLINK_TEMPERATURE", "0")),
				env.getOrDefault("EVOLINK_THINKING", "disabled"),
				Double.parseDouble(env.getOrDefault("EVOLINK_VIDEO_FPS", "1")));

		EvoLinkChatClient chatClient = new EvoLinkChatClient(properties, RestClient.builder());

		String content = new EvoLinkVideoAnalyzer(chatClient, properties, stubTopicFinder(), new ObjectMapper())
				.analyze(env.get("EVOLINK_TEST_VIDEO_URL"));

		System.out.println("===== 영상 분석 결과 =====");
		System.out.println(content);

		String title = new EvoLinkReportTitleGenerator(chatClient, properties).generateTitle(content);

		System.out.println("===== 리포트 제목 =====");
		System.out.println(title);
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
