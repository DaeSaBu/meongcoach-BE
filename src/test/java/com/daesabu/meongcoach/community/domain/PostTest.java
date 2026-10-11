package com.daesabu.meongcoach.community.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.community.domain.exception.InvalidPostContentException;
import java.util.List;
import org.junit.jupiter.api.Test;

class PostTest {

	private static final Long USER_ID = 42L;

	@Test
	void 작성하면_작성자와_본문이_채워진다() {
		Post post = Post.write(new PostWriteCommand(USER_ID, "오늘 앉아 훈련 성공!", List.of()));

		assertThat(post.getUserId()).isEqualTo(USER_ID);
		assertThat(post.getContent()).isEqualTo("오늘 앉아 훈련 성공!");
		assertThat(post.getMentions()).isEmpty();
	}

	@Test
	void 멘션은_게시글에_묶여_생성된다() {
		Post post = Post.write(new PostWriteCommand(USER_ID, "본문",
				List.of(new MentionCommand(MentionTargetType.CARD, 10L))));

		PostMention mention = post.getMentions().getFirst();
		assertThat(mention.getPost()).isSameAs(post);
		assertThat(mention.getTargetType()).isEqualTo(MentionTargetType.CARD);
		assertThat(mention.getTargetId()).isEqualTo(10L);
	}

	@Test
	void 같은_대상을_여러_번_멘션하면_하나만_남긴다() {
		Post post = Post.write(new PostWriteCommand(USER_ID, "본문", List.of(
				new MentionCommand(MentionTargetType.CARD, 10L),
				new MentionCommand(MentionTargetType.CARD, 10L),
				new MentionCommand(MentionTargetType.CARD, 11L))));

		assertThat(post.findMentionedCardIds()).containsExactly(10L, 11L);
	}

	@Test
	void 본문이_비어_있으면_작성할_수_없다() {
		assertThatThrownBy(() -> Post.write(new PostWriteCommand(USER_ID, "   ", List.of())))
				.isInstanceOf(InvalidPostContentException.class);
	}

	@Test
	void 본문이_2000자를_넘으면_작성할_수_없다() {
		String tooLong = "a".repeat(Post.MAX_CONTENT_LENGTH + 1);

		assertThatThrownBy(() -> Post.write(new PostWriteCommand(USER_ID, tooLong, List.of())))
				.isInstanceOf(InvalidPostContentException.class);
	}

	@Test
	void 본문이_2000자면_작성할_수_있다() {
		String maxLength = "a".repeat(Post.MAX_CONTENT_LENGTH);

		Post post = Post.write(new PostWriteCommand(USER_ID, maxLength, List.of()));

		assertThat(post.getContent()).hasSize(Post.MAX_CONTENT_LENGTH);
	}

	@Test
	void 작성자_본인인지_판단한다() {
		Post post = Post.write(new PostWriteCommand(USER_ID, "본문", List.of()));

		assertThat(post.isWrittenBy(USER_ID)).isTrue();
		assertThat(post.isWrittenBy(43L)).isFalse();
	}
}
