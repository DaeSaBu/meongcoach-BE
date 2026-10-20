package com.daesabu.meongcoach.community.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.community.domain.MentionCommand;
import com.daesabu.meongcoach.community.domain.MentionTargetType;
import com.daesabu.meongcoach.community.domain.Post;
import com.daesabu.meongcoach.community.domain.PostMention;
import com.daesabu.meongcoach.community.domain.PostWriteCommand;
import com.daesabu.meongcoach.community.domain.exception.MentionTargetNotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;

class MentionTargetVerifiersTest {

	private static final PostMention CARD_MENTION = mentionOf(MentionTargetType.CARD, 1L);

	@Test
	void 대상_종류에_맞는_확인기로_존재를_확인한다() {
		MentionTargetVerifiers verifiers = new MentionTargetVerifiers(
				List.of(new StubMentionTargetVerifier(MentionTargetType.CARD, true)));

		assertThatCode(() -> verifiers.validateExists(CARD_MENTION))
				.doesNotThrowAnyException();
	}

	@Test
	void 대상이_없으면_예외를_던진다() {
		MentionTargetVerifiers verifiers = new MentionTargetVerifiers(
				List.of(new StubMentionTargetVerifier(MentionTargetType.CARD, false)));

		assertThatThrownBy(() -> verifiers.validateExists(CARD_MENTION))
				.isInstanceOf(MentionTargetNotFoundException.class);
	}

	@Test
	void 확인기가_없는_대상_종류가_있으면_생성할_수_없다() {
		List<MentionTargetVerifier> verifiers = List.of();

		assertThatThrownBy(() -> new MentionTargetVerifiers(verifiers))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("CARD");
	}

	private static PostMention mentionOf(MentionTargetType targetType, Long targetId) {
		MentionCommand mention = new MentionCommand(targetType, targetId);
		Post post = Post.write(42L, new PostWriteCommand("본문", mention));
		return post.getMention();
	}

	private record StubMentionTargetVerifier(MentionTargetType targetType, boolean found)
			implements MentionTargetVerifier {

		@Override
		public boolean exists(Long targetId) {
			return found;
		}
	}
}
