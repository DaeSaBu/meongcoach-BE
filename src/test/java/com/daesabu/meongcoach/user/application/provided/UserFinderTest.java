package com.daesabu.meongcoach.user.application.provided;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfileCreateCommand;
import com.daesabu.meongcoach.user.domain.UserRole;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

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

		UserProfileSummaries summaries = userFinder.findProfileSummaries(Set.of(user.getId()));

		assertThat(summaries.profiles()).containsExactly(
				new UserProfileSummaryResult(user.getId(), "멍멍이집사", "https://cdn.example.com/profile.png", false));
	}

	@Test
	void 프로필이_없는_사용자는_결과에서_빠진다() {
		User withProfile = saveUserWithProfile("멍멍이집사", "");
		User withoutProfile = userRepository.save(User.registerUser());
		flushAndClear();

		UserProfileSummaries summaries = userFinder.findProfileSummaries(
				Set.of(withProfile.getId(), withoutProfile.getId()));

		assertThat(summaries.profiles()).extracting(UserProfileSummaryResult::userId).containsExactly(withProfile.getId());
	}

	@Test
	void 빈_id_목록이면_빈_목록을_반환한다() {
		UserProfileSummaries summaries = userFinder.findProfileSummaries(Set.of());

		assertThat(summaries.profiles()).isEmpty();
	}

	@Test
	void 전문가_프로필과_보호자_회원_ID를_나눠_반환한다() {
		User guardian = saveUserWithProfile("멍멍이집사", "");
		User expert = saveUserWithProfile("멍코치 훈련사", "https://cdn.example.com/coach.png");
		entityManager.createNativeQuery("UPDATE users SET role = 'EXPERT' WHERE id = :id")
				.setParameter("id", expert.getId())
				.executeUpdate();
		flushAndClear();

		UserProfileSummaries summaries = userFinder.findProfileSummaries(Set.of(guardian.getId(), expert.getId()));

		assertThat(summaries.experts()).containsExactly(
				new UserProfileSummaryResult(expert.getId(), "멍코치 훈련사", "https://cdn.example.com/coach.png", true));
		assertThat(summaries.guardianUserIds()).containsExactly(guardian.getId());
	}

	@Test
	void 회원_ID_목록에서_활성_회원의_ID만_반환한다() {
		User active = userRepository.save(User.registerUser());
		User withdrawn = userRepository.save(User.registerUser());
		withdrawn.withdraw();
		flushAndClear();

		Set<Long> activeIds = userFinder.findActiveUserIds(
				List.of(active.getId(), withdrawn.getId(), withdrawn.getId() + 1_000_000L));

		assertThat(activeIds).containsExactly(active.getId());
	}

	@Test
	void 빈_ID_목록이면_빈_활성_회원_ID_집합을_반환한다() {
		Set<Long> activeIds = userFinder.findActiveUserIds(List.of());

		assertThat(activeIds).isEmpty();
	}

    @Test
    void 활성_ADMIN과_EXPERT의_ID만_반환한다() {
        User admin = userRepository.save(User.registerUser());
        User expert = userRepository.save(User.registerUser());
        User withdrawn = userRepository.save(User.registerUser());
        userRepository.save(User.registerUser());
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        ReflectionTestUtils.setField(expert, "role", UserRole.EXPERT);
        ReflectionTestUtils.setField(withdrawn, "role", UserRole.EXPERT);
        withdrawn.withdraw();
        flushAndClear();

        assertThat(userFinder.findActiveOperatorIds()).containsExactlyInAnyOrder(admin.getId(), expert.getId());
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
