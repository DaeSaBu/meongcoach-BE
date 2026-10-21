package com.daesabu.meongcoach.community.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CommentClosureTest {

	private final Post post = Post.write(7L, new PostWriteCommand("오늘 앉아 훈련 성공!", null));

	@Test
	void 자기_자신_경로는_조상과_자손이_같고_깊이가_0이다() {
		Comment comment = Comment.write(post, 42L, "저희 강아지도 성공했어요!");

		CommentClosure closure = CommentClosure.self(comment);

		assertThat(closure.getAncestor()).isSameAs(comment);
		assertThat(closure.getDescendant()).isSameAs(comment);
		assertThat(closure.getDepth()).isZero();
	}

	@Test
	void 답글_경로는_루트가_조상이고_답글이_자손이며_깊이가_1이다() {
		Comment root = Comment.write(post, 42L, "저희 강아지도 성공했어요!");
		Comment reply = Comment.write(post, 43L, "축하해요!");

		CommentClosure closure = CommentClosure.reply(root, reply);

		assertThat(closure.getAncestor()).isSameAs(root);
		assertThat(closure.getDescendant()).isSameAs(reply);
		assertThat(closure.getDepth()).isEqualTo(1);
	}
}
