package com.daesabu.meongcoach.community.application.provided;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.community.application.provided.dto.AuthorResult;
import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogRegisterCommand;
import com.daesabu.meongcoach.dog.domain.DogSex;
import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.dog.domain.shared.RegisteredBreed;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfileCreateCommand;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class AuthorFinderTest {

	private static final String DOG_IMAGE_URL = "https://media.meongcoach.com/images/dog-profile/42/bori.jpg";

	@Autowired
	private AuthorFinder authorFinder;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private DogRepository dogRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 회원의_선택된_강아지를_작성자_정보로_반환한다() {
		User user = saveUserWithSelectedDog("보리");
		flushAndClear();

		assertThat(authorFinder.findAuthor(user.getId()))
				.contains(new AuthorResult(user.getId(), "보리", DOG_IMAGE_URL, false));
	}

	@Test
	void 전문가_회원은_전문가로_표시해_반환한다() {
		User user = saveUserWithSelectedDog("멍코치 훈련사");
		entityManager.createNativeQuery("UPDATE users SET role = 'EXPERT' WHERE id = :id")
				.setParameter("id", user.getId())
				.executeUpdate();
		flushAndClear();

		assertThat(authorFinder.findAuthor(user.getId()))
				.contains(new AuthorResult(user.getId(), "멍코치 훈련사", DOG_IMAGE_URL, true));
	}

	@Test
	void 선택된_강아지가_없으면_빈_값을_반환한다() {
		User user = saveUser();
		flushAndClear();

		assertThat(authorFinder.findAuthor(user.getId())).isEmpty();
	}

	private User saveUserWithSelectedDog(String dogName) {
		User user = saveUser();
		Dog dog = Dog.register(new DogRegisterCommand(user.getId(), dogName, Breed.of(RegisteredBreed.POODLE),
				DogSex.MALE, LocalDate.of(2024, 3, 1), new BigDecimal("4.50"), DOG_IMAGE_URL, ""));
		dog.select();
		dogRepository.save(dog);
		return user;
	}

	private User saveUser() {
		User user = User.registerUser();
		user.completeOnboarding(new UserProfileCreateCommand("멍멍이집사", "", "INFP", Set.of(), Set.of()));
		return userRepository.save(user);
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
