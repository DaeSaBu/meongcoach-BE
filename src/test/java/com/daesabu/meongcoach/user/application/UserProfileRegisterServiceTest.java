package com.daesabu.meongcoach.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.user.application.required.UserProfileRepository;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.Gender;
import com.daesabu.meongcoach.user.domain.Mbti;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfile;
import com.daesabu.meongcoach.user.domain.UserRole;
import com.daesabu.meongcoach.user.domain.command.UserProfileCreateCommand;
import com.daesabu.meongcoach.user.domain.exception.AlreadyOnboardedException;
import com.daesabu.meongcoach.user.domain.exception.InvalidGenderException;
import com.daesabu.meongcoach.user.domain.exception.InvalidMbtiException;
import com.daesabu.meongcoach.user.domain.exception.UserNotFoundException;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class UserProfileRegisterServiceTest {

	private static final String VALID_MBTI = "INTJ";
	private static final String VALID_GENDER = "FEMALE";

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserProfileRepository userProfileRepository;

	@Autowired
	private TestEntityManager entityManager;

	private UserProfileRegisterService service;

	private Long userId;

	@BeforeEach
	void setUp() {
		service = new UserProfileRegisterService(userRepository, userProfileRepository);
		userId = userRepository.save(User.registerOnboardingMember()).getId();
	}

	@Test
	void 필수_정보로_프로필을_생성한다() {
		service.register(userId,
				command("멍멍이집사", null, VALID_MBTI, VALID_GENDER, null));

		UserProfile profile = findPersistedProfile();
		assertThat(profile.getNickname()).isEqualTo("멍멍이집사");
		assertThat(profile.getBirthDate()).isNull();
		assertThat(profile.getMbti()).isEqualTo(Mbti.INTJ);
		assertThat(profile.getGender()).isEqualTo(Gender.FEMALE);
		assertThat(profile.getPriorTrainingTopicIds()).isEmpty();
		assertThat(profile.getTrainingGoalTopicIds()).isEmpty();
	}

	@Test
	void 생년월일을_필수_프로필_정보와_함께_저장한다() {
		service.register(userId,
				command("멍멍이집사", LocalDate.of(1998, 1, 1), VALID_MBTI, VALID_GENDER, null));

		UserProfile profile = userProfileRepository.findById(userId).orElseThrow();
		assertThat(profile.getBirthDate()).isEqualTo(LocalDate.of(1998, 1, 1));
		assertThat(profile.getMbti()).isEqualTo(Mbti.INTJ);
		assertThat(profile.getGender()).isEqualTo(Gender.FEMALE);
	}

	@Test
	void 프로필_이미지_URL을_함께_저장한다() {
		String imageUrl = "https://images.test.meongcoach.com/images/user-profile/1/a.jpg";

		service.register(userId,
				command("멍멍이집사", null, VALID_MBTI, VALID_GENDER, imageUrl));

		UserProfile profile = userProfileRepository.findById(userId).orElseThrow();
		assertThat(profile.getProfileImageUrl()).isEqualTo(imageUrl);
	}

	@Test
	void 프로필_이미지가_없으면_빈_문자열로_저장한다() {
		service.register(userId,
				command("멍멍이집사", null, VALID_MBTI, VALID_GENDER, null));

		UserProfile profile = userProfileRepository.findById(userId).orElseThrow();
		assertThat(profile.getProfileImageUrl()).isEmpty();
	}

	@Test
	void 교육_이력_목표를_함께_저장한다() {
		UserProfileCreateCommand command = new UserProfileCreateCommand(
				"멍멍이집사", null, null, VALID_MBTI, VALID_GENDER, Set.of(1L, 2L), Set.of(2L, 3L));

		service.register(userId, command);

		UserProfile profile = findPersistedProfile();
		assertThat(profile.getPriorTrainingTopicIds()).containsExactlyInAnyOrder(1L, 2L);
		assertThat(profile.getTrainingGoalTopicIds()).containsExactlyInAnyOrder(2L, 3L);
	}

	@Test
	void 프로필을_등록하면_온보딩_회원이_정회원으로_승격된다() {
		service.register(userId,
				command("멍멍이집사", null, VALID_MBTI, VALID_GENDER, null));

		entityManager.flush();
		entityManager.clear();
		User user = userRepository.findById(userId).orElseThrow();
		assertThat(user.getRole()).isEqualTo(UserRole.MEMBER);
	}

	@Test
	void 이미_프로필이_있으면_등록에_실패한다() {
		service.register(userId,
				command("멍멍이집사", null, VALID_MBTI, VALID_GENDER, null));

		assertThatThrownBy(() -> service.register(userId,
				command("다른닉네임", null, VALID_MBTI, VALID_GENDER, null)))
				.isInstanceOf(AlreadyOnboardedException.class);
	}

	@Test
	void 회원이_없으면_등록에_실패한다() {
		assertThatThrownBy(() -> service.register(999L,
				command("멍멍이집사", null, VALID_MBTI, VALID_GENDER, null)))
				.isInstanceOf(UserNotFoundException.class);
	}

	@Test
	void 잘못된_MBTI_값이면_등록에_실패한다() {
		assertThatThrownBy(() -> service.register(userId,
				command("멍멍이집사", null, "XXXX", VALID_GENDER, null)))
				.isInstanceOf(InvalidMbtiException.class);
	}

	@Test
	void MBTI가_null이면_등록에_실패한다() {
		assertThatThrownBy(() -> service.register(userId,
				command("멍멍이집사", null, null, VALID_GENDER, null)))
				.isInstanceOf(InvalidMbtiException.class);
	}

	@Test
	void MBTI가_공백이면_등록에_실패한다() {
		assertThatThrownBy(() -> service.register(userId,
				command("멍멍이집사", null, "   ", VALID_GENDER, null)))
				.isInstanceOf(InvalidMbtiException.class);
	}

	@Test
	void 잘못된_성별_값이면_등록에_실패한다() {
		assertThatThrownBy(() -> service.register(userId,
				command("멍멍이집사", null, VALID_MBTI, "OTHER", null)))
				.isInstanceOf(InvalidGenderException.class);
	}

	@Test
	void 성별이_null이면_등록에_실패한다() {
		assertThatThrownBy(() -> service.register(userId,
				command("멍멍이집사", null, VALID_MBTI, null, null)))
				.isInstanceOf(InvalidGenderException.class);
	}

	@Test
	void 성별이_공백이면_등록에_실패한다() {
		assertThatThrownBy(() -> service.register(userId,
				command("멍멍이집사", null, VALID_MBTI, "   ", null)))
				.isInstanceOf(InvalidGenderException.class);
	}

	private static UserProfileCreateCommand command(String nickname, LocalDate birthDate, String mbti, String gender,
			String profileImageUrl) {
		return new UserProfileCreateCommand(nickname, profileImageUrl, birthDate, mbti, gender, Set.of(), Set.of());
	}

	private UserProfile findPersistedProfile() {
		entityManager.flush();
		entityManager.clear();
		return userProfileRepository.findById(userId).orElseThrow();
	}
}
