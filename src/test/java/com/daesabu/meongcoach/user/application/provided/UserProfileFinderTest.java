package com.daesabu.meongcoach.user.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.user.application.required.UserProfileRepository;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.Mbti;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfile;
import com.daesabu.meongcoach.user.domain.UserProfileCreateCommand;
import com.daesabu.meongcoach.user.domain.exception.UserProfileNotFoundException;
import jakarta.persistence.EntityManager;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class UserProfileFinderTest {

	@Autowired
	private UserProfileFinder userProfileFinder;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserProfileRepository userProfileRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 사용자_ID로_프로필을_조회한다() {
		Long userId = saveProfile("INFP", Set.of());

		UserProfile profile = userProfileFinder.findProfile(userId);

		assertThat(profile.getNickname()).isEqualTo("멍멍이집사");
		assertThat(profile.getMbti()).isEqualTo(Mbti.INFP);
	}

	@Test
	void MBTI를_입력하지_않은_프로필도_조회한다() {
		Long userId = saveProfile(null, Set.of());

		UserProfile profile = userProfileFinder.findProfile(userId);

		assertThat(profile.getMbti()).isNull();
		assertThat(profile.getGender()).isNull();
	}

	@Test
	void 프로필이_없으면_예외가_발생한다() {
		assertThatThrownBy(() -> userProfileFinder.findProfile(999L))
				.isInstanceOf(UserProfileNotFoundException.class);
	}

	@Test
	void 배운_토픽_ID_집합을_조회한다() {
		Long userId = saveProfile("INFP", Set.of(1L, 2L));

		Set<Long> topicIds = userProfileFinder.findPriorTrainingTopicIds(userId);

		assertThat(topicIds).containsExactlyInAnyOrder(1L, 2L);
	}

	@Test
	void 배운_토픽을_조회할_때_프로필이_없으면_예외가_발생한다() {
		assertThatThrownBy(() -> userProfileFinder.findPriorTrainingTopicIds(999L))
				.isInstanceOf(UserProfileNotFoundException.class);
	}

	private Long saveProfile(String mbti, Set<Long> priorTrainingTopicIds) {
		User user = userRepository.save(User.registerUser());
		userProfileRepository.save(UserProfile.create(user,
				new UserProfileCreateCommand("멍멍이집사", null, null, mbti, null, priorTrainingTopicIds, Set.of())));
		entityManager.flush();
		entityManager.clear();
		return user.getId();
	}
}
