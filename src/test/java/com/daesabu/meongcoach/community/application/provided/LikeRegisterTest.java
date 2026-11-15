package com.daesabu.meongcoach.community.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.community.application.provided.dto.CommentListRequest;
import com.daesabu.meongcoach.community.application.provided.dto.CommentRegisterRequest;
import com.daesabu.meongcoach.community.application.provided.dto.PostListRequest;
import com.daesabu.meongcoach.community.application.required.CommentLikeRepository;
import com.daesabu.meongcoach.community.application.required.CommentRepository;
import com.daesabu.meongcoach.community.application.required.PostLikeRepository;
import com.daesabu.meongcoach.community.application.required.PostRepository;
import com.daesabu.meongcoach.community.domain.Comment;
import com.daesabu.meongcoach.community.domain.CommentLike;
import com.daesabu.meongcoach.community.domain.Post;
import com.daesabu.meongcoach.community.domain.PostLike;
import com.daesabu.meongcoach.community.domain.PostRegisterCommand;
import com.daesabu.meongcoach.community.domain.exception.CommentNotFoundException;
import com.daesabu.meongcoach.community.domain.exception.PostNotFoundException;
import com.daesabu.meongcoach.support.ApplicationTest;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class LikeRegisterTest {

	@Autowired
	private LikeRegister likeRegister;
	@Autowired
	private PostLikeRepository postLikeRepository;
	@Autowired
	private CommentLikeRepository commentLikeRepository;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private PostFinder postFinder;
	@Autowired
	private CommentFinder commentFinder;
	@Autowired
	private CommentRegister commentRegister;
	@Autowired
	private LikeFinder likeFinder;
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

	@Test
	void 목록과_상세는_현재_사용자의_좋아요_상태를_반환한다() {
		Post post = savePost();
		Comment comment = commentRepository.save(Comment.register(post, 41L, "댓글"));
		likeRegister.likePost(42L, post.getId());
		likeRegister.likeComment(42L, comment.getId());

		var detail = postFinder.findPostDetail(42L, post.getId());
		var otherDetail = postFinder.findPostDetail(43L, post.getId());
		var posts = postFinder.findPosts(42L,
				new PostListRequest(null, 10, null));
		var comments = commentFinder.findComments(42L, post.getId(),
				new CommentListRequest(null, 10));

		assertThat(detail.likeCount()).isEqualTo(1);
		assertThat(detail.likedByMe()).isTrue();
		assertThat(otherDetail.likeCount()).isEqualTo(1);
		assertThat(otherDetail.likedByMe()).isFalse();
		assertThat(posts.posts()).anySatisfy(result -> {
			assertThat(result.post().getId()).isEqualTo(post.getId());
			assertThat(result.likedByMe()).isTrue();
		});
		assertThat(comments.comments().getFirst().likedByMe()).isTrue();
		assertThat(comments.comments().getFirst().likeCount()).isEqualTo(1);
	}

	@Test
	void 좋아요가_없는_대상과_빈_목록을_조회한다() {
		Post post = savePost();
		var likes = likeFinder.findPostLikes(42L, List.of(post.getId()));

		assertThat(likes.get(post.getId()).likeCount()).isZero();
		assertThat(likes.get(post.getId()).likedByMe()).isFalse();
		assertThat(likeFinder.findPostLikes(42L, List.of())).isEmpty();
		assertThat(likeFinder.findCommentLikes(42L, List.of())).isEmpty();
	}

	@Test
	void 답글에도_좋아요를_추가하고_조회한다() {
		Post post = savePost();
		Comment root = commentRegister.register(41L, post.getId(),
				new CommentRegisterRequest("댓글", null));
		Comment reply = commentRegister.register(42L, post.getId(),
				new CommentRegisterRequest("답글", root.getId()));
		likeRegister.likeComment(43L, reply.getId());

		var replies = commentFinder.findReplies(43L, root.getId(),
				new CommentListRequest(null, 10));

		assertThat(replies.replies().getFirst().likeCount()).isEqualTo(1);
		assertThat(replies.replies().getFirst().likedByMe()).isTrue();
	}

	@Test
	void 게시글_좋아요의_감사_시각을_저장하고_중복_요청에서도_유지한다() {
		Post post = savePost();
		Long targetId = post.getId();
		likeRegister.likePost(42L, targetId);
		entityManager.clear();
		PostLike saved = postLikeRepository.findAll().stream()
				.filter(like -> like.getPostId().equals(targetId))
				.findFirst().orElseThrow();
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
		LocalDateTime original = LocalDateTime.of(2026, 1, 1, 0, 0);
		entityManager.createNativeQuery("update post_likes set created_at = :at, updated_at = :at where id = :id")
				.setParameter("at", original).setParameter("id", saved.getId()).executeUpdate();
		entityManager.clear();

		likeRegister.likePost(42L, targetId);
		entityManager.clear();

		PostLike found = postLikeRepository.findById(saved.getId()).orElseThrow();
		assertThat(found.getCreatedAt()).isEqualTo(original);
		assertThat(found.getUpdatedAt()).isEqualTo(original);
	}

	@Test
	void 댓글_좋아요의_감사_시각을_저장하고_중복_요청에서도_유지한다() {
		Post post = savePost();
		Comment comment = commentRepository.save(Comment.register(post, 41L, "댓글"));
		Long targetId = comment.getId();
		likeRegister.likeComment(42L, targetId);
		entityManager.clear();
		CommentLike saved = commentLikeRepository.findAll().stream()
				.filter(like -> like.getCommentId().equals(targetId))
				.findFirst().orElseThrow();
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
		LocalDateTime original = LocalDateTime.of(2026, 1, 1, 0, 0);
		entityManager.createNativeQuery("update comment_likes set created_at = :at, updated_at = :at where id = :id")
				.setParameter("at", original).setParameter("id", saved.getId()).executeUpdate();
		entityManager.clear();

		likeRegister.likeComment(42L, targetId);
		entityManager.clear();

		CommentLike found = commentLikeRepository.findById(saved.getId()).orElseThrow();
		assertThat(found.getCreatedAt()).isEqualTo(original);
		assertThat(found.getUpdatedAt()).isEqualTo(original);
	}
}
