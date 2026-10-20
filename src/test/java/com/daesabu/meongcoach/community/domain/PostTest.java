package com.daesabu.meongcoach.community.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.community.domain.exception.InvalidPostContentException;
import org.junit.jupiter.api.Test;

class PostTest {

	private static final Long USER_ID = 42L;

	@Test
	void 작성하면_작성자와_본문이_채워진다() {
		Post post = Post.write(USER_ID, new PostWriteCommand("오늘 앉아 훈련 성공!", null));

		assertThat(post.getUserId()).isEqualTo(USER_ID);
		assertThat(post.getContent()).isEqualTo(new PostContent("오늘 앉아 훈련 성공!"));
	}

	@Test
	void 멘션과_함께_작성하면_멘션이_채워진다() {
		MentionCommand mention = new MentionCommand(MentionTargetType.LESSON, 10L);

		Post post = Post.write(USER_ID, new PostWriteCommand("이 레슨 따라해봤어요", mention));

		assertThat(post.getMention().getTargetType()).isEqualTo(MentionTargetType.LESSON);
		assertThat(post.getMention().getTargetId()).isEqualTo(10L);
	}

	@Test
	void 본문이_비어_있으면_작성할_수_없다() {
		assertThatThrownBy(() -> Post.write(USER_ID, new PostWriteCommand("   ", null)))
				.isInstanceOf(InvalidPostContentException.class);
	}
}
