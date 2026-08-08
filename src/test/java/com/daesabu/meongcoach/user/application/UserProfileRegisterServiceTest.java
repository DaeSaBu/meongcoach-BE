package com.daesabu.meongcoach.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.user.application.required.UserProfileRepository;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.Gender;
import com.daesabu.meongcoach.user.domain.Mbti;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfile;
import com.daesabu.meongcoach.user.domain.command.UserProfileCreateCommand;
import com.daesabu.meongcoach.user.domain.exception.AlreadyOnboardedException;
import com.daesabu.meongcoach.user.domain.exception.InvalidGenderException;
import com.daesabu.meongcoach.user.domain.exception.InvalidMbtiException;
import com.daesabu.meongcoach.user.domain.exception.UserNotFoundException;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
@DisplayName("사용자 프로필 등록 서비스")
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
		userId = userRepository.save(User.registerMember()).getId();
	}

	@Test
	@DisplayName("필수 정보로 프로필을 생성한다")
	void registerCreatesProfileWithRequiredFields() {
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
	@DisplayName("생년월일을 필수 프로필 정보와 함께 저장한다")
	void registerCreatesProfileWithBirthDate() {
		service.register(userId,
				command("멍멍이집사", LocalDate.of(1998, 1, 1), VALID_MBTI, VALID_GENDER, null));

		UserProfile profile = userProfileRepository.findById(userId).orElseThrow();
		assertThat(profile.getBirthDate()).isEqualTo(LocalDate.of(1998, 1, 1));
		assertThat(profile.getMbti()).isEqualTo(Mbti.INTJ);
		assertThat(profile.getGender()).isEqualTo(Gender.FEMALE);
	}

	@Test
	@DisplayName("프로필 이미지 URL을 함께 저장한다")
	void registerCreatesProfileWithProfileImage() {
		String imageUrl = "https://images.test.meongcoach.com/images/user-profile/1/a.jpg";

		service.register(userId,
				command("멍멍이집사", null, VALID_MBTI, VALID_GENDER, imageUrl));

		UserProfile profile = userProfileRepository.findById(userId).orElseThrow();
		assertThat(profile.getProfileImageUrl()).isEqualTo(imageUrl);
	}

	@Test
	@DisplayName("프로필 이미지가 없으면 빈 문자열로 저장한다")
	void registerStoresEmptyProfileImageWhenAbsent() {
		service.register(userId,
				command("멍멍이집사", null, VALID_MBTI, VALID_GENDER, null));

		UserProfile profile = userProfileRepository.findById(userId).orElseThrow();
		assertThat(profile.getProfileImageUrl()).isEmpty();
	}

	@Test
	@DisplayName("교육 이력·목표를 함께 저장한다")
	void registerStoresTrainingTopics() {
		UserProfileCreateCommand command = new UserProfileCreateCommand(
				"멍멍이집사", null, null, VALID_MBTI, VALID_GENDER, Set.of(1L, 2L), Set.of(2L, 3L));

		service.register(userId, command);

		UserProfile profile = findPersistedProfile();
		assertThat(profile.getPriorTrainingTopicIds()).containsExactlyInAnyOrder(1L, 2L);
		assertThat(profile.getTrainingGoalTopicIds()).containsExactlyInAnyOrder(2L, 3L);
	}

	@Test
	@DisplayName("이미 프로필이 있으면 등록에 실패한다")
	void registerFailsWhenProfileAlreadyExists() {
		service.register(userId,
				command("멍멍이집사", null, VALID_MBTI, VALID_GENDER, null));

		assertThatThrownBy(() -> service.register(userId,
				command("다른닉네임", null, VALID_MBTI, VALID_GENDER, null)))
				.isInstanceOf(AlreadyOnboardedException.class);
	}

	@Test
	@DisplayName("회원이 없으면 등록에 실패한다")
	void registerFailsWhenUserDoesNotExist() {
		assertThatThrownBy(() -> service.register(999L,
				command("멍멍이집사", null, VALID_MBTI, VALID_GENDER, null)))
				.isInstanceOf(UserNotFoundException.class);
	}

	@Test
	@DisplayName("잘못된 MBTI 값이면 등록에 실패한다")
	void registerFailsWhenMbtiIsInvalid() {
		assertThatThrownBy(() -> service.register(userId,
				command("멍멍이집사", null, "XXXX", VALID_GENDER, null)))
				.isInstanceOf(InvalidMbtiException.class);
	}

	@Test
	@DisplayName("MBTI가 null이면 등록에 실패한다")
	void registerFailsWhenMbtiIsNull() {
		assertThatThrownBy(() -> service.register(userId,
				command("멍멍이집사", null, null, VALID_GENDER, null)))
				.isInstanceOf(InvalidMbtiException.class);
	}

	@Test
	@DisplayName("MBTI가 공백이면 등록에 실패한다")
	void registerFailsWhenMbtiIsBlank() {
		assertThatThrownBy(() -> service.register(userId,
				command("멍멍이집사", null, "   ", VALID_GENDER, null)))
				.isInstanceOf(InvalidMbtiException.class);
	}

	@Test
	@DisplayName("잘못된 성별 값이면 등록에 실패한다")
	void registerFailsWhenGenderIsInvalid() {
		assertThatThrownBy(() -> service.register(userId,
				command("멍멍이집사", null, VALID_MBTI, "OTHER", null)))
				.isInstanceOf(InvalidGenderException.class);
	}

	@Test
	@DisplayName("성별이 null이면 등록에 실패한다")
	void registerFailsWhenGenderIsNull() {
		assertThatThrownBy(() -> service.register(userId,
				command("멍멍이집사", null, VALID_MBTI, null, null)))
				.isInstanceOf(InvalidGenderException.class);
	}

	@Test
	@DisplayName("성별이 공백이면 등록에 실패한다")
	void registerFailsWhenGenderIsBlank() {
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
