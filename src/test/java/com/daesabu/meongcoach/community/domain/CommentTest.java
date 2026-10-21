package com.daesabu.meongcoach.community.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.community.domain.exception.InvalidCommentContentException;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommentTest {

	private static final Long USER_ID = 42L;

	private final Post post = Post.register(7L, new PostRegisterCommand("오늘 앉아 훈련 성공!", null, List.of()));

	@Test
	void 등록하면_게시글과_작성자와_본문이_채워진다() {
		Comment comment = Comment.register(post, USER_ID, "저희 강아지도 성공했어요!");

		assertThat(comment.getPost()).isSameAs(post);
		assertThat(comment.getUserId()).isEqualTo(USER_ID);
		assertThat(comment.getContent()).isEqualTo(new CommentContent("저희 강아지도 성공했어요!"));
		assertThat(comment.getDeletedAt()).isNull();
	}

	@Test
	void 본문이_비어_있으면_등록할_수_없다() {
		assertThatThrownBy(() -> Comment.register(post, USER_ID, "   "))
				.isInstanceOf(InvalidCommentContentException.class);
	}

	@Test
	void 삭제하면_삭제_시각이_기록된다() {
		Comment comment = Comment.register(post, USER_ID, "저희 강아지도 성공했어요!");

		comment.delete();

		assertThat(comment.getDeletedAt()).isNotNull();
	}
}
