package com.daesabu.meongcoach.dog.adapter.webapi;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.dog.application.provided.DogProfileUpdateInfo;
import com.daesabu.meongcoach.dog.application.provided.DogProfileUpdater;
import com.daesabu.meongcoach.dog.domain.Breed;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogRegisterCommand;
import com.daesabu.meongcoach.dog.domain.DogSex;
import com.daesabu.meongcoach.dog.domain.Personality;
import com.daesabu.meongcoach.dog.domain.exception.DogNotFoundException;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 강아지 프로필 조회·수정 API 검증.
 */
@WebMvcTest(DogController.class)
@AutoConfigureRestDocs
class DogControllerTest {

	// 컨트롤러 슬라이스에는 필터 체인이 없으므로 인증 주체를 요청에 직접 실어 보낸다 (test-convention.md)
	private static final Principal CURRENT_USER = () -> "42";

	private static final String IMAGE_URL = "https://images.test.meongcoach.com/images/dog-profile/42/a.jpg";
	private static final String EXPECTATION = "보호자와 즐겁게 교육받고 싶어요.";

	private static final String NEW_IMAGE_URL = "https://images.test.meongcoach.com/images/dog-profile/42/b.jpg";
	private static final String NEW_EXPECTATION = "산책 예절을 배우고 싶어요.";
	private static final String UPDATE_BODY = """
			{
			  "name": "보리",
			  "breed": "MALTESE",
			  "sex": "FEMALE",
			  "birthDate": "2023-01-15",
			  "weightKg": 3.2,
			  "personalities": ["TIMID", "LIVELY"],
			  "profileImageUrl": "%s",
			  "expectation": "%s"
			}
			""".formatted(NEW_IMAGE_URL, NEW_EXPECTATION);

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DogProfileFinder dogProfileFinder;

	@MockitoBean
	private DogProfileUpdater dogProfileUpdater;

	@Test
	@DisplayName("보유 강아지 목록을 등록 순으로 반환한다")
	void findDogsReturnsDogsInRegistrationOrder() throws Exception {
		Dog selected = selectedDog(10L);
		Dog unselected = unselectedDog(11L);
		given(dogProfileFinder.findDogs(42L)).willReturn(List.of(selected, unselected));

		mockMvc.perform(get("/api/dogs")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dogs[0].dogId").value(10))
				.andExpect(jsonPath("$.dogs[0].name").value("초코"))
				.andExpect(jsonPath("$.dogs[0].breed.code").value("POODLE"))
				.andExpect(jsonPath("$.dogs[0].breed.label").value("푸들"))
				.andExpect(jsonPath("$.dogs[0].sex").value("MALE"))
				.andExpect(jsonPath("$.dogs[0].birthDate").value("2024-03-01"))
				.andExpect(jsonPath("$.dogs[0].age").isNumber())
				.andExpect(jsonPath("$.dogs[0].weightKg").value(4.5))
				.andExpect(jsonPath("$.dogs[0].status").value("SELECTED"))
				.andExpect(jsonPath("$.dogs[0].profileImageUrl").value(IMAGE_URL))
				.andExpect(jsonPath("$.dogs[0].expectation").value(EXPECTATION))
				.andExpect(jsonPath("$.dogs[0].personalities[0].code").value("TIMID"))
				.andExpect(jsonPath("$.dogs[0].personalities[0].label").value("소심함"))
				.andExpect(jsonPath("$.dogs[0].personalities[1].code").value("FRIENDLY"))
				.andExpect(jsonPath("$.dogs[1].dogId").value(11))
				.andExpect(jsonPath("$.dogs[1].status").value("UNSELECTED"))
				.andExpect(jsonPath("$.dogs[1].birthDate").value(nullValue()))
				.andExpect(jsonPath("$.dogs[1].age").value(nullValue()))
				.andExpect(jsonPath("$.dogs[1].personalities").isEmpty())
				.andDo(document("dog/list",
						responseFields(
								fieldWithPath("dogs[]").description("보유 강아지 목록. 등록 순"),
								fieldWithPath("dogs[].dogId").description("강아지 ID"),
								fieldWithPath("dogs[].name").description("강아지 이름"),
								fieldWithPath("dogs[].breed").description("견종"),
								fieldWithPath("dogs[].breed.code").description("견종 코드"),
								fieldWithPath("dogs[].breed.label").description("견종 표시명"),
								fieldWithPath("dogs[].sex").description("성별(MALE, FEMALE)"),
								fieldWithPath("dogs[].birthDate").optional()
										.description("생년월일(yyyy-MM-dd). 나이 미상이면 null"),
								fieldWithPath("dogs[].age").optional()
										.description("생년월일로 계산한 만 나이. 나이 미상이면 null"),
								fieldWithPath("dogs[].weightKg").description("몸무게(kg)"),
								fieldWithPath("dogs[].status").description("선택 상태(SELECTED, UNSELECTED). 사용자당 SELECTED는 한 마리"),
								fieldWithPath("dogs[].profileImageUrl")
										.description("프로필 이미지 공개 URL. 등록하지 않았으면 빈 문자열"),
								fieldWithPath("dogs[].expectation").description("교육 기대 사항. 입력하지 않았으면 빈 문자열"),
								fieldWithPath("dogs[].personalities[]").description("성격 목록. 선언 순 정렬, 없으면 빈 배열"),
								fieldWithPath("dogs[].personalities[].code").description("성격 코드"),
								fieldWithPath("dogs[].personalities[].label").description("성격 표시명")
						)
				));
	}

	@Test
	@DisplayName("강아지가 없으면 빈 배열과 200을 반환한다")
	void findDogsReturnsEmptyArrayWhenNoDogExists() throws Exception {
		given(dogProfileFinder.findDogs(42L)).willReturn(List.of());

		mockMvc.perform(get("/api/dogs").principal(CURRENT_USER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dogs").isArray())
				.andExpect(jsonPath("$.dogs").isEmpty());
	}

	@Test
	@DisplayName("인증 주체에서 읽은 사용자로 목록 조회를 위임한다")
	void findDogsDelegatesWithCurrentUserId() throws Exception {
		given(dogProfileFinder.findDogs(42L)).willReturn(List.of());

		mockMvc.perform(get("/api/dogs").principal(CURRENT_USER))
				.andExpect(status().isOk());

		then(dogProfileFinder).should().findDogs(42L);
	}

	@Test
	@DisplayName("인증 정보가 없으면 목록 조회는 401을 반환한다")
	void findDogsReturnsUnauthorizedWhenNotAuthenticated() throws Exception {
		mockMvc.perform(get("/api/dogs"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("강아지 한 마리의 프로필을 반환한다")
	void findDogReturnsDogProfile() throws Exception {
		given(dogProfileFinder.findDog(42L, 10L)).willReturn(selectedDog(10L));

		mockMvc.perform(get("/api/dogs/{dogId}", 10L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dogId").value(10))
				.andExpect(jsonPath("$.name").value("초코"))
				.andExpect(jsonPath("$.breed.code").value("POODLE"))
				.andExpect(jsonPath("$.breed.label").value("푸들"))
				.andExpect(jsonPath("$.sex").value("MALE"))
				.andExpect(jsonPath("$.birthDate").value("2024-03-01"))
				.andExpect(jsonPath("$.age").isNumber())
				.andExpect(jsonPath("$.weightKg").value(4.5))
				.andExpect(jsonPath("$.status").value("SELECTED"))
				.andExpect(jsonPath("$.profileImageUrl").value(IMAGE_URL))
				.andExpect(jsonPath("$.expectation").value(EXPECTATION))
				.andExpect(jsonPath("$.personalities[0].code").value("TIMID"))
				.andExpect(jsonPath("$.personalities[1].code").value("FRIENDLY"))
				.andDo(document("dog/detail",
						pathParameters(
								parameterWithName("dogId").description("조회할 강아지 ID")
						),
						responseFields(
								fieldWithPath("dogId").description("강아지 ID"),
								fieldWithPath("name").description("강아지 이름"),
								fieldWithPath("breed").description("견종"),
								fieldWithPath("breed.code").description("견종 코드"),
								fieldWithPath("breed.label").description("견종 표시명"),
								fieldWithPath("sex").description("성별(MALE, FEMALE)"),
								fieldWithPath("birthDate").optional()
										.description("생년월일(yyyy-MM-dd). 나이 미상이면 null"),
								fieldWithPath("age").optional()
										.description("생년월일로 계산한 만 나이. 나이 미상이면 null"),
								fieldWithPath("weightKg").description("몸무게(kg)"),
								fieldWithPath("status").description("선택 상태(SELECTED, UNSELECTED). 사용자당 SELECTED는 한 마리"),
								fieldWithPath("profileImageUrl")
										.description("프로필 이미지 공개 URL. 등록하지 않았으면 빈 문자열"),
								fieldWithPath("expectation").description("교육 기대 사항. 입력하지 않았으면 빈 문자열"),
								fieldWithPath("personalities[]").description("성격 목록. 선언 순 정렬, 없으면 빈 배열"),
								fieldWithPath("personalities[].code").description("성격 코드"),
								fieldWithPath("personalities[].label").description("성격 표시명")
						)
				));
	}

	@Test
	@DisplayName("인증 주체에서 읽은 사용자와 경로의 강아지 ID로 단건 조회를 위임한다")
	void findDogDelegatesWithCurrentUserIdAndDogId() throws Exception {
		given(dogProfileFinder.findDog(42L, 10L)).willReturn(selectedDog(10L));

		mockMvc.perform(get("/api/dogs/{dogId}", 10L).principal(CURRENT_USER))
				.andExpect(status().isOk());

		then(dogProfileFinder).should().findDog(42L, 10L);
	}

	@Test
	@DisplayName("없거나 본인 소유가 아닌 강아지면 404와 에러 코드를 반환한다")
	void findDogReturnsNotFoundWhenDogDoesNotExistOrIsNotOwned() throws Exception {
		given(dogProfileFinder.findDog(42L, 999L)).willThrow(new DogNotFoundException(999L));

		mockMvc.perform(get("/api/dogs/{dogId}", 999L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.code").value("DOG_NOT_FOUND"))
				.andExpect(jsonPath("$.detail").value("id가 999인 강아지를 찾을 수 없습니다."))
				.andDo(document("dog/detail-error",
						pathParameters(
								parameterWithName("dogId").description("조회할 강아지 ID")
						),
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
	@DisplayName("인증 정보가 없으면 단건 조회는 401을 반환한다")
	void findDogReturnsUnauthorizedWhenNotAuthenticated() throws Exception {
		mockMvc.perform(get("/api/dogs/{dogId}", 10L))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("선택된 강아지의 id와 프로필 이미지 URL을 반환한다")
	void findProfileImageReturnsImageUrl() throws Exception {
		given(dogProfileFinder.findSelectedDog(42L)).willReturn(selectedDog(10L));

		mockMvc.perform(get("/api/dogs/profile/image")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dogId").value(10))
				.andExpect(jsonPath("$.profileImageUrl").value(IMAGE_URL))
				.andDo(document("dog/profile-image",
						responseFields(
								fieldWithPath("dogId").description("선택된 강아지 ID"),
								fieldWithPath("profileImageUrl")
										.description("강아지 프로필 이미지 공개 URL. 등록하지 않았으면 빈 문자열")
						)
				));
	}

	@Test
	@DisplayName("로그인한 사용자 ID로 선택된 강아지의 프로필 이미지를 조회한다")
	void findProfileImageWithCurrentUserId() throws Exception {
		given(dogProfileFinder.findSelectedDog(42L)).willReturn(selectedDog(10L));

		mockMvc.perform(get("/api/dogs/profile/image").principal(CURRENT_USER))
				.andExpect(status().isOk());

		then(dogProfileFinder).should().findSelectedDog(42L);
	}

	@Test
	@DisplayName("프로필 이미지가 없는 강아지는 빈 문자열과 200을 반환한다")
	void findProfileImageReturnsEmptyStringWhenImageIsAbsent() throws Exception {
		Dog dogWithoutImage = selectedDog(10L);
		dogWithoutImage.changeProfileImage("");
		given(dogProfileFinder.findSelectedDog(42L)).willReturn(dogWithoutImage);

		mockMvc.perform(get("/api/dogs/profile/image").principal(CURRENT_USER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dogId").value(10))
				.andExpect(jsonPath("$.profileImageUrl").value(""));
	}

	@Test
	@DisplayName("선택된 강아지가 없으면 404와 에러 코드를 반환한다")
	void findProfileImageReturnsNotFoundWhenNoDogIsSelected() throws Exception {
		given(dogProfileFinder.findSelectedDog(42L)).willThrow(new DogNotFoundException());

		mockMvc.perform(get("/api/dogs/profile/image")
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.code").value("DOG_NOT_FOUND"))
				.andExpect(jsonPath("$.detail").value("선택된 강아지를 찾을 수 없습니다."))
				.andDo(document("dog/profile-image-error",
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
	@DisplayName("인증 정보가 없으면 프로필 이미지 조회는 401을 반환한다")
	void findProfileImageReturnsUnauthorizedWhenNotAuthenticated() throws Exception {
		mockMvc.perform(get("/api/dogs/profile/image"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void 소유_강아지의_프로필을_전체_교체하고_수정된_프로필을_반환한다() throws Exception {
		given(dogProfileUpdater.update(eq(42L), eq(10L), any(DogProfileUpdateInfo.class))).willReturn(updatedDog(10L));

		mockMvc.perform(put("/api/dogs/{dogId}", 10L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(UPDATE_BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dogId").value(10))
				.andExpect(jsonPath("$.name").value("보리"))
				.andExpect(jsonPath("$.breed.code").value("MALTESE"))
				.andExpect(jsonPath("$.breed.label").value("말티즈"))
				.andExpect(jsonPath("$.sex").value("FEMALE"))
				.andExpect(jsonPath("$.birthDate").value("2023-01-15"))
				.andExpect(jsonPath("$.age").isNumber())
				.andExpect(jsonPath("$.weightKg").value(3.2))
				.andExpect(jsonPath("$.status").value("SELECTED"))
				.andExpect(jsonPath("$.profileImageUrl").value(NEW_IMAGE_URL))
				.andExpect(jsonPath("$.expectation").value(NEW_EXPECTATION))
				.andExpect(jsonPath("$.personalities[0].code").value("TIMID"))
				.andExpect(jsonPath("$.personalities[1].code").value("LIVELY"))
				.andDo(document("dog/update",
						pathParameters(
								parameterWithName("dogId").description("수정할 강아지 ID")
						),
						requestFields(
								fieldWithPath("name").description("필수 입력. 강아지 이름(최대 50자)"),
								fieldWithPath("breed").description("필수 입력. 견종 코드(온보딩 메타데이터 breeds의 code)"),
								fieldWithPath("sex").description("필수 입력. 성별(MALE, FEMALE)"),
								fieldWithPath("birthDate").optional()
										.description("생년월일(yyyy-MM-dd, 과거 날짜). 나이 미상이면 생략하거나 null"),
								fieldWithPath("weightKg").description("필수 입력. 몸무게(kg, 양수)"),
								fieldWithPath("personalities").optional()
										.description("성격 코드 목록(온보딩 메타데이터 personalities의 code). 생략하거나 null이면 빈 목록으로 저장"),
								fieldWithPath("profileImageUrl").optional()
										.description("프로필 이미지 공개 URL(최대 512자). 생략하거나 null이면 빈 문자열로 저장"),
								fieldWithPath("expectation").optional()
										.description("교육 기대 사항(최대 500자). 생략하거나 null이면 빈 문자열로 저장")
						),
						responseFields(
								fieldWithPath("dogId").description("강아지 ID"),
								fieldWithPath("name").description("강아지 이름"),
								fieldWithPath("breed").description("견종"),
								fieldWithPath("breed.code").description("견종 코드"),
								fieldWithPath("breed.label").description("견종 표시명"),
								fieldWithPath("sex").description("성별(MALE, FEMALE)"),
								fieldWithPath("birthDate").optional()
										.description("생년월일(yyyy-MM-dd). 나이 미상이면 null"),
								fieldWithPath("age").optional()
										.description("생년월일로 계산한 만 나이. 나이 미상이면 null"),
								fieldWithPath("weightKg").description("몸무게(kg)"),
								fieldWithPath("status").description("선택 상태(SELECTED, UNSELECTED). 수정으로 바뀌지 않음"),
								fieldWithPath("profileImageUrl")
										.description("프로필 이미지 공개 URL. 등록하지 않았으면 빈 문자열"),
								fieldWithPath("expectation").description("교육 기대 사항. 입력하지 않았으면 빈 문자열"),
								fieldWithPath("personalities[]").description("성격 목록. 선언 순 정렬, 없으면 빈 배열"),
								fieldWithPath("personalities[].code").description("성격 코드"),
								fieldWithPath("personalities[].label").description("성격 표시명")
						)
				));
	}

	@Test
	void 인증_주체와_경로의_강아지_ID_요청_본문으로_수정을_위임한다() throws Exception {
		given(dogProfileUpdater.update(eq(42L), eq(10L), any(DogProfileUpdateInfo.class))).willReturn(updatedDog(10L));

		mockMvc.perform(put("/api/dogs/{dogId}", 10L)
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content(UPDATE_BODY))
				.andExpect(status().isOk());

		then(dogProfileUpdater).should().update(42L, 10L, new DogProfileUpdateInfo("보리", "MALTESE", "FEMALE",
				LocalDate.of(2023, 1, 15), new BigDecimal("3.2"), Set.of("TIMID", "LIVELY"), NEW_IMAGE_URL,
				NEW_EXPECTATION));
	}

	@Test
	void 이름이_비어_있으면_수정은_검증에_실패한다() throws Exception {
		mockMvc.perform(put("/api/dogs/{dogId}", 10L)
						.principal(CURRENT_USER)
						.contentType(MediaType.APPLICATION_JSON)
						.content(UPDATE_BODY.replace("\"보리\"", "\" \"")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.errors[0].field").value("name"));

		then(dogProfileUpdater).shouldHaveNoInteractions();
	}

	@Test
	void 없거나_본인_소유가_아닌_강아지를_수정하면_404와_에러_코드를_반환한다() throws Exception {
		given(dogProfileUpdater.update(eq(42L), eq(999L), any(DogProfileUpdateInfo.class)))
				.willThrow(new DogNotFoundException(999L));

		mockMvc.perform(put("/api/dogs/{dogId}", 999L)
						.principal(CURRENT_USER)
						.header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(UPDATE_BODY))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.code").value("DOG_NOT_FOUND"))
				.andExpect(jsonPath("$.detail").value("id가 999인 강아지를 찾을 수 없습니다."))
				.andDo(document("dog/update-error",
						pathParameters(
								parameterWithName("dogId").description("수정할 강아지 ID")
						),
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
	void 인증_정보가_없으면_수정은_401을_반환한다() throws Exception {
		mockMvc.perform(put("/api/dogs/{dogId}", 10L)
						.contentType(MediaType.APPLICATION_JSON)
						.content(UPDATE_BODY))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	// 슬라이스 테스트는 DB를 거치지 않아 JPA가 id를 채우지 않으므로, 응답의 dogId를 검증하려면 리플렉션으로 주입한다
	private Dog selectedDog(Long id) {
		Dog dog = Dog.register(new DogRegisterCommand(42L, "초코", Breed.POODLE, DogSex.MALE,
				LocalDate.of(2024, 3, 1), new BigDecimal("4.50"), IMAGE_URL, EXPECTATION));
		dog.select();
		dog.changePersonalities(Set.of(Personality.FRIENDLY, Personality.TIMID));
		ReflectionTestUtils.setField(dog, "id", id);
		return dog;
	}

	// 수정 서비스가 돌려주는, 요청 본문대로 교체된 강아지
	private Dog updatedDog(Long id) {
		Dog dog = Dog.register(new DogRegisterCommand(42L, "보리", Breed.MALTESE, DogSex.FEMALE,
				LocalDate.of(2023, 1, 15), new BigDecimal("3.20"), NEW_IMAGE_URL, NEW_EXPECTATION));
		dog.select();
		dog.changePersonalities(Set.of(Personality.TIMID, Personality.LIVELY));
		ReflectionTestUtils.setField(dog, "id", id);
		return dog;
	}

	private Dog unselectedDog(Long id) {
		Dog dog = Dog.register(new DogRegisterCommand(42L, "보리", Breed.MALTESE, DogSex.FEMALE,
				null, new BigDecimal("3.20"), "", null));
		ReflectionTestUtils.setField(dog, "id", id);
		return dog;
	}
}
