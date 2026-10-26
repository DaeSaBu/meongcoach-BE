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

import com.daesabu.meongcoach.dog.domain.shared.Personality;
import com.daesabu.meongcoach.dog.domain.shared.RegisteredBreed;
import com.daesabu.meongcoach.media.domain.ImageType;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingCompleter;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingImageUploadUrlIssuer;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingImageUploadUrlResult;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingMetadataFinder;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingMetadataResult;
import com.daesabu.meongcoach.onboarding.application.provided.dto.OnboardingCompleteRequest;
import com.daesabu.meongcoach.onboarding.domain.OnboardingImageTarget;
import java.util.Objects;
import com.daesabu.meongcoach.training.application.provided.dto.TopicResult;
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
			"https://test-account.r2.cloudflarestorage.com/test-bucket/images/dog-profile/1/uuid.jpg"
					+ "?X-Amz-Expires=600&X-Amz-Signature=example";
	private static final String IMAGE_PUBLIC_URL = "https://images.test.meongcoach.com/images/dog-profile/1/uuid.jpg";

	private static final String IMAGE_ISSUE_REQUEST = """
			{
				"contentType": "image/jpeg"
			}
			""";

	private static final String COMPLETE_REQUEST = """
			{
				"dogs": [
					{
						"name": "초코",
						"breed": "POODLE",
						"sex": "MALE",
						"birthDate": "2024-03-01",
						"personalities": ["TIMID", "LIVELY"],
						"profileImageUrl": "https://images.test.meongcoach.com/images/dog-profile/1/b.jpg"
					}
				]
			}
			""";

	private static final String COMPLETE_REQUEST_WITH_REQUIRED_FIELDS = """
			{
				"dogs": [
					{
						"name": "초코",
						"breed": "POODLE",
						"sex": "MALE",
						"birthDate": "2024-03-01"
					}
				]
			}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RecordingOnboardingCompleter onboardingCompleter;

	@Test
	void 보호자_프로필_없이_온보딩을_완료한다() throws Exception {
		mockMvc.perform(post("/api/onboarding")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(COMPLETE_REQUEST))
				.andExpect(status().isCreated());
	}

	@Test
	void 온보딩_메타데이터를_조회한다() throws Exception {
		mockMvc.perform(get("/api/onboarding/metadata")
						.principal(() -> "1")
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.topics[0].id").value(1))
				.andExpect(jsonPath("$.breeds[0].code").value("POODLE"))
				.andExpect(jsonPath("$.breeds[0].label").value("푸들"))
				.andExpect(jsonPath("$.breeds[0].aliases[0]").value("POODLE"))
				.andExpect(jsonPath("$.breeds[0].aliases[1]").value("CANICHE"))
				.andExpect(jsonPath("$.personalities[0].code").value("TIMID"))
				.andExpect(jsonPath("$.mbtis[0]").value("INTJ"))
				.andDo(document("onboarding/metadata",
						responseFields(
								fieldWithPath("topics[].id").description("구버전 앱의 교육 토픽 ID"),
								fieldWithPath("topics[].title").description("교육 토픽 이름"),
								fieldWithPath("mbtis[]").description("구버전 앱의 MBTI 선택 목록"),
								fieldWithPath("breeds[]").description("등록 견종 목록. 믹스견이 맨 앞이고 나머지는 한글 라벨 가나다순"),
								fieldWithPath("breeds[].code").description("강아지 견종 코드"),
								fieldWithPath("breeds[].label").description("강아지 견종 한글 라벨"),
								fieldWithPath("breeds[].aliases[]").description("견종 검색에 쓰는 한글·영문 별칭 목록. 라벨은 포함하지 않으며 대소문자·띄어쓰기만 다른 값은 하나만 둔다"),
								fieldWithPath("personalities[].code").description("강아지 성격 코드"),
								fieldWithPath("personalities[].label").description("강아지 성격 한글 라벨")
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
				.andExpect(jsonPath("$.dogIds.length()").value(1))
				.andExpect(jsonPath("$.dogIds[0]").value(1))
				.andDo(document("onboarding/complete",
						requestFields(
								fieldWithPath("nickname").type(org.springframework.restdocs.payload.JsonFieldType.STRING).optional().description("구버전 보호자 닉네임, 최대 50자. 생략하면 빈 문자열"),
								fieldWithPath("mbti").type(org.springframework.restdocs.payload.JsonFieldType.STRING).optional().description("구버전 MBTI. 생략하면 null"),
								fieldWithPath("profileImageUrl").type(org.springframework.restdocs.payload.JsonFieldType.STRING).optional().description("구버전 보호자 이미지 URL, 최대 512자"),
								fieldWithPath("priorTrainingTopicIds").type(org.springframework.restdocs.payload.JsonFieldType.ARRAY).optional().description("구버전 학습 토픽 ID 목록, 최대 100개"),
								fieldWithPath("trainingGoalTopicIds").type(org.springframework.restdocs.payload.JsonFieldType.ARRAY).optional().description("구버전 교육 목표 토픽 ID 목록, 최대 100개"),
								fieldWithPath("dogs[].weightKg").type(org.springframework.restdocs.payload.JsonFieldType.NUMBER).optional().description("구버전 몸무게, 양수. 생략하면 null"),
								fieldWithPath("dogs[].expectation").type(org.springframework.restdocs.payload.JsonFieldType.STRING).optional().description("구버전 교육 기대 사항, 최대 500자"),
								fieldWithPath("dogs[]").description("등록할 강아지 목록. 1~5마리. 3.2.0 앱은 1마리만 입력"),
								fieldWithPath("dogs[].name").description("강아지 이름. 필수 입력, 최대 50자"),
								fieldWithPath("dogs[].breed").description(
										"메타데이터 조회 응답에서 선택한 강아지 견종 코드. "
												+ "목록에 없는 견종은 견종 이름을 직접 입력. 최대 30자"),
								fieldWithPath("dogs[].sex").description("강아지 성별. `MALE` 또는 `FEMALE`"),
								fieldWithPath("dogs[].birthDate")
										.description("강아지 생년월일. 과거 날짜, 필수 입력"),
								fieldWithPath("dogs[].personalities").description("강아지 성격 코드 목록. 선택 입력").optional(),
								fieldWithPath("dogs[].profileImageUrl").description(
										"강아지 프로필 이미지 공개 URL. 이미지 업로드 URL 발급 API의 "
												+ "publicUrl. 최대 550자, 선택 입력").optional()
						),
						responseFields(
								fieldWithPath("dogIds").description("생성된 강아지 ID 목록")
						)
				));

		OnboardingCompleteRequest info = onboardingCompleter.lastInfo;
		assertThat(info.dogs().getFirst().profileImageUrl())
				.isEqualTo("https://images.test.meongcoach.com/images/dog-profile/1/b.jpg");
	}

	@Test
	void 구버전_보호자_프로필을_받고_성별과_보호자_생일은_무시한다() throws Exception {
		String request = COMPLETE_REQUEST.replace("\"dogs\": [",
				"\"nickname\": \"멍멍이집사\", \"gender\": \"FEMALE\", \"birthDate\": \"1998-01-01\", \"dogs\": [");

		mockMvc.perform(post("/api/onboarding")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isCreated());

		assertThat(onboardingCompleter.lastInfo.dogs()).hasSize(1);
		assertThat(onboardingCompleter.lastInfo.nickname()).isEqualTo("멍멍이집사");
	}

	@Test
	void 필수_입력만으로_온보딩을_완료한다() throws Exception {
		mockMvc.perform(post("/api/onboarding")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(COMPLETE_REQUEST_WITH_REQUIRED_FIELDS))
				.andExpect(status().isCreated());
	}

	@Test
	void 강아지가_5마리를_넘으면_온보딩_완료에_실패한다() throws Exception {
		String dog = """
				{
				    "name": "초코",
				    "breed": "POODLE",
				    "sex": "MALE",
				    "birthDate": "2024-03-01"
				}
				""";
		String request = COMPLETE_REQUEST_WITH_REQUIRED_FIELDS.replace(
				"\"dogs\": [", "\"dogs\": [" + (dog + ", ").repeat(5));

		assertValidationFails(request, "dogs");
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
	void 강아지_이름이_50자이면_온보딩을_완료할_수_있다() throws Exception {
		String request = COMPLETE_REQUEST.replace(
				"\"name\": \"초코\"",
				"\"name\": \"" + "가".repeat(50) + "\"");

		mockMvc.perform(post("/api/onboarding")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isCreated());
	}

	@Test
	void 강아지_이름이_50자를_넘으면_검증에_실패한다() throws Exception {
		String request = COMPLETE_REQUEST.replace(
				"\"name\": \"초코\"",
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
	void 강아지_생년월일이_없으면_검증에_실패한다() throws Exception {
		String request = COMPLETE_REQUEST.replace(
				"\"birthDate\": \"2024-03-01\",", "");

		assertValidationFails(request, "dogs[0].birthDate");
	}

	@Test
	void 강아지_생년월일이_과거가_아니면_검증에_실패한다() throws Exception {
		String request = COMPLETE_REQUEST.replace("2024-03-01", "2999-01-01");

		assertValidationFails(request, "dogs[0].birthDate");
	}

	@Test
	void 강아지_몸무게_없이_온보딩을_완료한다() throws Exception {
		mockMvc.perform(post("/api/onboarding")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(COMPLETE_REQUEST))
				.andExpect(status().isCreated());

		assertThat(onboardingCompleter.lastInfo.dogs().getFirst().toInfo().weightKg())
				.isNull();
	}

	@Test
	void 강아지_프로필_이미지_URL이_550자를_넘으면_검증에_실패한다() throws Exception {
		String request = COMPLETE_REQUEST.replace(
				"https://images.test.meongcoach.com/images/dog-profile/1/b.jpg",
				"https://example.com/" + "a".repeat(500));

		assertValidationFails(request, "dogs[0].profileImageUrl");
	}

	@Test
	void 강아지_목록에_null이_있으면_검증에_실패한다() throws Exception {
		String request = """
				{"dogs": [null]}
				""";

		assertValidationFails(request, "dogs[0]");
	}

	@Test
	void 구버전처럼_강아지를_두_마리_등록한다() throws Exception {
		String request = """
				{
				    "dogs": [
				        {
				            "name": "초코",
				            "breed": "POODLE",
				            "sex": "MALE",
				            "birthDate": "2024-03-01"
				        },
				        {
				            "name": "보리",
				            "breed": "POODLE",
				            "sex": "FEMALE",
				            "birthDate": "2024-03-01"
				        }
				    ]
				}
				""";

		mockMvc.perform(post("/api/onboarding")
				.principal(() -> "1")
				.contentType(MediaType.APPLICATION_JSON)
				.content(request))
				.andExpect(status().isCreated());
		assertThat(onboardingCompleter.lastInfo.dogs()).hasSize(2);
	}

	@Test
	void 강아지_없이_온보딩을_완료할_수_없다() throws Exception {
		mockMvc.perform(post("/api/onboarding")
						.principal(() -> "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"dogs": []}
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
	void 강아지_프로필_이미지_업로드_URL을_발급한다() throws Exception {
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
								fieldWithPath("target").type(org.springframework.restdocs.payload.JsonFieldType.STRING).optional()
										.description("USER_PROFILE 또는 DOG_PROFILE. 생략하면 DOG_PROFILE"),
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

	@Test
	void 구버전_입력값을_온보딩_서비스에_전달한다() throws Exception {
		String request = COMPLETE_REQUEST.replace("\"dogs\": [",
				"\"nickname\": \"보호자\", \"mbti\": \"INTJ\", \"priorTrainingTopicIds\": [1], "
						+ "\"trainingGoalTopicIds\": [2], \"dogs\": [")
				.replace("\"name\": \"초코\"", "\"name\": \"초코\", \"weightKg\": 4.5, \"expectation\": \"산책\"");

		mockMvc.perform(post("/api/onboarding").principal(() -> "1")
				.contentType(MediaType.APPLICATION_JSON).content(request))
				.andExpect(status().isCreated());

		OnboardingCompleteRequest saved = onboardingCompleter.lastInfo;
		assertThat(saved.mbti()).isEqualTo("INTJ");
		assertThat(saved.priorTrainingTopicIds()).containsExactly(1L);
		assertThat(saved.trainingGoalTopicIds()).containsExactly(2L);
		assertThat(saved.dogs().getFirst().weightKg()).isEqualByComparingTo("4.5");
		assertThat(saved.dogs().getFirst().expectation()).isEqualTo("산책");
	}

	@Test
	void 구버전_몸무게가_양수가_아니면_거절한다() throws Exception {
		String request = COMPLETE_REQUEST.replace("\"name\": \"초코\"", "\"name\": \"초코\", \"weightKg\": 0");
		assertValidationFails(request, "dogs[0].weightKg");
	}

	@Test
	void 구버전_보호자_사진_업로드_대상을_전달한다() throws Exception {
		mockMvc.perform(post("/api/onboarding/presigned-urls").principal(() -> "1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"target\":\"USER_PROFILE\",\"contentType\":\"image/jpeg\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.publicUrl").value(IMAGE_PUBLIC_URL.replace("dog-profile", "user-profile")));
	}

	@Test
	void 잘못된_업로드_대상은_400으로_거절한다() throws Exception {
		mockMvc.perform(post("/api/onboarding/presigned-urls").principal(() -> "1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"target\":\"INVALID\",\"contentType\":\"image/jpeg\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("ONBOARDING_INVALID_IMAGE_UPLOAD_TARGET"));
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
					List.of(new TopicResult(1L, "배변 훈련", null)),
					List.of(RegisteredBreed.POODLE, RegisteredBreed.MALTESE),
					List.of(Personality.TIMID, Personality.LIVELY), List.of("INTJ", "ENFP"));
		}

		@Bean
		RecordingOnboardingCompleter onboardingCompleter() {
			return new RecordingOnboardingCompleter();
		}

		@Bean
		OnboardingImageUploadUrlIssuer onboardingImageUploadUrlIssuer() {
			return (userId, request) -> {
				ImageType.fromContentType(request.contentType());
				OnboardingImageTarget target = OnboardingImageTarget.from(
						Objects.requireNonNullElse(request.target(), "DOG_PROFILE"));
				if (target == OnboardingImageTarget.USER_PROFILE) {
					return new OnboardingImageUploadUrlResult(UPLOAD_URL.replace("dog-profile", "user-profile"),
							IMAGE_PUBLIC_URL.replace("dog-profile", "user-profile"), 600L);
				}
				return new OnboardingImageUploadUrlResult(UPLOAD_URL, IMAGE_PUBLIC_URL, 600L);
			};
		}
	}

	static class RecordingOnboardingCompleter implements OnboardingCompleter {

		private OnboardingCompleteRequest lastInfo;

		@Override
		public List<Long> complete(Long userId, OnboardingCompleteRequest info) {
			if (userId == ONBOARDED_USER_ID) {
				throw new AlreadyOnboardedException();
			}
			lastInfo = info;
			return List.of(1L);
		}
	}
}
