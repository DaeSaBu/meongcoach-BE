package com.daesabu.meongcoach.community.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.community.application.required.PostLikeRepository;
import com.daesabu.meongcoach.community.application.required.PostRepository;
import com.daesabu.meongcoach.community.domain.Post;
import com.daesabu.meongcoach.community.domain.PostRegisterCommand;
import com.daesabu.meongcoach.support.NonTransactionalApplicationTest;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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

	@Test
	void 같은_사용자의_동시_좋아요는_한_번만_저장된다() throws Exception {
		Post post = Post.register(41L, new PostRegisterCommand("동시성", null, List.of()));
		postRepository.save(post);
		Long id = post.getId();
		CountDownLatch inserted = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var first = executor.submit(() -> transactionTemplate.execute(status -> {
				var result = likeRegister.likePost(42L, id);
				inserted.countDown();
				await(release);
				return result;
			}));
			try {
				assertThat(inserted.await(10, TimeUnit.SECONDS)).isTrue();
				var second = executor.submit(() -> likeRegister.likePost(42L, id));
				assertThatThrownBy(() -> second.get(300, TimeUnit.MILLISECONDS))
						.isInstanceOf(TimeoutException.class);
				release.countDown();
				assertThat(first.get(10, TimeUnit.SECONDS).likeCount()).isEqualTo(1);
				assertThat(second.get(10, TimeUnit.SECONDS).likeCount()).isEqualTo(1);
			} finally {
				release.countDown();
			}
		} finally {
			transactionTemplate.executeWithoutResult(status -> postLikeRepository.deleteLike(id, 42L));
			postRepository.deleteById(id);
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
