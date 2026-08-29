package com.daesabu.meongcoach.onboarding.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.dog.application.provided.BreedInfo;
import com.daesabu.meongcoach.dog.application.provided.PersonalityInfo;
import com.daesabu.meongcoach.media.domain.ImageType;
import com.daesabu.meongcoach.media.domain.ImageUploadTarget;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingCompleteInfo;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingCompleter;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingImageUploadUrlIssuer;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingImageUploadUrlResult;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingMetadataFinder;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingMetadataResult;
import com.daesabu.meongcoach.training.application.provided.TopicSummary;
import com.daesabu.meongcoach.user.domain.exception.AlreadyOnboardedException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OnboardingController.class)
@Import(OnboardingControllerTest.StubConfig.class)
@AutoConfigureRestDocs
class OnboardingControllerTest {

	private static final long ONBOARDED_USER_ID = 99L;

	private static final String UPLOAD_URL =
			"https://test-account.r2.cloudflarestorage.com/test-bucket/images/user-profile/1/uuid.jpg"
					+ "?X-Amz-Expires=600&X-Amz-Signature=example";
	private static final String IMAGE_PUBLIC_URL = "https://images.test.meongcoach.com/images/user-profile/1/uuid.jpg";

	private static final String IMAGE_ISSUE_REQUEST = """
			{
				"target": "USER_PROFILE",
				"contentType": "image/jpeg"
			}
			""";

	private static final String COMPLETE_REQUEST = """
			{
				"nickname": "멍멍이집사",
				"birthDate": "1998-01-01",
				"mbti": "INTJ",
				"gender": "FEMALE",
				"profileImageUrl": "https://images.test.meongcoach.com/images/user-profile/1/a.jpg",
				"priorTrainingTopicIds": [1, 2],
				"trainingGoalTopicIds": [2, 3],
				"dogs": [
					{
						"name": "초코",
						"breed": "POODLE",
						"sex": "MALE",
						"birthDate": "2024-03-01",
						"weightKg": 4.50,
						"personalities": ["TIMID", "LIVELY"],
						"profileImageUrl": "https://images.test.meongcoach.com/images/dog-profile/1/b.jpg",
						"expectation": "산책할 때 보호자에게 집중하면 좋겠어요."
					}
				]
			}
			""";

	private static final String COMPLETE_REQUEST_WITH_NULL_ARRAYS = """
			{
				"nickname": "멍멍이집사",
				"mbti": "INTJ",
				"gender": "NONE",
				"priorTrainingTopicIds": null,
				"trainingGoalTopicIds": [],
				"dogs": [
					{
						"name": "초코",
						"breed": "POODLE",
						"sex": "MALE",
						"weightKg": 4.50
					}
				]
			}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RecordingOnboardingCompleter onboardingCompleter;

	@Test
	void 온보딩_메타데이터를_조회한다() throws Exception {
		mockMvc.perform(get("/api/onboarding/metadata")
						.principal(() -> "1")
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.topics[0].title").value("배변 훈련"))
				.andExpect(jsonPath("$.breeds[0].code").value("POODLE"))
				.andExpect(jsonPath("$.breeds[0].label").value("푸들"))
				.andExpect(jsonPath("$.personalities[0].code").value("TIMID"))
				.andExpect(jsonPath("$.mbtis[0]").value("ISTJ"))
				.andDo(document("onboarding/metadata",
						responseFields(
								fieldWithPath("topics[].id").description("토픽 ID"),
								fieldWithPath("topics[].title").description("토픽 이름"),
								fieldWithPath("breeds[].code").description("강아지 견종 코드"),
								fieldWithPath("breeds[].label").description("강아지 견종 한글 라벨"),
								fieldWithPath("personalities[].code").description("강아지 성격 코드"),
								fieldWithPath("personalities[].label").description("강아지 성격 한글 라벨"),
								fieldWithPath("mbtis[]").description("선택 가능한 사람 MBTI 코드 목록")
						)
				));
	}

	@Test
	void 온보딩을_완료하면_생성된_강아지_ID_목록을_반환한다() throws Exception {
		mockMvc.perform(post("/api/onboarding")
						.principal(() -> "1")
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(COMPLETE_REQUEST))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.dogIds[0]").value(1))
				.andDo(document("onboarding/complete",
						requestFields(
								fieldWithPath("nickname").description("사용자 닉네임. 필수 입력, 최대 50자"),
								fieldWithPath("birthDate").description("사용자 생년월일. 과거 날짜, 선택 입력").optional(),
								fieldWithPath("mbti").description("사용자 MBTI 코드. 필수 입력"),
								fieldWithPath("gender").description(
										"사용자 성별. `MALE`/`FEMALE`/`NONE`(응답 안 함). 필수 입력"),
								fieldWithPath("profileImageUrl").description(
										"사용자 프로필 이미지 공개 URL. 이미지 업로드 URL 발급 API의 "
												+ "publicUrl. 최대 512자, 선택 입력").optional(),
								fieldWithPath("priorTrainingTopicIds").description(
										"이전에 교육한 양수 토픽 ID 배열 (최대 100개). "
												+ "미입력·null·빈 배열은 선택 없음").optional(),
								fieldWithPath("trainingGoalTopicIds").description(
										"앞으로 교육할 양수 목표 토픽 ID 배열 (최대 100개). "
												+ "미입력·null·빈 배열은 선택 없음").optional(),
								fieldWithPath("dogs[]").description("등록할 강아지 목록. 1마리 이상 5마리 이하"),
								fieldWithPath("dogs[].name").description("강아지 이름. 필수 입력, 최대 50자"),
								fieldWithPath("dogs[].breed").description(
										"메타데이터 조회 응답에서 선택한 강아지 견종 코드. 최대 30자"),
								fieldWithPath("dogs[].sex").description("강아지 성별. `MALE` 또는 `FEMALE`"),
								fieldWithPath("dogs[].birthDate")
										.description("강아지 생년월일. 과거 날짜, 선택 입력").optional(),
								fieldWithPath("dogs[].weightKg").description("0보다 큰 강아지 몸무게(kg)"),
								fieldWithPath("dogs[].personalities").description("강아지 성격 코드 목록. 선택 입력").optional(),
								fieldWithPath("dogs[].profileImageUrl").description(
										"강아지 프로필 이미지 공개 URL. 이미지 업로드 URL 발급 API의 "
												+ "publicUrl. 최대 512자, 선택 입력").optional(),
								fieldWithPath("dogs[].expectation").description(
										"강아지 교육 기대 사항 (최대 500자). 선택 입력").optional()
						),
						responseFields(
								fieldWithPath("dogIds").description("생성된 강아지 ID 목록")
						)
				));

		OnboardingCompleteInfo info = onboardingCompleter.lastInfo;
		assertThat(info.priorTrainingTopicIds()).containsExactlyInAnyOrder(1L, 2L);
		assertThat(info.trainingGoalTopicIds()).containsExactlyInAnyOrder(2L, 3L);
		assertThat(info.dogs().getFirst().expectation())
				.isEqualTo("산책할 때 보호자에게 집중하면 좋겠어요.");
	}

	@Test
	void 교육_토픽_배열이_null_또는_빈_배열이면_선택_없음으로_처리한다() throws Exception {
		mockMvc.perform(post("/api/onboarding")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(COMPLETE_REQUEST_WITH_NULL_ARRAYS))
				.andExpect(status().isCreated());

		OnboardingCompleteInfo info = onboardingCompleter.lastInfo;
		assertThat(info.priorTrainingTopicIds()).isEmpty();
		assertThat(info.trainingGoalTopicIds()).isEmpty();
		assertThat(info.dogs().getFirst().expectation()).isNull();
	}

	@Test
	void 중복된_교육_토픽_ID는_한_번만_전달한다() throws Exception {
		String request = COMPLETE_REQUEST.replace("\"priorTrainingTopicIds\": [1, 2]",
				"\"priorTrainingTopicIds\": [1, 1, 2]");

		mockMvc.perform(post("/api/onboarding")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isCreated());

		assertThat(onboardingCompleter.lastInfo.priorTrainingTopicIds())
				.containsExactlyInAnyOrder(1L, 2L);
	}

	@Test
	void 교육_토픽_ID가_양수가_아니면_검증에_실패한다() throws Exception {
		String request = COMPLETE_REQUEST.replace("\"priorTrainingTopicIds\": [1, 2]",
				"\"priorTrainingTopicIds\": [0]");

		mockMvc.perform(post("/api/onboarding")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("priorTrainingTopicIds[0]"));
	}

	@Test
	void 강아지가_5마리를_넘으면_온보딩_완료에_실패한다() throws Exception {
		String dog = """
				{ "name": "초코", "breed": "POODLE", "sex": "MALE", "weightKg": 4.50 }""";
		String request = COMPLETE_REQUEST_WITH_NULL_ARRAYS.replace(
				"\"dogs\": [", "\"dogs\": [" + (dog + ", ").repeat(5));

		assertValidationFails(request, "dogs");
	}

	@Test
	void 강아지_기대_사항이_500자를_넘으면_검증에_실패한다() throws Exception {
		String request = COMPLETE_REQUEST.replace(
				"산책할 때 보호자에게 집중하면 좋겠어요.", "가".repeat(501));

		mockMvc.perform(post("/api/onboarding")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("dogs[0].expectation"));
	}

	@Test
	void 이미_온보딩을_완료한_회원이면_409를_반환한다() throws Exception {
		mockMvc.perform(post("/api/onboarding")
						.principal(() -> String.valueOf(ONBOARDED_USER_ID))
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(COMPLETE_REQUEST))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("USER_ALREADY_ONBOARDED"))
				.andDo(document("onboarding/complete-error",
						responseFields(
								fieldWithPath("title").description("HTTP 상태 이름"),
								fieldWithPath("status").description("HTTP 상태 코드"),
								fieldWithPath("detail").description("사람이 읽을 수 있는 에러 설명"),
								fieldWithPath("instance").description("에러가 발생한 요청 경로"),
								fieldWithPath("code").description("클라이언트 분기용 에러 코드"),
								fieldWithPath("timestamp").description("에러 발생 시각(UTC)")
						)
				));
	}

	@Test
	void 닉네임이_비어_있으면_검증에_실패한다() throws Exception {
		mockMvc.perform(post("/api/onboarding")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(COMPLETE_REQUEST.replace("멍멍이집사", "")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("nickname"));
	}

	@Test
	void 닉네임이_50자를_넘으면_검증에_실패한다() throws Exception {
		String request = COMPLETE_REQUEST.replace("멍멍이집사", "가".repeat(51));

		assertValidationFails(request, "nickname");
	}

	@Test
	void 사용자_생년월일이_과거가_아니면_검증에_실패한다() throws Exception {
		String request = COMPLETE_REQUEST.replace("1998-01-01", "2999-01-01");

		assertValidationFails(request, "birthDate");
	}

	@Test
	void 사용자_프로필_이미지_URL이_512자를_넘으면_검증에_실패한다() throws Exception {
		String request = COMPLETE_REQUEST.replace(
				"https://images.test.meongcoach.com/images/user-profile/1/a.jpg",
				"https://example.com/" + "a".repeat(500));

		assertValidationFails(request, "profileImageUrl");
	}

	@Test
	void 교육_토픽_ID가_100개를_넘으면_검증에_실패한다() throws Exception {
		String request = COMPLETE_REQUEST.replace("\"priorTrainingTopicIds\": [1, 2]",
				"\"priorTrainingTopicIds\": [" + "1, ".repeat(100) + "1]");

		assertValidationFails(request, "priorTrainingTopicIds");
	}

	@Test
	void 목표_토픽_ID가_양수가_아니면_검증에_실패한다() throws Exception {
		String request = COMPLETE_REQUEST.replace("\"trainingGoalTopicIds\": [2, 3]",
				"\"trainingGoalTopicIds\": [0]");

		assertValidationFails(request, "trainingGoalTopicIds[0]");
	}

	@Test
	void MBTI가_null이면_검증에_실패한다() throws Exception {
		String request = COMPLETE_REQUEST.replace("\"mbti\": \"INTJ\"", "\"mbti\": null");

		mockMvc.perform(post("/api/onboarding")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("mbti"));
	}

	@Test
	void MBTI가_공백이면_검증에_실패한다() throws Exception {
		String request = COMPLETE_REQUEST.replace("\"mbti\": \"INTJ\"", "\"mbti\": \"   \"");

		mockMvc.perform(post("/api/onboarding")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("mbti"));
	}

	@Test
	void 성별이_null이면_검증에_실패한다() throws Exception {
		String request = COMPLETE_REQUEST.replace("\"gender\": \"FEMALE\"", "\"gender\": null");

		mockMvc.perform(post("/api/onboarding")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("gender"));
	}

	@Test
	void 성별이_공백이면_검증에_실패한다() throws Exception {
		String request = COMPLETE_REQUEST.replace("\"gender\": \"FEMALE\"", "\"gender\": \"   \"");

		mockMvc.perform(post("/api/onboarding")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("gender"));
	}

	@Test
	void 강아지_이름이_50자를_넘으면_검증에_실패한다() throws Exception {
		String request = COMPLETE_REQUEST.replace("\"name\": \"초코\"",
				"\"name\": \"" + "가".repeat(51) + "\"");

		assertValidationFails(request, "dogs[0].name");
	}

	@Test
	void 강아지_이름이_비어_있으면_검증에_실패한다() throws Exception {
		String request = COMPLETE_REQUEST.replace("\"name\": \"초코\"", "\"name\": \"\"");

		assertValidationFails(request, "dogs[0].name");
	}

	@Test
	void 강아지_견종_코드가_30자를_넘으면_검증에_실패한다() throws Exception {
		String request = COMPLETE_REQUEST.replace("\"breed\": \"POODLE\"",
				"\"breed\": \"" + "A".repeat(31) + "\"");

		assertValidationFails(request, "dogs[0].breed");
	}

	@Test
	void 강아지_견종_코드가_비어_있으면_검증에_실패한다() throws Exception {
		String request = COMPLETE_REQUEST.replace("\"breed\": \"POODLE\"", "\"breed\": \"\"");

		assertValidationFails(request, "dogs[0].breed");
	}

	@Test
	void 강아지_성별이_비어_있으면_검증에_실패한다() throws Exception {
		String request = COMPLETE_REQUEST.replace("\"sex\": \"MALE\"", "\"sex\": \"\"");

		assertValidationFails(request, "dogs[0].sex");
	}

	@Test
	void 강아지_생년월일이_과거가_아니면_검증에_실패한다() throws Exception {
		String request = COMPLETE_REQUEST.replace("2024-03-01", "2999-01-01");

		assertValidationFails(request, "dogs[0].birthDate");
	}

	@Test
	void 강아지_몸무게가_양수가_아니면_검증에_실패한다() throws Exception {
		String request = COMPLETE_REQUEST.replace("\"weightKg\": 4.50", "\"weightKg\": 0");

		assertValidationFails(request, "dogs[0].weightKg");
	}

	@Test
	void 강아지_몸무게가_없으면_검증에_실패한다() throws Exception {
		String request = COMPLETE_REQUEST.replace("\"weightKg\": 4.50,", "");

		assertValidationFails(request, "dogs[0].weightKg");
	}

	@Test
	void 강아지_프로필_이미지_URL이_512자를_넘으면_검증에_실패한다() throws Exception {
		String request = COMPLETE_REQUEST.replace(
				"https://images.test.meongcoach.com/images/dog-profile/1/b.jpg",
				"https://example.com/" + "a".repeat(500));

		assertValidationFails(request, "dogs[0].profileImageUrl");
	}

	@Test
	void 강아지_없이_온보딩을_완료할_수_없다() throws Exception {
		mockMvc.perform(post("/api/onboarding")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nickname": "멍멍이집사", "mbti": "INTJ", "gender": "FEMALE", "dogs": []}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("dogs"));
	}

	@Test
	void 인증_정보가_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(post("/api/onboarding")
						.contentType(MediaType.APPLICATION_JSON)
						.content(COMPLETE_REQUEST))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void 프로필_이미지_업로드_URL을_발급한다() throws Exception {
		mockMvc.perform(post("/api/onboarding/presigned-urls")
						.principal(() -> "1")
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(IMAGE_ISSUE_REQUEST))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.uploadUrl").value(UPLOAD_URL))
				.andExpect(jsonPath("$.publicUrl").value(IMAGE_PUBLIC_URL))
				.andExpect(jsonPath("$.expiresInSeconds").value(600))
				.andDo(document("onboarding/image-upload-url",
						requestFields(
								fieldWithPath("target").description(
										"필수 입력. 업로드 대상. `USER_PROFILE`(사용자 프로필) 또는 "
												+ "`DOG_PROFILE`(강아지 프로필)"),
								fieldWithPath("contentType").description(
										"필수 입력. 업로드할 이미지의 Content-Type. `image/jpeg`, `image/png`, `image/webp`만 지원")
						),
						responseFields(
								fieldWithPath("uploadUrl").description(
										"이미지를 PUT할 presigned URL. 요청한 Content-Type과 동일하게 업로드해야 한다"),
								fieldWithPath("publicUrl").description(
										"업로드 완료 후 이미지가 공개되는 URL. 온보딩 완료 요청 등에 이 값을 담아 등록한다"),
								fieldWithPath("expiresInSeconds").description("uploadUrl의 유효 시간(초)")
						)
				));
	}

	@Test
	void 지원하지_않는_이미지_형식이면_400을_반환한다() throws Exception {
		mockMvc.perform(post("/api/onboarding/presigned-urls")
						.principal(() -> "1")
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(IMAGE_ISSUE_REQUEST.replace("image/jpeg", "image/gif")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MEDIA_UNSUPPORTED_IMAGE_TYPE"))
				.andDo(document("onboarding/image-upload-url-error",
						responseFields(
								fieldWithPath("title").description("HTTP 상태 이름"),
								fieldWithPath("status").description("HTTP 상태 코드"),
								fieldWithPath("detail").description("사람이 읽을 수 있는 에러 설명"),
								fieldWithPath("instance").description("에러가 발생한 요청 경로"),
								fieldWithPath("code").description("클라이언트 분기용 에러 코드"),
								fieldWithPath("timestamp").description("에러 발생 시각(UTC)")
						)
				));
	}

	@Test
	void 지원하지_않는_업로드_대상이면_400을_반환한다() throws Exception {
		mockMvc.perform(post("/api/onboarding/presigned-urls")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(IMAGE_ISSUE_REQUEST.replace("USER_PROFILE", "BANNER")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MEDIA_INVALID_UPLOAD_TARGET"));
	}

	@Test
	void 업로드_대상이_비어_있으면_검증에_실패한다() throws Exception {
		mockMvc.perform(post("/api/onboarding/presigned-urls")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"target\": \"\", \"contentType\": \"image/jpeg\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("target"));
	}

	@Test
	void 이미지_contentType이_비어_있으면_검증에_실패한다() throws Exception {
		mockMvc.perform(post("/api/onboarding/presigned-urls")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(IMAGE_ISSUE_REQUEST.replace("image/jpeg", "")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("contentType"));
	}

	@Test
	void 이미지_업로드_URL_발급_시_인증_정보가_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(post("/api/onboarding/presigned-urls")
						.contentType(MediaType.APPLICATION_JSON)
						.content(IMAGE_ISSUE_REQUEST))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	private void assertValidationFails(String request, String field) throws Exception {
		mockMvc.perform(post("/api/onboarding")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value(field));
	}

	@TestConfiguration
	static class StubConfig {

		@Bean
		OnboardingMetadataFinder onboardingMetadataFinder() {
			return () -> new OnboardingMetadataResult(
					List.of(new TopicSummary(1L, "배변 훈련", "편안한 배변 습관 만들기"),
								new TopicSummary(2L, "산책 훈련", "즐겁고 안전한 첫 산책")),
					List.of(new BreedInfo("POODLE", "푸들"), new BreedInfo("MALTESE", "말티즈")),
					List.of(new PersonalityInfo("TIMID", "소심함"), new PersonalityInfo("LIVELY", "활발함")),
					List.of("ISTJ", "INTJ"));
		}

		@Bean
		RecordingOnboardingCompleter onboardingCompleter() {
			return new RecordingOnboardingCompleter();
		}

		// target·contentType 검증은 media 도메인이 수행하므로 스텁에서도 실제 변환을 태워 400 경로를 재현한다
		@Bean
		OnboardingImageUploadUrlIssuer onboardingImageUploadUrlIssuer() {
			return (userId, target, contentType) -> {
				ImageUploadTarget.from(target);
				ImageType.fromContentType(contentType);
				return new OnboardingImageUploadUrlResult(UPLOAD_URL, IMAGE_PUBLIC_URL, 600L);
			};
		}
	}

	static class RecordingOnboardingCompleter implements OnboardingCompleter {

		private OnboardingCompleteInfo lastInfo;

		@Override
		public List<Long> complete(Long userId, OnboardingCompleteInfo info) {
			if (userId == ONBOARDED_USER_ID) {
				throw new AlreadyOnboardedException();
			}
			lastInfo = info;
			return List.of(1L, 2L);
		}
	}
}
