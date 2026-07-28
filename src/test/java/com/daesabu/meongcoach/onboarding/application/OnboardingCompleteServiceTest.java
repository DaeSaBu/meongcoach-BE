package com.daesabu.meongcoach.onboarding.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.dog.application.DogRegisterService;
import com.daesabu.meongcoach.dog.application.provided.DogRegisterInfo;
import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.Personality;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingCompleteInfo;
import com.daesabu.meongcoach.user.application.UserProfileRegisterService;
import com.daesabu.meongcoach.user.application.required.UserProfileRepository;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfile;
import com.daesabu.meongcoach.user.domain.exception.AlreadyOnboardedException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
@DisplayName("온보딩 완료 서비스")
class OnboardingCompleteServiceTest {

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
				new DogRegisterService(dogRepository));
		userId = userRepository.save(User.registerMember()).getId();
	}

	private OnboardingCompleteInfo completeInfo() {
		return new OnboardingCompleteInfo("멍멍이집사", LocalDate.of(1998, 1, 1), "INTJ", "FEMALE", List.of(
				new DogRegisterInfo("초코", "푸들", "MALE", LocalDate.of(2024, 3, 1),
						new BigDecimal("4.50"), Set.of("TIMID")),
				new DogRegisterInfo("보리", "말티즈", "FEMALE", null,
						new BigDecimal("3.20"), Set.of())));
	}

	@Test
	@DisplayName("온보딩을 완료하면 프로필과 강아지들이 함께 생성된다")
	void completeCreatesProfileAndDogs() {
		List<Long> dogIds = service.complete(userId, completeInfo());

		UserProfile profile = userProfileRepository.findById(userId).orElseThrow();
		assertThat(profile.getNickname()).isEqualTo("멍멍이집사");
		assertThat(dogIds).hasSize(2).doesNotContainNull();
		List<Dog> dogs = dogRepository.findAllById(dogIds);
		assertThat(dogs).extracting(Dog::getUserId).containsOnly(userId);
		assertThat(dogs).extracting(Dog::getName).containsExactlyInAnyOrder("초코", "보리");
	}

	@Test
	@DisplayName("등록한 강아지에 성격이 저장된다")
	void completeSavesDogPersonalities() {
		List<Long> dogIds = service.complete(userId, completeInfo());

		List<Dog> dogs = dogRepository.findAllById(dogIds);
		assertThat(dogs).filteredOn(dog -> dog.getName().equals("초코"))
				.singleElement()
				.satisfies(dog -> assertThat(dog.getPersonalities()).containsExactly(Personality.TIMID));
	}

	@Test
	@DisplayName("이미 온보딩을 완료한 회원이면 실패한다")
	void completeFailsWhenAlreadyOnboarded() {
		service.complete(userId, completeInfo());

		assertThatThrownBy(() -> service.complete(userId, completeInfo()))
				.isInstanceOf(AlreadyOnboardedException.class);
	}
}
