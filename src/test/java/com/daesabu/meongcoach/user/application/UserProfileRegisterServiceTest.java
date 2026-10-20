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
import com.daesabu.meongcoach.user.domain.exception.AlreadyOnboardedException;
import com.daesabu.meongcoach.user.domain.exception.InvalidMbtiException;
import com.daesabu.meongcoach.user.domain.exception.UserNotFoundException;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class UserProfileRegisterServiceTest {

	private static final String VALID_MBTI = "INTJ";

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TestEntityManager entityManager;

	private UserProfileRegisterService service;

	private Long userId;

	@BeforeEach
	void setUp() {
		service = new UserProfileRegisterService(new UserQueryService(userRepository));
		userId = userRepository.save(User.registerUser()).getId();
	}

	@Test
	void 필수_정보로_프로필을_생성한다() {
		service.register(userId,
				command("멍멍이집사", VALID_MBTI, null));

		UserProfile profile = findPersistedProfile();
		assertThat(profile.getNickname()).isEqualTo("멍멍이집사");
		assertThat(profile.getMbti()).isEqualTo(Mbti.INTJ);
		assertThat(profile.getPriorTrainingTopicIds()).isEmpty();
		assertThat(profile.getTrainingGoalTopicIds()).isEmpty();
	}

	@Test
	void 프로필_이미지_URL을_함께_저장한다() {
		String imageUrl = "https://images.test.meongcoach.com/images/user-profile/1/a.jpg";

		service.register(userId,
				command("멍멍이집사", VALID_MBTI, imageUrl));

		UserProfile profile = findPersistedProfile();
		assertThat(profile.getProfileImageUrl()).isEqualTo(imageUrl);
	}

	@Test
	void 프로필_이미지가_없으면_빈_문자열로_저장한다() {
		service.register(userId,
				command("멍멍이집사", VALID_MBTI, null));

		UserProfile profile = findPersistedProfile();
		assertThat(profile.getProfileImageUrl()).isEmpty();
	}

	@Test
	void 교육_이력과_목표를_함께_저장한다() {
		UserProfileRegisterRequest command = new UserProfileRegisterRequest(
				"멍멍이집사", null, VALID_MBTI, Set.of(1L, 2L), Set.of(2L, 3L));

		service.register(userId, command);

		UserProfile profile = findPersistedProfile();
		assertThat(profile.getPriorTrainingTopicIds()).containsExactlyInAnyOrder(1L, 2L);
		assertThat(profile.getTrainingGoalTopicIds()).containsExactlyInAnyOrder(2L, 3L);
	}

	@Test
	void 프로필을_등록하면_온보딩_회원이_정회원으로_승격된다() {
		service.register(userId,
				command("멍멍이집사", VALID_MBTI, null));

		entityManager.flush();
		entityManager.clear();
		User user = userRepository.findById(userId).orElseThrow();
		assertThat(user.getRole()).isEqualTo(UserRole.USER);
	}

	@Test
	void 이미_정회원이면_등록에_실패한다() {
		User promoted = User.registerUser();
		promoted.completeOnboarding(new UserProfileCreateCommand("기존회원", null, VALID_MBTI, Set.of(), Set.of()));
		Long promotedId = userRepository.save(promoted).getId();

		assertThatThrownBy(() -> service.register(promotedId,
				command("멍멍이집사", VALID_MBTI, null)))
				.isInstanceOf(AlreadyOnboardedException.class);
	}

	@Test
	void 회원이_없으면_등록에_실패한다() {
		assertThatThrownBy(() -> service.register(999L,
				command("멍멍이집사", VALID_MBTI, null)))
				.isInstanceOf(UserNotFoundException.class);
	}

	@Test
	void 잘못된_MBTI_값이면_등록에_실패한다() {
		assertThatThrownBy(() -> service.register(userId,
				command("멍멍이집사", "XXXX", null)))
				.isInstanceOf(InvalidMbtiException.class);
	}

	@Test
	void MBTI가_null이면_등록에_실패한다() {
		assertThatThrownBy(() -> service.register(userId,
				command("멍멍이집사", null, null)))
				.isInstanceOf(InvalidMbtiException.class);
	}

	@Test
	void MBTI가_공백이면_등록에_실패한다() {
		assertThatThrownBy(() -> service.register(userId,
				command("멍멍이집사", "   ", null)))
				.isInstanceOf(InvalidMbtiException.class);
	}

	private static UserProfileRegisterRequest command(String nickname, String mbti, String profileImageUrl) {
		return new UserProfileRegisterRequest(nickname, profileImageUrl, mbti, Set.of(), Set.of());
	}

	private UserProfile findPersistedProfile() {
		entityManager.flush();
		entityManager.clear();
		return userRepository.findById(userId).orElseThrow().getProfile();
	}
}
