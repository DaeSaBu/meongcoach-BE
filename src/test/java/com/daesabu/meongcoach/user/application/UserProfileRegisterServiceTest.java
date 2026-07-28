package com.daesabu.meongcoach.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.user.application.provided.UserProfileCreateInfo;
import com.daesabu.meongcoach.user.application.required.UserProfileRepository;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.Mbti;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfile;
import com.daesabu.meongcoach.user.domain.exception.AlreadyOnboardedException;
import com.daesabu.meongcoach.user.domain.exception.InvalidMbtiException;
import com.daesabu.meongcoach.user.domain.exception.UserNotFoundException;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
@DisplayName("사용자 프로필 등록 서비스")
class UserProfileRegisterServiceTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserProfileRepository userProfileRepository;

	private UserProfileRegisterService service;

	private Long userId;

	@BeforeEach
	void setUp() {
		service = new UserProfileRegisterService(userRepository, userProfileRepository);
		userId = userRepository.save(User.registerMember()).getId();
	}

	@Test
	@DisplayName("닉네임만으로 프로필을 생성한다")
	void registerCreatesProfileWithNicknameOnly() {
		service.register(userId, new UserProfileCreateInfo("멍멍이집사", null, null));

		UserProfile profile = userProfileRepository.findById(userId).orElseThrow();
		assertThat(profile.getNickname()).isEqualTo("멍멍이집사");
		assertThat(profile.getBirthDate()).isNull();
		assertThat(profile.getMbti()).isNull();
	}

	@Test
	@DisplayName("생년월일과 MBTI를 함께 저장한다")
	void registerCreatesProfileWithOptionalFields() {
		service.register(userId, new UserProfileCreateInfo("멍멍이집사", LocalDate.of(1998, 1, 1), "INTJ"));

		UserProfile profile = userProfileRepository.findById(userId).orElseThrow();
		assertThat(profile.getBirthDate()).isEqualTo(LocalDate.of(1998, 1, 1));
		assertThat(profile.getMbti()).isEqualTo(Mbti.INTJ);
	}

	@Test
	@DisplayName("이미 프로필이 있으면 등록에 실패한다")
	void registerFailsWhenProfileAlreadyExists() {
		service.register(userId, new UserProfileCreateInfo("멍멍이집사", null, null));

		assertThatThrownBy(() -> service.register(userId, new UserProfileCreateInfo("다른닉네임", null, null)))
				.isInstanceOf(AlreadyOnboardedException.class);
	}

	@Test
	@DisplayName("회원이 없으면 등록에 실패한다")
	void registerFailsWhenUserDoesNotExist() {
		assertThatThrownBy(() -> service.register(999L, new UserProfileCreateInfo("멍멍이집사", null, null)))
				.isInstanceOf(UserNotFoundException.class);
	}

	@Test
	@DisplayName("잘못된 MBTI 값이면 등록에 실패한다")
	void registerFailsWhenMbtiIsInvalid() {
		assertThatThrownBy(() -> service.register(userId, new UserProfileCreateInfo("멍멍이집사", null, "XXXX")))
				.isInstanceOf(InvalidMbtiException.class);
	}
}
