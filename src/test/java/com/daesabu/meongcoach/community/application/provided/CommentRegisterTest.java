package com.daesabu.meongcoach.community.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.community.application.provided.dto.CommentRegisterRequest;
import com.daesabu.meongcoach.community.application.required.CommentClosureRepository;
import com.daesabu.meongcoach.community.application.required.CommentRepository;
import com.daesabu.meongcoach.community.application.required.PostRepository;
import com.daesabu.meongcoach.community.domain.Comment;
import com.daesabu.meongcoach.community.domain.CommentClosure;
import com.daesabu.meongcoach.community.domain.Post;
import com.daesabu.meongcoach.community.domain.PostWriteCommand;
import com.daesabu.meongcoach.community.domain.exception.CommentNotFoundException;
import com.daesabu.meongcoach.community.domain.exception.InvalidCommentContentException;
import com.daesabu.meongcoach.community.domain.exception.PostNotFoundException;
import com.daesabu.meongcoach.support.ApplicationTest;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class CommentRegisterTest {

	private static final Long USER_ID = 42L;
	private static final Long OTHER_USER_ID = 99L;

	@Autowired
	private CommentRegister commentRegister;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private CommentRepository commentRepository;

	@Autowired
	private CommentClosureRepository commentClosureRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 게시글에_댓글을_등록한다() {
		Post post = savePost();

		Comment registered = commentRegister.register(USER_ID, post.getId(), new CommentRegisterRequest("축하해요!", null));
		flushAndClear();

		Comment found = commentRepository.findById(registered.getId()).orElseThrow();
		assertThat(found.getPost().getId()).isEqualTo(post.getId());
		assertThat(found.getUserId()).isEqualTo(USER_ID);
		assertThat(found.getContent().value()).isEqualTo("축하해요!");
	}

	@Test
	void 댓글은_자기_자신_경로만_갖는다() {
		Post post = savePost();

		Comment registered = commentRegister.register(USER_ID, post.getId(), new CommentRegisterRequest("축하해요!", null));
		flushAndClear();

		CommentClosure closure = commentClosureRepository.findFirstByDescendantIdOrderByDepthDesc(registered.getId())
				.orElseThrow();
		assertThat(closure.getAncestor().getId()).isEqualTo(registered.getId());
		assertThat(closure.getDepth()).isZero();
		assertThat(commentClosureRepository.count()).isOne();
	}

	@Test
	void 댓글에_답글을_등록하면_루트_댓글_경로가_생긴다() {
		Post post = savePost();
		Comment root = saveRootComment(post);

		Comment reply = commentRegister.register(USER_ID, post.getId(), new CommentRegisterRequest("감사해요!", root.getId()));
		flushAndClear();

		CommentClosure closure = commentClosureRepository.findFirstByDescendantIdOrderByDepthDesc(reply.getId())
				.orElseThrow();
		assertThat(closure.getAncestor().getId()).isEqualTo(root.getId());
		assertThat(closure.getDepth()).isOne();
	}

	@Test
	void 답글에_답글을_달면_루트_댓글의_답글로_등록한다() {
		Post post = savePost();
		Comment root = saveRootComment(post);
		Comment reply = saveReply(root);

		Comment replyOfReply = commentRegister.register(USER_ID, post.getId(),
				new CommentRegisterRequest("저도요!", reply.getId()));
		flushAndClear();

		CommentClosure closure = commentClosureRepository.findFirstByDescendantIdOrderByDepthDesc(replyOfReply.getId())
				.orElseThrow();
		assertThat(closure.getAncestor().getId()).isEqualTo(root.getId());
		assertThat(closure.getDepth()).isOne();
	}

	@Test
	void 게시글이_없으면_예외를_던진다() {
		CommentRegisterRequest request = new CommentRegisterRequest("축하해요!", null);

		assertThatThrownBy(() -> commentRegister.register(USER_ID, 999_999L, request))
				.isInstanceOf(PostNotFoundException.class);
	}

	@Test
	void 부모_댓글이_없으면_예외를_던지고_저장하지_않는다() {
		Post post = savePost();
		CommentRegisterRequest request = new CommentRegisterRequest("감사해요!", 999_999L);

		assertThatThrownBy(() -> commentRegister.register(USER_ID, post.getId(), request))
				.isInstanceOf(CommentNotFoundException.class);
		assertThat(commentRepository.count()).isZero();
	}

	@Test
	void 삭제된_댓글에는_답글을_달_수_없다() {
		Post post = savePost();
		Comment root = saveRootComment(post);
		root.delete();
		CommentRegisterRequest request = new CommentRegisterRequest("감사해요!", root.getId());

		assertThatThrownBy(() -> commentRegister.register(USER_ID, post.getId(), request))
				.isInstanceOf(CommentNotFoundException.class);
	}

	@Test
	void 다른_게시글의_댓글에는_답글을_달_수_없다() {
		Post post = savePost();
		Comment otherPostComment = saveRootComment(savePost());
		CommentRegisterRequest request = new CommentRegisterRequest("감사해요!", otherPostComment.getId());

		assertThatThrownBy(() -> commentRegister.register(USER_ID, post.getId(), request))
				.isInstanceOf(CommentNotFoundException.class);
	}

	@Test
	void 본문이_비어_있으면_예외를_던진다() {
		Post post = savePost();
		CommentRegisterRequest request = new CommentRegisterRequest(" ", null);

		assertThatThrownBy(() -> commentRegister.register(USER_ID, post.getId(), request))
				.isInstanceOf(InvalidCommentContentException.class);
	}

	@Test
	void parentId가_양수가_아니면_예외를_던진다() {
		Post post = savePost();
		CommentRegisterRequest request = new CommentRegisterRequest("감사해요!", 0L);

		assertThatThrownBy(() -> commentRegister.register(USER_ID, post.getId(), request))
				.isInstanceOf(ConstraintViolationException.class);
	}

	private Post savePost() {
		return postRepository.save(Post.register(OTHER_USER_ID, new PostWriteCommand("오늘 앉아 훈련 성공!", null, List.of())));
	}

	private Comment saveRootComment(Post post) {
		Comment root = commentRepository.save(Comment.register(post, OTHER_USER_ID, "축하해요!"));
		commentClosureRepository.save(CommentClosure.self(root));
		return root;
	}

	private Comment saveReply(Comment root) {
		Comment reply = commentRepository.save(Comment.register(root.getPost(), OTHER_USER_ID, "감사해요!"));
		commentClosureRepository.save(CommentClosure.self(reply));
		commentClosureRepository.save(CommentClosure.reply(root, reply));
		return reply;
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
