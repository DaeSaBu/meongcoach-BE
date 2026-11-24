package com.daesabu.meongcoach.community.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.community.domain.exception.SelfBlockNotAllowedException;
import org.junit.jupiter.api.Test;

class UserBlockTest {

	private static final Long BLOCKER_ID = 42L;

	private static final Long BLOCKED_ID = 41L;

	@Test
	void 차단을_등록하면_차단한_회원과_차단된_회원을_담는다() {
		UserBlock block = UserBlock.register(BLOCKER_ID, BLOCKED_ID);

		assertThat(block.getBlockerId()).isEqualTo(BLOCKER_ID);
		assertThat(block.getBlockedId()).isEqualTo(BLOCKED_ID);
	}

	@Test
	void 자기_자신은_차단할_수_없다() {
		assertThatThrownBy(() -> UserBlock.register(BLOCKER_ID, BLOCKER_ID))
				.isInstanceOf(SelfBlockNotAllowedException.class);
	}
}
