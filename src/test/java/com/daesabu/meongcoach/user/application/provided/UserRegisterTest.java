package com.daesabu.meongcoach.user.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.user.application.provided.dto.UserOnboardingRequest;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfile;
import com.daesabu.meongcoach.user.domain.UserProfileCreateCommand;
import com.daesabu.meongcoach.user.domain.UserRole;
import com.daesabu.meongcoach.user.domain.UserStatus;
import com.daesabu.meongcoach.user.domain.exception.AlreadyOnboardedException;
import com.daesabu.meongcoach.user.domain.exception.UserNotFoundException;
import jakarta.persistence.EntityManager;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class UserRegisterTest {

	private static final Long UNREGISTERED_USER_ID = 999L;
	private static final String VALID_MBTI = "INTJ";

	@Autowired
	private UserRegister userRegister;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 등록하면_온보딩_전_활성_회원이_저장되고_ID를_반환한다() {
		Long userId = userRegister.register();

		User user = userRepository.findById(userId).orElseThrow();
		assertThat(user.isOnboarding()).isTrue();
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	void 온보딩을_완료하면_빈_닉네임으로_기본_프로필을_생성한다() {
		Long userId = saveOnboardingUser();

		userRegister.completeOnboarding(userId, new UserOnboardingRequest(null, null, null, null, null));

		assertThat(findPersistedProfile(userId).getNickname()).isEmpty();
	}

	@Test
	void 온보딩을_완료하면_받지_않는_정보는_비워_저장한다() {
		Long userId = saveOnboardingUser();

		userRegister.completeOnboarding(userId, new UserOnboardingRequest(null, null, null, null, null));

		UserProfile profile = findPersistedProfile(userId);
		assertThat(profile.getProfileImageUrl()).isEmpty();
		assertThat(profile.getMbti()).isNull();
		assertThat(profile.getPriorTrainingTopicIds()).isEmpty();
		assertThat(profile.getTrainingGoalTopicIds()).isEmpty();
	}

	@Test
	void 온보딩을_완료하면_정회원으로_승격된다() {
		Long userId = saveOnboardingUser();

		userRegister.completeOnboarding(userId, new UserOnboardingRequest(null, null, null, null, null));
		flushAndClear();

		assertThat(userRepository.findById(userId).orElseThrow().getRole()).isEqualTo(UserRole.USER);
	}

	@Test
	void 이미_정회원이면_온보딩_완료에_실패한다() {
		User promoted = User.registerUser();
		promoted.completeOnboarding(new UserProfileCreateCommand("기존회원", null, VALID_MBTI, Set.of(), Set.of()));
		Long promotedId = userRepository.save(promoted).getId();

		assertThatThrownBy(() -> userRegister.completeOnboarding(promotedId, new UserOnboardingRequest(null, null, null, null, null)))
				.isInstanceOf(AlreadyOnboardedException.class);
	}

	@Test
	void 회원이_없으면_온보딩_완료에_실패한다() {
		assertThatThrownBy(() -> userRegister.completeOnboarding(UNREGISTERED_USER_ID, new UserOnboardingRequest(null, null, null, null, null)))
				.isInstanceOf(UserNotFoundException.class);
	}

	@Test
	void 탈퇴하면_회원_상태가_WITHDRAWN이_되고_행은_남는다() {
		Long userId = userRepository.save(User.registerUser()).getId();

		userRegister.withdraw(userId);
		flushAndClear();

		User withdrawn = userRepository.findById(userId).orElseThrow();
		assertThat(withdrawn.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
	}

	@Test
	void 탈퇴하면_프로필_행이_삭제된다() {
		User user = User.registerUser();
		user.completeOnboarding(new UserProfileCreateCommand("멍멍이집사", null, "INTJ", Set.of(), Set.of()));
		userRepository.save(user);
		Long profileId = user.getProfile().getId();
		flushAndClear();

		userRegister.withdraw(user.getId());
		flushAndClear();

		assertThat(entityManager.find(UserProfile.class, profileId)).isNull();
		assertThat(userRepository.findById(user.getId()).orElseThrow().getProfile()).isNull();
	}

	@Test
	void 온보딩_미완료_회원은_프로필이_없어도_탈퇴할_수_있다() {
		Long userId = userRepository.save(User.registerUser()).getId();

		userRegister.withdraw(userId);
		flushAndClear();

		assertThat(userRepository.findById(userId).orElseThrow().getStatus()).isEqualTo(UserStatus.WITHDRAWN);
	}

	@Test
	void 없는_회원_ID로_탈퇴하면_예외를_던진다() {
		assertThatThrownBy(() -> userRegister.withdraw(UNREGISTERED_USER_ID))
				.isInstanceOf(UserNotFoundException.class);
	}

	private Long saveOnboardingUser() {
		return userRepository.save(User.registerUser()).getId();
	}

	private UserProfile findPersistedProfile(Long userId) {
		flushAndClear();
		return userRepository.findById(userId).orElseThrow().getProfile();
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
