package com.daesabu.meongcoach.user.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.domain.exception.InvalidImageUrlException;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.user.application.required.UserProfileRepository;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.Mbti;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfile;
import com.daesabu.meongcoach.user.domain.UserProfileCreateCommand;
import com.daesabu.meongcoach.user.domain.exception.InvalidMbtiException;
import com.daesabu.meongcoach.user.domain.exception.UserProfileNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class UserProfileUpdaterTest {

	private static final String IMAGE_URL = "https://images.test.meongcoach.com/images/user-profile/1/a.jpg";

	@Autowired
	private UserProfileUpdater userProfileUpdater;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserProfileRepository userProfileRepository;

	@Autowired
	private EntityManager entityManager;

	private Long userId;

	@BeforeEach
	void setUp() {
		User user = userRepository.save(User.registerUser());
		userProfileRepository.save(UserProfile.create(user, new UserProfileCreateCommand(
				"멍멍이집사", null, "INFP", Set.of(1L), Set.of())));
		userId = user.getId();
		flushAndClear();
	}

	@Test
	void 프로필을_전체_교체한다() {
		UserProfileUpdateRequest request = new UserProfileUpdateRequest("새닉네임", IMAGE_URL, "ENTJ");

		UserProfile updated = userProfileUpdater.update(userId, request);

		assertThat(updated.getNickname()).isEqualTo("새닉네임");
		flushAndClear();
		UserProfile saved = userProfileRepository.findById(userId).orElseThrow();
		assertThat(saved.getNickname()).isEqualTo("새닉네임");
		assertThat(saved.getProfileImageUrl()).isEqualTo(IMAGE_URL);
		assertThat(saved.getMbti()).isEqualTo(Mbti.ENTJ);
	}

	@Test
	void 선택_입력을_비우면_미입력으로_교체한다() {
		userProfileUpdater.update(userId, new UserProfileUpdateRequest("새닉네임", null, null));

		flushAndClear();
		UserProfile saved = userProfileRepository.findById(userId).orElseThrow();
		assertThat(saved.getProfileImageUrl()).isEmpty();
		assertThat(saved.getMbti()).isNull();
	}

	@Test
	void 프로필을_수정해도_배운_토픽은_유지된다() {
		userProfileUpdater.update(userId, new UserProfileUpdateRequest("새닉네임", null, null));

		flushAndClear();
		UserProfile saved = userProfileRepository.findById(userId).orElseThrow();
		assertThat(saved.getPriorTrainingTopicIds()).containsExactly(1L);
	}

	@Test
	void 우리_스토리지_URL이_아닌_이미지면_실패한다() {
		UserProfileUpdateRequest request = new UserProfileUpdateRequest("새닉네임", "https://evil.example.com/a.jpg", null);

		assertThatThrownBy(() -> userProfileUpdater.update(userId, request))
				.isInstanceOf(InvalidImageUrlException.class);
	}

	@Test
	void 유효하지_않은_MBTI면_실패한다() {
		UserProfileUpdateRequest request = new UserProfileUpdateRequest("새닉네임", null, "XXXX");

		assertThatThrownBy(() -> userProfileUpdater.update(userId, request))
				.isInstanceOf(InvalidMbtiException.class);
	}

	@Test
	void 닉네임이_비어_있으면_검증에_실패한다() {
		UserProfileUpdateRequest request = new UserProfileUpdateRequest(" ", null, null);

		assertThatThrownBy(() -> userProfileUpdater.update(userId, request))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@Test
	void 프로필이_없으면_수정에_실패한다() {
		UserProfileUpdateRequest request = new UserProfileUpdateRequest("새닉네임", null, null);

		assertThatThrownBy(() -> userProfileUpdater.update(999L, request))
				.isInstanceOf(UserProfileNotFoundException.class);
	}

	@Test
	void 배운_토픽을_추가하면_기존_토픽에_누적되고_중복은_제거된다() {
		userProfileUpdater.addPriorTrainingTopics(userId, List.of(1L, 2L, 2L, 3L));

		flushAndClear();
		UserProfile saved = userProfileRepository.findById(userId).orElseThrow();
		assertThat(saved.getPriorTrainingTopicIds()).containsExactlyInAnyOrder(1L, 2L, 3L);
	}

	@Test
	void 배운_토픽을_추가할_때_프로필이_없으면_실패한다() {
		assertThatThrownBy(() -> userProfileUpdater.addPriorTrainingTopics(999L, List.of(1L)))
				.isInstanceOf(UserProfileNotFoundException.class);
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
