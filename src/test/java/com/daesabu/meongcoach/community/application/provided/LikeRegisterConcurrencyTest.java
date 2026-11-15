package com.daesabu.meongcoach.community.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.community.application.required.CommentLikeRepository;
import com.daesabu.meongcoach.community.application.required.CommentRepository;
import com.daesabu.meongcoach.community.application.required.PostLikeRepository;
import com.daesabu.meongcoach.community.application.required.PostRepository;
import com.daesabu.meongcoach.community.domain.Comment;
import com.daesabu.meongcoach.community.domain.LikeSummary;
import com.daesabu.meongcoach.community.domain.Post;
import com.daesabu.meongcoach.community.domain.PostRegisterCommand;
import com.daesabu.meongcoach.support.NonTransactionalApplicationTest;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

@NonTransactionalApplicationTest
class LikeRegisterConcurrencyTest {
	@Autowired
	private LikeRegister likeRegister;
	@Autowired
	private PostRepository postRepository;
	@Autowired
	private PostLikeRepository postLikeRepository;
	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private CommentRepository commentRepository;
	@Autowired
	private CommentLikeRepository commentLikeRepository;

	private Long postId;
	private Long commentId;

	@BeforeEach
	void setUp() {
		transactionTemplate.executeWithoutResult(status -> {
			Post post = postRepository.save(Post.register(41L, new PostRegisterCommand("동시성", null, List.of())));
			postId = post.getId();
			commentId = commentRepository.save(Comment.register(post, 41L, "댓글")).getId();
		});
	}

	@AfterEach
	void cleanUp() {
		transactionTemplate.executeWithoutResult(status -> {
			postLikeRepository.findByPostIdAndUserId(postId, 42L).ifPresent(postLikeRepository::delete);
			commentLikeRepository.findByCommentIdAndUserId(commentId, 42L).ifPresent(commentLikeRepository::delete);
			commentRepository.deleteById(commentId);
			postRepository.deleteById(postId);
		});
	}

	@Test
	void 같은_사용자의_동시_게시글_좋아요는_한_번만_저장된다() throws Exception {
		LikeSummary result = assertSerialized(
				() -> likeRegister.likePost(42L, postId),
				() -> likeRegister.likePost(42L, postId));

		assertThat(result).isEqualTo(new LikeSummary(1, true));
	}

	@Test
	void 같은_사용자의_동시_댓글_좋아요는_한_번만_저장된다() throws Exception {
		LikeSummary result = assertSerialized(
				() -> likeRegister.likeComment(42L, commentId),
				() -> likeRegister.likeComment(42L, commentId));

		assertThat(result).isEqualTo(new LikeSummary(1, true));
	}

	@Test
	void 게시글_좋아요_복구는_진행_중인_취소가_커밋될_때까지_기다린다() throws Exception {
		likeRegister.likePost(42L, postId);
		Long originalId = postLikeRepository.findByPostIdAndUserId(postId, 42L).orElseThrow().getId();

		LikeSummary result = assertSerialized(
				() -> likeRegister.unlikePost(42L, postId),
				() -> likeRegister.likePost(42L, postId));

		assertThat(result).isEqualTo(new LikeSummary(1, true));
		assertThat(postLikeRepository.findByPostIdAndUserId(postId, 42L).orElseThrow().getId()).isEqualTo(originalId);
	}

	@Test
	void 댓글_좋아요_복구는_진행_중인_취소가_커밋될_때까지_기다린다() throws Exception {
		likeRegister.likeComment(42L, commentId);
		Long originalId = commentLikeRepository.findByCommentIdAndUserId(commentId, 42L).orElseThrow().getId();

		LikeSummary result = assertSerialized(
				() -> likeRegister.unlikeComment(42L, commentId),
				() -> likeRegister.likeComment(42L, commentId));

		assertThat(result).isEqualTo(new LikeSummary(1, true));
		assertThat(commentLikeRepository.findByCommentIdAndUserId(commentId, 42L).orElseThrow().getId()).isEqualTo(originalId);
	}

	private LikeSummary assertSerialized(Supplier<LikeSummary> firstAction, Supplier<LikeSummary> secondAction) throws Exception {
		CountDownLatch firstApplied = new CountDownLatch(1);
		CountDownLatch secondStarted = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var first = executor.submit(() -> transactionTemplate.execute(status -> {
				LikeSummary result = firstAction.get();
				firstApplied.countDown();
				await(release);
				return result;
			}));
			try {
				assertThat(firstApplied.await(10, TimeUnit.SECONDS)).isTrue();
				var second = executor.submit(() -> {
					secondStarted.countDown();
					return secondAction.get();
				});
				assertThat(secondStarted.await(10, TimeUnit.SECONDS)).isTrue();
				assertThatThrownBy(() -> second.get(300, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
				release.countDown();
				first.get(10, TimeUnit.SECONDS);
				return second.get(10, TimeUnit.SECONDS);
			} finally {
				release.countDown();
			}
		}
	}

	private void await(CountDownLatch latch) {
		try {
			if (!latch.await(10, TimeUnit.SECONDS)) {
				throw new IllegalStateException("대기 시간 초과");
			}
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(exception);
		}
	}
}
