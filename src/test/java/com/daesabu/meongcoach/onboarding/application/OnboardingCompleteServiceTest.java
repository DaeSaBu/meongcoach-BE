package com.daesabu.meongcoach.onboarding.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.dog.application.DogRegisterService;
import com.daesabu.meongcoach.dog.application.provided.DogRegisterInfo;
import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.Breed;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogStatus;
import com.daesabu.meongcoach.dog.domain.Personality;
import com.daesabu.meongcoach.media.application.provided.StoredImageUrlValidator;
import com.daesabu.meongcoach.media.domain.exception.InvalidImageUrlException;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingCompleteInfo;
import com.daesabu.meongcoach.user.application.UserProfileRegisterService;
import com.daesabu.meongcoach.user.application.required.UserProfileRepository;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfile;
import com.daesabu.meongcoach.user.domain.UserRole;
import com.daesabu.meongcoach.user.domain.exception.AlreadyOnboardedException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class OnboardingCompleteServiceTest {

	private static final String STORAGE_BASE_URL = "https://images.test.meongcoach.com/";
	private static final long PRIOR_TRAINING_TOPIC_ID = 1L;
	private static final long TRAINING_GOAL_TOPIC_ID = 2L;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserProfileRepository userProfileRepository;

	@Autowired
	private DogRepository dogRepository;

	private OnboardingCompleteService service;

	private Long userId;

	@BeforeEach
	void setUp() {
		service = new OnboardingCompleteService(
				new UserProfileRegisterService(userRepository, userProfileRepository),
				new DogRegisterService(dogRepository),
				prefixValidator());
		userId = userRepository.save(User.registerOnboardingMember()).getId();
	}

	// 미설정(null·빈 문자열)은 통과하고
	// 우리 스토리지 밖 URL은 거부하는 media 모듈 검증기의 스텁
	private StoredImageUrlValidator prefixValidator() {
		return url -> {
			if (url != null && !url.isBlank() && !url.startsWith(STORAGE_BASE_URL)) {
				throw new InvalidImageUrlException();
			}
		};
	}

	private OnboardingCompleteInfo completeInfo() {
		return completeInfo(null, null);
	}

	private OnboardingCompleteInfo completeInfo(String userImageUrl, String dogImageUrl) {
		return new OnboardingCompleteInfo(
				"멍멍이집사",
				LocalDate.of(1998, 1, 1),
				"INTJ",
				"FEMALE",
				userImageUrl,
				Set.of(PRIOR_TRAINING_TOPIC_ID),
				Set.of(TRAINING_GOAL_TOPIC_ID),
				List.of(
						new DogRegisterInfo("초코", "POODLE", "MALE", LocalDate.of(2024, 3, 1),
								new BigDecimal("4.50"), Set.of("TIMID"), dogImageUrl,
								"산책할 때 보호자에게 집중하면 좋겠어요."),
						new DogRegisterInfo("보리", "MALTESE", "FEMALE", null,
								new BigDecimal("3.20"), Set.of(), null, null)));
	}

	@Test
	void 온보딩을_완료하면_프로필과_강아지들이_함께_생성된다() {
		List<Long> dogIds = service.complete(userId, completeInfo());

		UserProfile profile = userProfileRepository.findById(userId).orElseThrow();
		assertThat(profile.getNickname()).isEqualTo("멍멍이집사");
		assertThat(dogIds).hasSize(2).doesNotContainNull();
		List<Dog> dogs = dogRepository.findAllById(dogIds);
		assertThat(dogs).extracting(Dog::getUserId).containsOnly(userId);
		assertThat(dogs).extracting(Dog::getName).containsExactlyInAnyOrder("초코", "보리");
		assertThat(dogs).extracting(Dog::getBreed).containsExactlyInAnyOrder(Breed.POODLE, Breed.MALTESE);
	}

	@Test
	void 여러_마리를_등록하면_첫_번째_강아지만_선택_상태가_된다() {
		List<Long> dogIds = service.complete(userId, completeInfo());

		List<Dog> dogs = dogRepository.findAllById(dogIds);

		assertThat(dogs).filteredOn(dog -> dog.getStatus() == DogStatus.SELECTED)
				.extracting(Dog::getName)
				.containsExactly("초코");
	}

	@Test
	void 등록한_강아지에_성격이_저장된다() {
		List<Long> dogIds = service.complete(userId, completeInfo());

		List<Dog> dogs = dogRepository.findAllById(dogIds);
		assertThat(dogs).filteredOn(dog -> dog.getName().equals("초코"))
				.singleElement()
				.satisfies(dog -> assertThat(dog.getPersonalities()).containsExactly(Personality.TIMID));
	}

	@Test
	void 등록한_강아지에_기대_사항이_저장된다() {
		List<Long> dogIds = service.complete(userId, completeInfo());

		assertThat(dogRepository.findAllById(dogIds))
				.filteredOn(dog -> dog.getName().equals("초코"))
				.singleElement()
				.satisfies(dog -> assertThat(dog.getExpectation())
						.isEqualTo("산책할 때 보호자에게 집중하면 좋겠어요."));
	}

	@Test
	void 교육_이력과_목표가_저장된다() {
		service.complete(userId, completeInfo());

		UserProfile profile = userProfileRepository.findById(userId).orElseThrow();
		assertThat(profile.getPriorTrainingTopicIds()).containsExactly(PRIOR_TRAINING_TOPIC_ID);
		assertThat(profile.getTrainingGoalTopicIds()).containsExactly(TRAINING_GOAL_TOPIC_ID);
	}

	@Test
	void 사용자와_강아지_프로필_이미지_URL이_함께_저장된다() {
		String userImageUrl = STORAGE_BASE_URL + "images/user-profile/1/a.jpg";
		String dogImageUrl = STORAGE_BASE_URL + "images/dog-profile/1/b.jpg";

		List<Long> dogIds = service.complete(userId, completeInfo(userImageUrl, dogImageUrl));

		UserProfile profile = userProfileRepository.findById(userId).orElseThrow();
		assertThat(profile.getProfileImageUrl()).isEqualTo(userImageUrl);
		assertThat(dogRepository.findAllById(dogIds))
				.filteredOn(dog -> dog.getName().equals("초코"))
				.singleElement()
				.satisfies(dog -> assertThat(dog.getProfileImageUrl()).isEqualTo(dogImageUrl));
	}

	@Test
	void 사용자_이미지가_우리_스토리지_URL이_아니면_실패한다() {
		assertThatThrownBy(() -> service.complete(userId,
				completeInfo("https://evil.example.com/a.jpg", null)))
				.isInstanceOf(InvalidImageUrlException.class);
	}

	@Test
	void 강아지_이미지가_우리_스토리지_URL이_아니면_실패한다() {
		assertThatThrownBy(() -> service.complete(userId,
				completeInfo(null, "https://evil.example.com/b.jpg")))
				.isInstanceOf(InvalidImageUrlException.class);
	}

	@Test
	void 온보딩을_완료하면_온보딩_회원이_정회원으로_승격된다() {
		service.complete(userId, completeInfo());

		User user = userRepository.findById(userId).orElseThrow();
		assertThat(user.getRole()).isEqualTo(UserRole.MEMBER);
	}

	@Test
	void 이미_온보딩을_완료한_회원이면_실패한다() {
		service.complete(userId, completeInfo());

		assertThatThrownBy(() -> service.complete(userId, completeInfo()))
				.isInstanceOf(AlreadyOnboardedException.class);
	}
}
