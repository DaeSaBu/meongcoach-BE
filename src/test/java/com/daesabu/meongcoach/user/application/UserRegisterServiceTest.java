package com.daesabu.meongcoach.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.user.application.provided.UserProfileRegisterRequest;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.Mbti;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfile;
import com.daesabu.meongcoach.user.domain.UserProfileCreateCommand;
import com.daesabu.meongcoach.user.domain.UserRole;
import com.daesabu.meongcoach.user.domain.UserStatus;
import com.daesabu.meongcoach.user.domain.exception.AlreadyOnboardedException;
import com.daesabu.meongcoach.user.domain.exception.InvalidMbtiException;
import com.daesabu.meongcoach.user.domain.exception.UserNotFoundException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({UserRegisterService.class, UserQueryService.class})
class UserRegisterServiceTest {

	private static final Long UNREGISTERED_USER_ID = 999L;
	private static final String VALID_MBTI = "INTJ";

	@Autowired
	private UserRegisterService userRegisterService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 등록하면_온보딩_전_활성_회원이_저장되고_ID를_반환한다() {
		Long userId = userRegisterService.register();

		User user = userRepository.findById(userId).orElseThrow();
		assertThat(user.isOnboarding()).isTrue();
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	void 온보딩을_완료하면_필수_정보로_프로필을_생성한다() {
		Long userId = saveOnboardingUser();

		userRegisterService.completeOnboarding(userId, request(VALID_MBTI, null));

		UserProfile profile = findPersistedProfile(userId);
		assertThat(profile.getNickname()).isEqualTo("멍멍이집사");
		assertThat(profile.getMbti()).isEqualTo(Mbti.INTJ);
		assertThat(profile.getPriorTrainingTopicIds()).isEmpty();
		assertThat(profile.getTrainingGoalTopicIds()).isEmpty();
	}

	@Test
	void 온보딩을_완료하면_프로필_이미지_URL을_함께_저장한다() {
		Long userId = saveOnboardingUser();
		String imageUrl = "https://images.test.meongcoach.com/images/user-profile/1/a.jpg";

		userRegisterService.completeOnboarding(userId, request(VALID_MBTI, imageUrl));

		assertThat(findPersistedProfile(userId).getProfileImageUrl()).isEqualTo(imageUrl);
	}

	@Test
	void 프로필_이미지가_없으면_빈_문자열로_저장한다() {
		Long userId = saveOnboardingUser();

		userRegisterService.completeOnboarding(userId, request(VALID_MBTI, null));

		assertThat(findPersistedProfile(userId).getProfileImageUrl()).isEmpty();
	}

	@Test
	void 온보딩을_완료하면_교육_이력과_목표를_함께_저장한다() {
		Long userId = saveOnboardingUser();
		UserProfileRegisterRequest request = new UserProfileRegisterRequest(
				"멍멍이집사", null, VALID_MBTI, Set.of(1L, 2L), Set.of(2L, 3L));

		userRegisterService.completeOnboarding(userId, request);

		UserProfile profile = findPersistedProfile(userId);
		assertThat(profile.getPriorTrainingTopicIds()).containsExactlyInAnyOrder(1L, 2L);
		assertThat(profile.getTrainingGoalTopicIds()).containsExactlyInAnyOrder(2L, 3L);
	}

	@Test
	void 온보딩을_완료하면_정회원으로_승격된다() {
		Long userId = saveOnboardingUser();

		userRegisterService.completeOnboarding(userId, request(VALID_MBTI, null));
		flushAndClear();

		assertThat(userRepository.findById(userId).orElseThrow().getRole()).isEqualTo(UserRole.USER);
	}

	@Test
	void 이미_정회원이면_온보딩_완료에_실패한다() {
		User promoted = User.registerUser();
		promoted.completeOnboarding(new UserProfileCreateCommand("기존회원", null, VALID_MBTI, Set.of(), Set.of()));
		Long promotedId = userRepository.save(promoted).getId();

		assertThatThrownBy(() -> userRegisterService.completeOnboarding(promotedId, request(VALID_MBTI, null)))
				.isInstanceOf(AlreadyOnboardedException.class);
	}

	@Test
	void 회원이_없으면_온보딩_완료에_실패한다() {
		assertThatThrownBy(() -> userRegisterService.completeOnboarding(UNREGISTERED_USER_ID,
				request(VALID_MBTI, null)))
				.isInstanceOf(UserNotFoundException.class);
	}

	@Test
	void 잘못된_MBTI_값이면_온보딩_완료에_실패한다() {
		Long userId = saveOnboardingUser();

		assertThatThrownBy(() -> userRegisterService.completeOnboarding(userId, request("XXXX", null)))
				.isInstanceOf(InvalidMbtiException.class);
	}

	@Test
	void MBTI가_null이면_온보딩_완료에_실패한다() {
		Long userId = saveOnboardingUser();

		assertThatThrownBy(() -> userRegisterService.completeOnboarding(userId, request(null, null)))
				.isInstanceOf(InvalidMbtiException.class);
	}

	@Test
	void MBTI가_공백이면_온보딩_완료에_실패한다() {
		Long userId = saveOnboardingUser();

		assertThatThrownBy(() -> userRegisterService.completeOnboarding(userId, request("   ", null)))
				.isInstanceOf(InvalidMbtiException.class);
	}

	@Test
	void 탈퇴하면_회원_상태가_WITHDRAWN이_되고_행은_남는다() {
		Long userId = userRepository.save(User.registerUser()).getId();

		userRegisterService.withdraw(userId);
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

		userRegisterService.withdraw(user.getId());
		flushAndClear();

		assertThat(entityManager.find(UserProfile.class, profileId)).isNull();
		assertThat(userRepository.findById(user.getId()).orElseThrow().getProfile()).isNull();
	}

	@Test
	void 온보딩_미완료_회원은_프로필이_없어도_탈퇴할_수_있다() {
		Long userId = userRepository.save(User.registerUser()).getId();

		userRegisterService.withdraw(userId);
		flushAndClear();

		assertThat(userRepository.findById(userId).orElseThrow().getStatus()).isEqualTo(UserStatus.WITHDRAWN);
	}

	@Test
	void 없는_회원_ID로_탈퇴하면_예외를_던진다() {
		assertThatThrownBy(() -> userRegisterService.withdraw(UNREGISTERED_USER_ID))
				.isInstanceOf(UserNotFoundException.class);
	}

	private Long saveOnboardingUser() {
		return userRepository.save(User.registerUser()).getId();
	}

	private static UserProfileRegisterRequest request(String mbti, String profileImageUrl) {
		return new UserProfileRegisterRequest("멍멍이집사", profileImageUrl, mbti, Set.of(), Set.of());
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
