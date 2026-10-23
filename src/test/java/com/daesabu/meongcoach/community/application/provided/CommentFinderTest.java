package com.daesabu.meongcoach.community.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.community.application.provided.dto.CommentListRequest;
import com.daesabu.meongcoach.community.application.provided.dto.CommentListResult;
import com.daesabu.meongcoach.community.application.provided.dto.CommentResult;
import com.daesabu.meongcoach.community.application.provided.dto.ReplyListResult;
import com.daesabu.meongcoach.community.application.provided.dto.ReplyResult;
import com.daesabu.meongcoach.community.application.required.CommentClosureRepository;
import com.daesabu.meongcoach.community.application.required.CommentRepository;
import com.daesabu.meongcoach.community.application.required.PostRepository;
import com.daesabu.meongcoach.community.domain.Comment;
import com.daesabu.meongcoach.community.domain.CommentClosure;
import com.daesabu.meongcoach.community.domain.Post;
import com.daesabu.meongcoach.community.domain.PostRegisterCommand;
import com.daesabu.meongcoach.community.domain.exception.CommentNotFoundException;
import com.daesabu.meongcoach.community.domain.exception.PostNotFoundException;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.user.application.provided.UserProfileSummaryResult;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfileCreateCommand;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class CommentFinderTest {

	private static final Long USER_ID = 42L;

	@Autowired
	private CommentFinder commentFinder;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private CommentRepository commentRepository;

	@Autowired
	private CommentClosureRepository commentClosureRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	private Post post;

	@BeforeEach
	void setUp() {
		post = savePost();
	}

	@Test
	void 루트_댓글을_오래된_순으로_반환하고_답글은_제외한다() {
		Comment first = saveComment(USER_ID, "첫 댓글");
		saveReply(first, USER_ID, "첫 댓글의 답글");
		saveComment(USER_ID, "둘째 댓글");
		flushAndClear();

		CommentListResult result = commentFinder.findComments(post.getId(), new CommentListRequest(null, 20));

		assertThat(result.comments()).extracting(this::contentOf).containsExactly("첫 댓글", "둘째 댓글");
		assertThat(result.nextCursor()).isNull();
	}

	@Test
	void size보다_댓글이_많으면_다음_커서와_함께_size개만_반환한다() {
		List<Comment> comments = saveComments(3);
		flushAndClear();

		CommentListResult result = commentFinder.findComments(post.getId(), new CommentListRequest(null, 2));

		assertThat(result.comments()).extracting(this::contentOf).containsExactly("댓글 1", "댓글 2");
		assertThat(result.nextCursor()).isEqualTo(comments.get(1).getId());
	}

	@Test
	void 다음_커서로_이어서_조회하면_중복이나_누락_없이_전체를_순회한다() {
		saveComments(5);
		flushAndClear();

		CommentListResult first = commentFinder.findComments(post.getId(), new CommentListRequest(null, 2));
		CommentListResult second = commentFinder.findComments(post.getId(),
				new CommentListRequest(first.nextCursor(), 2));
		CommentListResult third = commentFinder.findComments(post.getId(),
				new CommentListRequest(second.nextCursor(), 2));

		assertThat(first.comments()).extracting(this::contentOf).containsExactly("댓글 1", "댓글 2");
		assertThat(second.comments()).extracting(this::contentOf).containsExactly("댓글 3", "댓글 4");
		assertThat(third.comments()).extracting(this::contentOf).containsExactly("댓글 5");
		assertThat(third.nextCursor()).isNull();
	}

	@Test
	void 다른_게시글의_댓글은_반환하지_않는다() {
		saveComment(USER_ID, "이 게시글 댓글");
		Post other = savePost();
		commentRepository.save(Comment.register(other, USER_ID, "다른 게시글 댓글"));
		flushAndClear();

		CommentListResult result = commentFinder.findComments(post.getId(), new CommentListRequest(null, 20));

		assertThat(result.comments()).extracting(this::contentOf).containsExactly("이 게시글 댓글");
	}

	@Test
	void 삭제된_루트_댓글은_반환하지_않는다() {
		saveComment(USER_ID, "삭제된 댓글").delete();
		saveComment(USER_ID, "남은 댓글");
		flushAndClear();

		CommentListResult result = commentFinder.findComments(post.getId(), new CommentListRequest(null, 20));

		assertThat(result.comments()).extracting(this::contentOf).containsExactly("남은 댓글");
	}

	@Test
	void 루트_댓글마다_답글_수를_함께_반환한다() {
		Comment first = saveComment(USER_ID, "첫 댓글");
		saveReply(first, USER_ID, "첫 답글");
		saveReply(first, USER_ID, "둘째 답글");
		Comment second = saveComment(USER_ID, "둘째 댓글");
		saveReply(second, USER_ID, "답글");
		flushAndClear();

		CommentListResult result = commentFinder.findComments(post.getId(), new CommentListRequest(null, 20));

		assertThat(result.comments()).extracting(CommentResult::replyCount).containsExactly(2L, 1L);
	}

	@Test
	void 답글이_없는_루트_댓글의_답글_수는_0이다() {
		saveComment(USER_ID, "댓글");
		flushAndClear();

		CommentListResult result = commentFinder.findComments(post.getId(), new CommentListRequest(null, 20));

		assertThat(result.comments().getFirst().replyCount()).isZero();
	}

	@Test
	void 삭제된_답글은_답글_수에서_제외한다() {
		Comment root = saveComment(USER_ID, "댓글");
		saveReply(root, USER_ID, "삭제된 답글").delete();
		saveReply(root, USER_ID, "남은 답글");
		flushAndClear();

		CommentListResult result = commentFinder.findComments(post.getId(), new CommentListRequest(null, 20));

		assertThat(result.comments().getFirst().replyCount()).isEqualTo(1L);
	}

	@Test
	void 작성자_프로필_요약을_함께_반환한다() {
		User author = saveUserWithProfile("멍멍이집사", "https://cdn.example.com/profile.png");
		saveComment(author.getId(), "댓글");
		flushAndClear();

		CommentListResult result = commentFinder.findComments(post.getId(), new CommentListRequest(null, 20));

		assertThat(result.comments().getFirst().author()).isEqualTo(
				new UserProfileSummaryResult(author.getId(), "멍멍이집사", "https://cdn.example.com/profile.png"));
	}

	@Test
	void 프로필이_없는_작성자의_댓글은_author가_null이다() {
		saveComment(USER_ID, "댓글");
		flushAndClear();

		CommentListResult result = commentFinder.findComments(post.getId(), new CommentListRequest(null, 20));

		assertThat(result.comments().getFirst().author()).isNull();
	}

	@Test
	void 댓글이_없으면_빈_목록을_반환한다() {
		CommentListResult result = commentFinder.findComments(post.getId(), new CommentListRequest(null, null));

		assertThat(result.comments()).isEmpty();
		assertThat(result.nextCursor()).isNull();
	}

	@Test
	void size를_생략하면_20개를_반환한다() {
		saveComments(21);
		flushAndClear();

		CommentListResult result = commentFinder.findComments(post.getId(), new CommentListRequest(null, null));

		assertThat(result.comments()).hasSize(20);
		assertThat(result.nextCursor()).isNotNull();
	}

	@Test
	void 게시글이_없으면_예외를_던진다() {
		assertThatThrownBy(() -> commentFinder.findComments(999_999L, new CommentListRequest(null, 20)))
				.isInstanceOf(PostNotFoundException.class);
	}

	@Test
	void 삭제된_게시글의_댓글을_조회하면_예외를_던진다() {
		post.delete();
		flushAndClear();

		assertThatThrownBy(() -> commentFinder.findComments(post.getId(), new CommentListRequest(null, 20)))
				.isInstanceOf(PostNotFoundException.class);
	}

	@Test
	void size가_허용_범위를_벗어나면_예외를_던진다() {
		assertThatThrownBy(() -> commentFinder.findComments(post.getId(), new CommentListRequest(null, 51)))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@Test
	void cursor가_양수가_아니면_예외를_던진다() {
		assertThatThrownBy(() -> commentFinder.findComments(post.getId(), new CommentListRequest(0L, 20)))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@Test
	void 답글을_오래된_순으로_반환한다() {
		Comment root = saveComment(USER_ID, "루트 댓글");
		saveReply(root, USER_ID, "답글 1");
		saveReply(root, USER_ID, "답글 2");
		Comment otherRoot = saveComment(USER_ID, "다른 루트 댓글");
		saveReply(otherRoot, USER_ID, "다른 루트의 답글");
		flushAndClear();

		ReplyListResult result = commentFinder.findReplies(root.getId(), new CommentListRequest(null, 20));

		assertThat(result.replies()).extracting(this::contentOf).containsExactly("답글 1", "답글 2");
		assertThat(result.nextCursor()).isNull();
	}

	@Test
	void 답글을_다음_커서로_이어서_조회하면_중복이나_누락_없이_전체를_순회한다() {
		Comment root = saveComment(USER_ID, "루트 댓글");
		IntStream.rangeClosed(1, 3).forEach(index -> saveReply(root, USER_ID, "답글 " + index));
		flushAndClear();

		ReplyListResult first = commentFinder.findReplies(root.getId(), new CommentListRequest(null, 2));
		ReplyListResult second = commentFinder.findReplies(root.getId(), new CommentListRequest(first.nextCursor(), 2));

		assertThat(first.replies()).extracting(this::contentOf).containsExactly("답글 1", "답글 2");
		assertThat(first.nextCursor()).isNotNull();
		assertThat(second.replies()).extracting(this::contentOf).containsExactly("답글 3");
		assertThat(second.nextCursor()).isNull();
	}

	@Test
	void 삭제된_답글은_반환하지_않는다() {
		Comment root = saveComment(USER_ID, "루트 댓글");
		saveReply(root, USER_ID, "삭제된 답글").delete();
		saveReply(root, USER_ID, "남은 답글");
		flushAndClear();

		ReplyListResult result = commentFinder.findReplies(root.getId(), new CommentListRequest(null, 20));

		assertThat(result.replies()).extracting(this::contentOf).containsExactly("남은 답글");
	}

	@Test
	void 삭제된_루트_댓글의_답글도_반환한다() {
		Comment root = saveComment(USER_ID, "삭제된 루트 댓글");
		saveReply(root, USER_ID, "남은 답글");
		root.delete();
		flushAndClear();

		ReplyListResult result = commentFinder.findReplies(root.getId(), new CommentListRequest(null, 20));

		assertThat(result.replies()).extracting(this::contentOf).containsExactly("남은 답글");
	}

	@Test
	void 답글의_작성자_프로필_요약을_함께_반환한다() {
		User author = saveUserWithProfile("멍멍이집사", "https://cdn.example.com/profile.png");
		Comment root = saveComment(USER_ID, "루트 댓글");
		saveReply(root, author.getId(), "답글");
		saveReply(root, USER_ID, "프로필 없는 작성자의 답글");
		flushAndClear();

		ReplyListResult result = commentFinder.findReplies(root.getId(), new CommentListRequest(null, 20));

		assertThat(result.replies()).extracting(ReplyResult::author).containsExactly(
				new UserProfileSummaryResult(author.getId(), "멍멍이집사", "https://cdn.example.com/profile.png"),
				null);
	}

	@Test
	void 답글을_조회할_댓글이_없으면_예외를_던진다() {
		assertThatThrownBy(() -> commentFinder.findReplies(999_999L, new CommentListRequest(null, 20)))
				.isInstanceOf(CommentNotFoundException.class);
	}

	@Test
	void 답글의_답글을_조회하면_예외를_던진다() {
		Comment root = saveComment(USER_ID, "루트 댓글");
		Comment reply = saveReply(root, USER_ID, "답글");
		flushAndClear();

		assertThatThrownBy(() -> commentFinder.findReplies(reply.getId(), new CommentListRequest(null, 20)))
				.isInstanceOf(CommentNotFoundException.class);
	}

	@Test
	void 답글_목록의_size가_허용_범위를_벗어나면_예외를_던진다() {
		Comment root = saveComment(USER_ID, "루트 댓글");

		assertThatThrownBy(() -> commentFinder.findReplies(root.getId(), new CommentListRequest(null, 0)))
				.isInstanceOf(ConstraintViolationException.class);
	}

	private String contentOf(ReplyResult result) {
		return result.comment().getContent().value();
	}

	private String contentOf(CommentResult result) {
		return result.comment().getContent().value();
	}

	private Post savePost() {
		return postRepository.save(Post.register(USER_ID, new PostRegisterCommand("본문", null, List.of())));
	}

	private List<Comment> saveComments(int count) {
		return IntStream.rangeClosed(1, count)
				.mapToObj(index -> saveComment(USER_ID, "댓글 " + index))
				.toList();
	}

	private Comment saveComment(Long userId, String content) {
		Comment comment = commentRepository.save(Comment.register(post, userId, content));
		commentClosureRepository.save(CommentClosure.self(comment));
		return comment;
	}

	private Comment saveReply(Comment root, Long userId, String content) {
		Comment reply = saveComment(userId, content);
		commentClosureRepository.save(CommentClosure.reply(root, reply));
		return reply;
	}

	private User saveUserWithProfile(String nickname, String profileImageUrl) {
		User user = User.registerUser();
		user.completeOnboarding(new UserProfileCreateCommand(nickname, profileImageUrl, "INFP", Set.of(), Set.of()));
		return userRepository.save(user);
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
