package com.daesabu.meongcoach.onboarding.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogStatus;
import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.dog.domain.shared.Personality;
import com.daesabu.meongcoach.dog.domain.shared.RegisteredBreed;
import com.daesabu.meongcoach.media.domain.exception.InvalidMediaUrlException;
import com.daesabu.meongcoach.onboarding.application.provided.dto.OnboardingCompleteRequest;
import com.daesabu.meongcoach.onboarding.application.provided.dto.OnboardingCompleteRequest.DogRequest;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfile;
import com.daesabu.meongcoach.user.domain.UserRole;
import com.daesabu.meongcoach.user.domain.exception.AlreadyOnboardedException;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class OnboardingCompleterTest {

	private static final String STORAGE_BASE_URL = "https://images.test.meongcoach.com/";

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private DogRepository dogRepository;

	@Autowired
	private OnboardingCompleter onboardingCompleter;

	@Autowired
	private EntityManager entityManager;

	private Long userId;

	@BeforeEach
	void setUp() {
		userId = userRepository.save(User.registerUser()).getId();
	}

	private OnboardingCompleteRequest completeInfo() {
		return completeInfo(null);
	}

	private OnboardingCompleteRequest completeInfo(String dogImageUrl) {
		return new OnboardingCompleteRequest(null, null, null, null, null,
				List.of(
						new DogRequest("초코", "POODLE", "MALE", LocalDate.of(2024, 3, 1),
								Set.of("TIMID"), dogImageUrl, null, null)));
	}

	@Test
	void 온보딩을_완료하면_기본_프로필과_강아지_한_마리가_함께_생성된다() {
		List<Long> dogIds = onboardingCompleter.complete(userId, completeInfo());

		UserProfile profile = userRepository.findById(userId).orElseThrow().getProfile();
		assertThat(profile.getNickname()).isEmpty();
		assertThat(dogIds).hasSize(1).doesNotContainNull();

		List<Dog> dogs = dogRepository.findAllById(dogIds);
		assertThat(dogs).extracting(Dog::getUserId).containsExactly(userId);
		assertThat(dogs).extracting(Dog::getName).containsExactly("초코");
		assertThat(dogs).extracting(Dog::getBreed)
				.containsExactly(Breed.of(RegisteredBreed.POODLE));
	}

	@Test
	void 온보딩에서_등록한_강아지는_선택_상태가_된다() {
		List<Long> dogIds = onboardingCompleter.complete(userId, completeInfo());

		assertThat(dogRepository.findAllById(dogIds))
				.singleElement()
				.satisfies(dog -> assertThat(dog.getStatus()).isEqualTo(DogStatus.SELECTED));
	}

	@Test
	void 등록한_강아지에_성격이_저장된다() {
		List<Long> dogIds = onboardingCompleter.complete(userId, completeInfo());

		List<Dog> dogs = dogRepository.findAllById(dogIds);
		assertThat(dogs).filteredOn(dog -> dog.getName().equals("초코"))
				.singleElement()
				.satisfies(dog -> assertThat(dog.getPersonalities()).containsExactly(Personality.TIMID));
	}

	@Test
	void 온보딩에서_받지_않는_강아지_정보는_비워_저장한다() {
		List<Long> dogIds = onboardingCompleter.complete(userId, completeInfo());

		assertThat(dogRepository.findAllById(dogIds))
				.singleElement()
				.satisfies(dog -> {
					assertThat(dog.getWeightKg()).isNull();
					assertThat(dog.getExpectation()).isEmpty();
				});
	}

	@Test
	void 온보딩에서_받지_않는_보호자_정보는_비워_저장한다() {
		onboardingCompleter.complete(userId, completeInfo());

		UserProfile profile = userRepository.findById(userId).orElseThrow().getProfile();

		assertThat(profile.getNickname()).isEmpty();
		assertThat(profile.getMbti()).isNull();
		assertThat(profile.getProfileImageUrl()).isEmpty();
		assertThat(profile.getPriorTrainingTopicIds()).isEmpty();
		assertThat(profile.getTrainingGoalTopicIds()).isEmpty();
	}

	@Test
	void 강아지_프로필_이미지_URL이_저장된다() {
		String dogImageUrl = STORAGE_BASE_URL + "images/dog-profile/" + userId + "/b.jpg";

		List<Long> dogIds = onboardingCompleter.complete(userId, completeInfo(dogImageUrl));

		assertThat(dogRepository.findAllById(dogIds))
				.filteredOn(dog -> dog.getName().equals("초코"))
				.singleElement()
				.satisfies(dog -> assertThat(dog.getProfileImageUrl()).isEqualTo(dogImageUrl));
	}

	@Test
	void 강아지_이미지가_우리_스토리지_URL이_아니면_실패한다() {
		assertThatThrownBy(() -> onboardingCompleter.complete(userId,
				completeInfo("https://evil.example.com/b.jpg")))
				.isInstanceOf(InvalidMediaUrlException.class);
	}

	@Test
	void 온보딩을_완료하면_온보딩_회원이_정회원으로_승격된다() {
		onboardingCompleter.complete(userId, completeInfo());

		User user = userRepository.findById(userId).orElseThrow();
		assertThat(user.getRole()).isEqualTo(UserRole.USER);
	}

	@Test
	void 구버전_보호자_정보와_강아지_다섯_마리를_새_스키마에_보존한다() {
		String profileImage = STORAGE_BASE_URL + "images/user-profile/" + userId + "/parent.jpg";
		DogRequest dog = new DogRequest("가".repeat(50), "POODLE", "MALE", LocalDate.of(2024, 3, 1),
				Set.of("TIMID"), null, new BigDecimal("4.50"), "즐거운 산책");
		OnboardingCompleteRequest request = new OnboardingCompleteRequest("나".repeat(50), "INTJ",
				profileImage, List.of(1L, 2L), List.of(3L), List.of(dog, dog, dog, dog, dog));

		List<Long> dogIds = onboardingCompleter.complete(userId, request);
		entityManager.flush();
		entityManager.clear();

		User user = userRepository.findById(userId).orElseThrow();
		UserProfile profile = user.getProfile();
		assertThat(user.getRole()).isEqualTo(UserRole.USER);
		assertThat(profile.getNickname()).isEqualTo("나".repeat(50));
		assertThat(profile.getMbti().name()).isEqualTo("INTJ");
		assertThat(profile.getProfileImageUrl()).isEqualTo(profileImage);
		assertThat(profile.getPriorTrainingTopicIds()).containsExactlyInAnyOrder(1L, 2L);
		assertThat(profile.getTrainingGoalTopicIds()).containsExactly(3L);
		List<Dog> dogs = dogRepository.findAllById(dogIds);
		assertThat(dogs).hasSize(5).allSatisfy(saved -> {
			assertThat(saved.getName()).isEqualTo("가".repeat(50));
			assertThat(saved.getWeightKg()).isEqualByComparingTo("4.50");
			assertThat(saved.getExpectation()).isEqualTo("즐거운 산책");
		});
		assertThat(dogs).filteredOn(saved -> saved.getStatus() == DogStatus.SELECTED).hasSize(1);
	}

	@Test
	void 구버전이_다른_사용자의_보호자_이미지를_보내면_거절한다() {
		OnboardingCompleteRequest request = new OnboardingCompleteRequest("보호자", "INTJ",
				STORAGE_BASE_URL + "images/user-profile/" + (userId + 1) + "/parent.jpg",
				null, null, completeInfo().dogs());

		assertThatThrownBy(() -> onboardingCompleter.complete(userId, request))
				.isInstanceOf(InvalidMediaUrlException.class);
	}

	@Test
	void 이미_온보딩을_완료한_회원이면_실패한다() {
		onboardingCompleter.complete(userId, completeInfo());

		assertThatThrownBy(() -> onboardingCompleter.complete(userId, completeInfo()))
				.isInstanceOf(AlreadyOnboardedException.class);
	}
}
