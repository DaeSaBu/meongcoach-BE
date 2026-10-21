package com.daesabu.meongcoach.community.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.community.domain.Comment;
import com.daesabu.meongcoach.community.domain.CommentClosure;
import com.daesabu.meongcoach.community.domain.Post;
import com.daesabu.meongcoach.community.domain.PostWriteCommand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class CommentClosureRepositoryTest {

	private static final Long USER_ID = 42L;

	@Autowired
	private CommentClosureRepository commentClosureRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 루트_댓글의_가장_깊은_경로는_자기_자신_경로다() {
		Comment root = persistComment();
		entityManager.persist(CommentClosure.self(root));

		CommentClosure found = commentClosureRepository.findFirstByDescendantIdOrderByDepthDesc(root.getId())
				.orElseThrow();

		assertThat(found.getAncestor().getId()).isEqualTo(root.getId());
		assertThat(found.getDepth()).isZero();
	}

	@Test
	void 답글의_가장_깊은_경로는_루트_댓글_경로다() {
		Comment root = persistComment();
		Comment reply = persistComment();
		entityManager.persist(CommentClosure.self(root));
		entityManager.persist(CommentClosure.self(reply));
		entityManager.persist(CommentClosure.reply(root, reply));

		CommentClosure found = commentClosureRepository.findFirstByDescendantIdOrderByDepthDesc(reply.getId())
				.orElseThrow();

		assertThat(found.getAncestor().getId()).isEqualTo(root.getId());
		assertThat(found.getDepth()).isOne();
	}

	@Test
	void 경로가_없으면_빈_Optional을_반환한다() {
		assertThat(commentClosureRepository.findFirstByDescendantIdOrderByDepthDesc(999_999L)).isEmpty();
	}

	private Comment persistComment() {
		Post post = entityManager.persist(Post.write(USER_ID, new PostWriteCommand("오늘 앉아 훈련 성공!", null)));
		return entityManager.persist(Comment.register(post, USER_ID, "축하해요!"));
	}
}
