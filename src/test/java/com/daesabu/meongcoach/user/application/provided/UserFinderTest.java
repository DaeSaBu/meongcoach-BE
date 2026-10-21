package com.daesabu.meongcoach.user.application.provided;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfileCreateCommand;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class UserFinderTest {

	@Autowired
	private UserFinder userFinder;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 사용자_id와_닉네임과_프로필_이미지를_요약으로_반환한다() {
		User user = saveUserWithProfile("멍멍이집사", "https://cdn.example.com/profile.png");
		flushAndClear();

		List<UserProfileSummaryResult> summaries = userFinder.findProfileSummaries(Set.of(user.getId()));

		assertThat(summaries).containsExactly(
				new UserProfileSummaryResult(user.getId(), "멍멍이집사", "https://cdn.example.com/profile.png"));
	}

	@Test
	void 프로필이_없는_사용자는_결과에서_빠진다() {
		User withProfile = saveUserWithProfile("멍멍이집사", "");
		User withoutProfile = userRepository.save(User.registerUser());
		flushAndClear();

		List<UserProfileSummaryResult> summaries = userFinder.findProfileSummaries(
				Set.of(withProfile.getId(), withoutProfile.getId()));

		assertThat(summaries).extracting(UserProfileSummaryResult::userId).containsExactly(withProfile.getId());
	}

	@Test
	void 빈_id_목록이면_빈_목록을_반환한다() {
		List<UserProfileSummaryResult> summaries = userFinder.findProfileSummaries(Set.of());

		assertThat(summaries).isEmpty();
	}

	private User saveUserWithProfile(String nickname, String profileImageUrl) {
		User user = User.registerUser();
		user.completeOnboarding(new UserProfileCreateCommand(nickname, profileImageUrl, "INFP", Set.of(), Set.of()));
		return userRepository.save(user);
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
