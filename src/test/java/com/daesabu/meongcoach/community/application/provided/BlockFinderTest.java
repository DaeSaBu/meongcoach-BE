package com.daesabu.meongcoach.community.application.provided;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.community.application.provided.dto.AuthorResult;
import com.daesabu.meongcoach.community.application.provided.dto.BlockedUserResult;
import com.daesabu.meongcoach.community.application.required.UserBlockRepository;
import com.daesabu.meongcoach.community.domain.UserBlock;
import com.daesabu.meongcoach.dog.application.required.DogRepository;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogRegisterCommand;
import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.dog.domain.shared.DogSex;
import com.daesabu.meongcoach.dog.domain.shared.RegisteredBreed;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfileCreateCommand;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class BlockFinderTest {

	private static final Long USER_ID = 42L;

	private static final Long OTHER_USER_ID = 43L;

	private static final String DOG_IMAGE_URL = "https://media.meongcoach.com/images/dog-profile/41/bori.jpg";

	@Autowired
	private BlockFinder blockFinder;

	@Autowired
	private UserBlockRepository userBlockRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private DogRepository dogRepository;

	@Test
	void 차단한_회원을_최근에_차단한_순서로_반환한다() {
		Long first = saveGuardian("보리");
		Long second = saveGuardian("초코");
		saveBlock(USER_ID, first);
		saveBlock(USER_ID, second);

		List<BlockedUserResult> result = blockFinder.findBlockedUsers(USER_ID);

		assertThat(result).extracting(BlockedUserResult::userId).containsExactly(second, first);
	}

	@Test
	void 차단한_회원의_선택된_강아지를_작성자_정보로_함께_반환한다() {
		Long blockedId = saveGuardian("보리");
		saveBlock(USER_ID, blockedId);

		List<BlockedUserResult> result = blockFinder.findBlockedUsers(USER_ID);

		assertThat(result.getFirst().author()).isEqualTo(new AuthorResult(blockedId, "보리", DOG_IMAGE_URL));
	}

	@Test
	void 작성자_정보가_없는_회원도_author가_null인_채로_반환한다() {
		User withoutProfile = userRepository.save(User.registerUser());
		saveBlock(USER_ID, withoutProfile.getId());

		List<BlockedUserResult> result = blockFinder.findBlockedUsers(USER_ID);

		assertThat(result).containsExactly(new BlockedUserResult(withoutProfile.getId(), null));
	}

	@Test
	void 다른_회원이_차단한_회원은_반환하지_않는다() {
		Long blockedId = saveGuardian("보리");
		saveBlock(OTHER_USER_ID, blockedId);

		List<BlockedUserResult> result = blockFinder.findBlockedUsers(USER_ID);

		assertThat(result).isEmpty();
	}

	@Test
	void 차단한_회원이면_차단_여부가_참이다() {
		saveBlock(USER_ID, OTHER_USER_ID);

		assertThat(blockFinder.isBlocked(USER_ID, OTHER_USER_ID)).isTrue();
	}

	@Test
	void 나를_차단한_회원을_내가_차단하지_않았으면_차단_여부가_거짓이다() {
		saveBlock(OTHER_USER_ID, USER_ID);

		assertThat(blockFinder.isBlocked(USER_ID, OTHER_USER_ID)).isFalse();
	}

	@Test
	void 대상_회원을_차단하지_않은_회원만_순서를_유지해_반환한다() {
		saveBlock(44L, USER_ID);
		saveBlock(45L, OTHER_USER_ID);

		List<Long> result = blockFinder.findUsersNotBlocking(USER_ID, List.of(46L, 44L, 45L, 43L));

		assertThat(result).containsExactly(46L, 45L, 43L);
	}

	@Test
	void 회원_목록이_비어_있으면_빈_목록을_반환한다() {
		List<Long> result = blockFinder.findUsersNotBlocking(USER_ID, List.of());

		assertThat(result).isEmpty();
	}

	private void saveBlock(Long blockerId, Long blockedId) {
		userBlockRepository.save(UserBlock.register(blockerId, blockedId));
	}

	private Long saveGuardian(String dogName) {
		User user = User.registerUser();
		user.completeOnboarding(new UserProfileCreateCommand("멍멍이집사", null, "INFP", Set.of(), Set.of()));
		Long userId = userRepository.save(user).getId();
		Dog dog = Dog.register(new DogRegisterCommand(userId, dogName, Breed.of(RegisteredBreed.POODLE), DogSex.MALE,
				LocalDate.of(2024, 3, 1), new BigDecimal("4.50"), DOG_IMAGE_URL, ""));
		dog.select();
		dogRepository.save(dog);
		return userId;
	}
}
