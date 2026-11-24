package com.daesabu.meongcoach.community.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.community.application.required.UserBlockRepository;
import com.daesabu.meongcoach.community.domain.UserBlock;
import com.daesabu.meongcoach.community.domain.exception.SelfBlockNotAllowedException;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.exception.UserNotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class BlockRegisterTest {

	private static final Long USER_ID = 42L;

	private static final Long OTHER_USER_ID = 43L;

	@Autowired
	private BlockRegister blockRegister;

	@Autowired
	private UserBlockRepository userBlockRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void 회원을_차단하면_차단_기록을_저장한다() {
		Long targetUserId = saveUser();

		blockRegister.block(USER_ID, targetUserId);

		List<UserBlock> blocks = userBlockRepository.findAll();
		assertThat(blocks).hasSize(1);
		UserBlock block = blocks.getFirst();
		assertThat(block.getBlockerId()).isEqualTo(USER_ID);
		assertThat(block.getBlockedId()).isEqualTo(targetUserId);
	}

	@Test
	void 이미_차단한_회원을_다시_차단해도_기록은_하나다() {
		Long targetUserId = saveUser();
		blockRegister.block(USER_ID, targetUserId);

		blockRegister.block(USER_ID, targetUserId);

		assertThat(userBlockRepository.findAll()).hasSize(1);
	}

	@Test
	void 자기_자신은_차단할_수_없다() {
		assertThatThrownBy(() -> blockRegister.block(USER_ID, USER_ID))
				.isInstanceOf(SelfBlockNotAllowedException.class);
		assertThat(userBlockRepository.findAll()).isEmpty();
	}

	@Test
	void 없는_회원은_차단할_수_없다() {
		assertThatThrownBy(() -> blockRegister.block(USER_ID, 999_999L))
				.isInstanceOf(UserNotFoundException.class);
		assertThat(userBlockRepository.findAll()).isEmpty();
	}

	@Test
	void 차단을_해제하면_차단_기록을_삭제한다() {
		Long targetUserId = saveUser();
		blockRegister.block(USER_ID, targetUserId);

		blockRegister.unblock(USER_ID, targetUserId);

		assertThat(userBlockRepository.findAll()).isEmpty();
	}

	@Test
	void 차단하지_않은_회원의_차단을_해제해도_예외를_던지지_않는다() {
		Long targetUserId = saveUser();

		blockRegister.unblock(USER_ID, targetUserId);

		assertThat(userBlockRepository.findAll()).isEmpty();
	}

	@Test
	void 차단을_해제해도_다른_회원이_차단한_기록은_남는다() {
		Long targetUserId = saveUser();
		blockRegister.block(USER_ID, targetUserId);
		blockRegister.block(OTHER_USER_ID, targetUserId);

		blockRegister.unblock(USER_ID, targetUserId);

		assertThat(userBlockRepository.findAll()).extracting(UserBlock::getBlockerId).containsExactly(OTHER_USER_ID);
	}

	private Long saveUser() {
		User user = userRepository.save(User.registerUser());
		return user.getId();
	}
}
