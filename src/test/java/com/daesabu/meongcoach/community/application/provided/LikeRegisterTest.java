package com.daesabu.meongcoach.community.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.community.application.required.PostRepository;
import com.daesabu.meongcoach.community.application.required.CommentRepository;
import com.daesabu.meongcoach.community.domain.Post;
import com.daesabu.meongcoach.community.domain.Comment;
import com.daesabu.meongcoach.community.domain.PostRegisterCommand;
import com.daesabu.meongcoach.community.domain.exception.PostNotFoundException;
import com.daesabu.meongcoach.community.domain.exception.CommentNotFoundException;
import com.daesabu.meongcoach.support.ApplicationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class LikeRegisterTest {

	@Autowired
	private LikeRegister likeRegister;
	@Autowired
	private PostRepository postRepository;
	@Autowired
	private CommentRepository commentRepository;

	@Test
	void 게시글_좋아요는_중복되지_않고_본인_좋아요만_취소한다() {
		Post post = savePost();
		Long id = post.getId();
		likeRegister.likePost(42L, id);
		assertThat(likeRegister.likePost(42L, id).likeCount()).isEqualTo(1);
		assertThat(likeRegister.likePost(43L, id).likeCount()).isEqualTo(2);

		var result = likeRegister.unlikePost(42L, id);

		assertThat(result.likeCount()).isEqualTo(1);
		assertThat(result.likedByMe()).isFalse();
		assertThat(likeRegister.unlikePost(42L, id).likeCount()).isEqualTo(1);
	}

	@Test
	void 댓글_좋아요는_중복되지_않고_본인_좋아요만_취소한다() {
		Post post = savePost();
		Comment comment = commentRepository.save(Comment.register(post, 41L, "댓글"));
		Long id = comment.getId();
		assertThat(likeRegister.likeComment(42L, id).likedByMe()).isTrue();
		assertThat(likeRegister.likeComment(42L, id).likeCount()).isEqualTo(1);
		likeRegister.likeComment(43L, id);

		var result = likeRegister.unlikeComment(42L, id);

		assertThat(result.likeCount()).isEqualTo(1);
		assertThat(result.likedByMe()).isFalse();
		assertThat(likeRegister.unlikeComment(42L, id).likeCount()).isEqualTo(1);
	}

	@Test
	void 삭제된_게시글에는_좋아요를_추가하거나_취소할_수_없다() {
		Post post = savePost();
		post.delete();

		assertThatThrownBy(() -> likeRegister.likePost(42L, post.getId()))
				.isInstanceOf(PostNotFoundException.class);
		assertThatThrownBy(() -> likeRegister.unlikePost(42L, post.getId()))
				.isInstanceOf(PostNotFoundException.class);
	}

	@Test
	void 게시글이_삭제된_댓글에는_좋아요를_추가하거나_취소할_수_없다() {
		Post post = savePost();
		Comment comment = commentRepository.save(Comment.register(post, 41L, "댓글"));
		post.delete();

		assertThatThrownBy(() -> likeRegister.likeComment(42L, comment.getId()))
				.isInstanceOf(CommentNotFoundException.class);
		assertThatThrownBy(() -> likeRegister.unlikeComment(42L, comment.getId()))
				.isInstanceOf(CommentNotFoundException.class);
	}

	@Test
	void 삭제된_댓글에는_좋아요를_추가할_수_없다() {
		Post post = savePost();
		Comment comment = commentRepository.save(Comment.register(post, 41L, "댓글"));
		comment.delete();

		assertThatThrownBy(() -> likeRegister.likeComment(42L, comment.getId()))
				.isInstanceOf(CommentNotFoundException.class);
	}

	@Test
	void 없는_대상에는_좋아요를_추가할_수_없다() {
		assertThatThrownBy(() -> likeRegister.likePost(42L, Long.MAX_VALUE))
				.isInstanceOf(PostNotFoundException.class);
		assertThatThrownBy(() -> likeRegister.likeComment(42L, Long.MAX_VALUE))
				.isInstanceOf(CommentNotFoundException.class);
	}

	private Post savePost() {
		Post post = Post.register(41L, new PostRegisterCommand("본문", null, List.of()));
		return postRepository.save(post);
	}
}
