package com.daesabu.meongcoach.user.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.user.application.required.UserProfileRepository;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfile;
import com.daesabu.meongcoach.user.domain.command.UserProfileCreateCommand;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
@DisplayName("사용자 프로필 조회 서비스")
class UserProfileFinderServiceTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserProfileRepository userProfileRepository;

	private UserProfileFinderService service;

	private Long userId;

	@BeforeEach
	void setUp() {
		service = new UserProfileFinderService(userProfileRepository);
		userId = userRepository.save(User.registerMember()).getId();
	}

	@Test
	@DisplayName("사용자의 프로필 이미지 URL을 반환한다")
	void findProfileImageUrlReturnsStoredUrl() {
		String imageUrl = "https://images.test.meongcoach.com/images/user-profile/1/a.jpg";
		saveProfile(imageUrl);

		assertThat(service.findProfileImageUrl(userId)).isEqualTo(imageUrl);
	}

	@Test
	@DisplayName("프로필이 없으면 빈 문자열을 반환한다")
	void findProfileImageUrlReturnsEmptyWhenProfileDoesNotExist() {
		assertThat(service.findProfileImageUrl(userId)).isEmpty();
	}

	@Test
	@DisplayName("프로필 이미지가 미설정이면 빈 문자열을 반환한다")
	void findProfileImageUrlReturnsEmptyWhenImageIsNotSet() {
		saveProfile(null);

		assertThat(service.findProfileImageUrl(userId)).isEmpty();
	}

	private void saveProfile(String profileImageUrl) {
		User user = userRepository.findById(userId).orElseThrow();
		UserProfileCreateCommand command = new UserProfileCreateCommand(
				"멍멍이집사", profileImageUrl, null, "INTJ", "FEMALE", Set.of(), Set.of());
		userProfileRepository.save(UserProfile.create(user, command));
	}
}
