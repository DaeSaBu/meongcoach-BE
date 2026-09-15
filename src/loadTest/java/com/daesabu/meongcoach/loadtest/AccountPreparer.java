package com.daesabu.meongcoach.loadtest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 인증이 필요한 시나리오의 사전 준비. 계정 생성 → 로그인 → (온보딩 전이면) 온보딩으로 정회원 승격 → (필요하면) AI 리포트 확보를
 * 실제 API로 수행하고 결과를 tokens.csv에 기록한다. 시드 SQL 없이 dev 서버에도 같은 절차로 데이터를 만들기 위해서다.
 * <p>
 * accounts.csv(이메일·비밀번호)는 실행 간 재사용하고 토큰은 매 실행 새로 발급한다 — 액세스 토큰은 1시간 만료, 리프레시 토큰은 1회용이다.
 */
public final class AccountPreparer {

	private static final String EMAIL_DOMAIN = "@meongcoach.test";
	private static final String PASSWORD = "loadtest-password";
	private static final String API_KEY_HEADER = "X-Loadtest-Key";

	// 온보딩 코드값은 각 모듈 enum 이름이다(user Gender·Mbti, dog Breed·DogSex·Personality). 강아지 1마리를 함께 등록해 /api/dogs에 데이터가 생긴다
	private static final String ONBOARDING_BODY = """
			{"nickname":"%s","birthDate":"1995-01-01","mbti":"ISTJ","gender":"NONE",
			 "priorTrainingTopicIds":[],"trainingGoalTopicIds":[],
			 "dogs":[{"name":"멍이","breed":"MALTESE","sex":"MALE","birthDate":"2022-01-01",
			          "weightKg":5.5,"personalities":["LIVELY"]}]}""";

	// 업로드는 하지 않고 UPLOADING 상태의 리포트 행만 만든다. 크기는 media의 상한(50MB) 안이면 된다
	private static final String REPORT_BODY = "{\"contentType\":\"video/mp4\",\"fileSizeBytes\":1048576}";

	private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
	private final ObjectMapper mapper = new ObjectMapper();
	private final LoadTestConfig config;

	public AccountPreparer(LoadTestConfig config) {
		this.config = config;
	}

	/** 계정을 준비해 tokens.csv에 쓰고 같은 내용을 돌려준다. */
	public List<PreparedAccount> prepare(RunOutput out, boolean withReport) {
		List<String[]> credentials = loadOrCreateCredentials(out.accountsCsv());
		List<PreparedAccount> accounts = new ArrayList<>();
		List<String> lines = new ArrayList<>();
		lines.add(PreparedAccount.CSV_HEADER);
		for (String[] credential : credentials) {
			PreparedAccount account = prepareOne(credential[0], credential[1], withReport);
			accounts.add(account);
			lines.add(account.toCsvLine());
		}
		write(out.tokensCsv(), lines);
		System.out.printf("[loadtest] 계정 %d개 준비 완료 → %s%n", credentials.size(), out.tokensCsv());
		return accounts;
	}

	// 레슨 ID는 환경마다 다르므로(시드 기준 11010101처럼 계층형) 선택된 토픽의 첫 커리큘럼에서 첫 레슨을 찾는다
	public long findAnyLessonId(String accessToken) {
		JsonNode curriculums = get("/api/training/curriculums", accessToken, "커리큘럼 목록").get("curriculums");
		if (curriculums == null || curriculums.isEmpty()) {
			throw new IllegalStateException("커리큘럼이 없어 레슨을 고를 수 없습니다. -Dloadtest.lessonId로 지정하세요");
		}
		long curriculumId = curriculums.get(0).get("curriculumId").asLong();
		JsonNode lessons = get("/api/training/curriculums/" + curriculumId, accessToken, "커리큘럼 상세").get("lessons");
		if (lessons == null || lessons.isEmpty()) {
			throw new IllegalStateException("커리큘럼 " + curriculumId + "에 레슨이 없습니다. -Dloadtest.lessonId로 지정하세요");
		}
		return lessons.get(0).get("lessonId").asLong();
	}

	private PreparedAccount prepareOne(String email, String password, boolean withReport) {
		JsonNode login = loginOrCreate(email, password);
		String accessToken = login.get("accessToken").asText();
		String refreshToken = login.get("refreshToken").asText();
		if (login.get("needsOnboarding").asBoolean()) {
			completeOnboarding(accessToken, email);
		}
		long reportId = 0;
		if (withReport) {
			reportId = ensureReport(accessToken);
		}
		return new PreparedAccount(email, password, accessToken, refreshToken, reportId);
	}

	// accounts.csv가 다른 서버(예: 로컬)에서 만든 것이면 dev에는 계정이 없어 401이 난다. 그때만 생성하고 다시 로그인한다
	private JsonNode loginOrCreate(String email, String password) {
		HttpResponse<String> response = post("/api/auth/login/local", loginBody(email, password), null);
		if (response.statusCode() == 401) {
			createAccount(email, password);
			response = post("/api/auth/login/local", loginBody(email, password), null);
		}
		return parse(response, "로그인", email);
	}

	private void createAccount(String email, String password) {
		HttpRequest request = builder("/api/loadtest/accounts")
				.header(API_KEY_HEADER, config.requireApiKey())
				.POST(BodyPublishers.ofString(loginBody(email, password)))
				.build();
		parse(send(request), "계정 생성", email);
	}

	private void completeOnboarding(String accessToken, String email) {
		String nickname = email.substring(0, Math.min(email.indexOf('@'), 50));
		parse(post("/api/onboarding", ONBOARDING_BODY.formatted(nickname), accessToken), "온보딩", email);
	}

	// 체험 횟수 제한이 있으므로 이미 리포트가 있으면 첫 번째를 재사용하고, 없을 때만 새로 만든다
	private long ensureReport(String accessToken) {
		JsonNode reports = get("/api/ai/reports", accessToken, "리포트 목록").get("reports");
		if (reports != null && !reports.isEmpty()) {
			return reports.get(0).get("id").asLong();
		}
		JsonNode created = parse(post("/api/ai/presigned-urls", REPORT_BODY, accessToken), "리포트 생성", "");
		return created.get("reportId").asLong();
	}

	private List<String[]> loadOrCreateCredentials(Path accountsCsv) {
		List<String[]> credentials = new ArrayList<>();
		if (Files.exists(accountsCsv)) {
			try {
				for (String line : Files.readAllLines(accountsCsv)) {
					if (!line.isBlank()) {
						credentials.add(line.split(",", 2));
					}
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		while (credentials.size() < config.accounts()) {
			String email = "lt-" + UUID.randomUUID().toString().substring(0, 8) + EMAIL_DOMAIN;
			credentials.add(new String[] {email, PASSWORD});
		}
		List<String> lines = credentials.stream().map(c -> c[0] + "," + c[1]).toList();
		write(accountsCsv, lines);
		return credentials.subList(0, config.accounts());
	}

	private JsonNode get(String path, String accessToken, String step) {
		HttpRequest request = builder(path).header("Authorization", "Bearer " + accessToken).GET().build();
		return parse(send(request), step, "");
	}

	private HttpResponse<String> post(String path, String body, String accessToken) {
		HttpRequest.Builder builder = builder(path).POST(BodyPublishers.ofString(body));
		if (accessToken != null) {
			builder.header("Authorization", "Bearer " + accessToken);
		}
		return send(builder.build());
	}

	private HttpRequest.Builder builder(String path) {
		return HttpRequest.newBuilder(URI.create(config.baseUrl() + path))
				.timeout(Duration.ofSeconds(15))
				.header("Content-Type", "application/json");
	}

	private HttpResponse<String> send(HttpRequest request) {
		try {
			return http.send(request, BodyHandlers.ofString());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("요청이 중단되었습니다", e);
		}
	}

	private JsonNode parse(HttpResponse<String> response, String step, String email) {
		if (response.statusCode() >= 300) {
			throw new IllegalStateException(step + " 실패 (" + email + "): HTTP " + response.statusCode() + " "
					+ response.body());
		}
		try {
			return mapper.readTree(response.body());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private String loginBody(String email, String password) {
		try {
			return mapper.writeValueAsString(Map.of("email", email, "password", password));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void write(Path path, List<String> lines) {
		try {
			Files.createDirectories(path.getParent());
			Files.write(path, lines);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
