package com.daesabu.meongcoach.comment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;

import com.daesabu.meongcoach.comment.application.provided.CardCommentCountResult;
import com.daesabu.meongcoach.comment.application.provided.CardCommentCreator;
import com.daesabu.meongcoach.comment.application.provided.CardCommentFinder;
import com.daesabu.meongcoach.comment.application.provided.CardCommentLiker;
import com.daesabu.meongcoach.comment.application.provided.CommentPageResult;
import com.daesabu.meongcoach.comment.application.provided.CommentResult;
import com.daesabu.meongcoach.comment.application.provided.LikeStateResult;
import com.daesabu.meongcoach.comment.application.provided.dto.CommentCreateRequest;
import com.daesabu.meongcoach.comment.domain.CardComment;
import com.daesabu.meongcoach.comment.domain.exception.CardNotFoundException;
import com.daesabu.meongcoach.comment.domain.exception.CommentNotFoundException;
import com.daesabu.meongcoach.comment.domain.exception.InvalidCommentContentException;
import com.daesabu.meongcoach.training.application.provided.CardFinder;
import com.daesabu.meongcoach.user.application.provided.UserProfileFinder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
@Import({CardCommentQueryService.class, CardCommentCreateService.class, CardCommentLikeService.class})
class CardCommentServiceTest {

	private static final Long CARD_ID = 7L;

	private static final Long OTHER_CARD_ID = 8L;

	private static final Long EMPTY_CARD_ID = 9L;

	private static final Long ABSENT_CARD_ID = -1L;

	private static final Long ABSENT_COMMENT_ID = -1L;

	private static final Long USER_ID = 42L;

	private static final Long OTHER_USER_ID = 43L;

	private static final Long USER_WITHOUT_PROFILE_ID = 44L;

	private static final int PAGE_SIZE = 20;

	@Autowired
	private CardCommentFinder finder;

	@Autowired
	private CardCommentCreator creator;

	@Autowired
	private CardCommentLiker liker;

	@Autowired
	private TestEntityManager entityManager;

	@MockitoBean
	private CardFinder cardFinder;

	@MockitoBean
	private UserProfileFinder userProfileFinder;

	@BeforeEach
	void stubCollaborators() {
		given(cardFinder.existsCard(anyLong())).willReturn(true);
		given(cardFinder.existsCard(ABSENT_CARD_ID)).willReturn(false);
		given(userProfileFinder.findNicknames(anySet()))
				.willReturn(Map.of(USER_ID, "멍멍이집사", OTHER_USER_ID, "두부집사"));
	}

	@Test
	void 최상위_댓글은_최신순으로_20건씩_조회하고_커서로_이어진다() {
		List<CommentResult> created = createComments(CARD_ID, PAGE_SIZE + 1);
		createComment(USER_ID, OTHER_CARD_ID, "다른 카드");
		flushAndClear();

		CommentPageResult firstPage = finder.findComments(USER_ID, CARD_ID, null);
		CommentPageResult secondPage = finder.findComments(USER_ID, CARD_ID, firstPage.nextCursor());

		List<Long> newestFirst = created.reversed().stream().map(CommentResult::id).toList();
		assertThat(firstPage.comments()).extracting(CommentResult::id).containsExactlyElementsOf(newestFirst.subList(0, PAGE_SIZE));
		assertThat(firstPage.nextCursor()).isEqualTo(newestFirst.get(PAGE_SIZE - 1));
		assertThat(firstPage.totalCount()).isEqualTo(PAGE_SIZE + 1);
		assertThat(secondPage.comments()).extracting(CommentResult::id).containsExactly(newestFirst.getLast());
		assertThat(secondPage.nextCursor()).isNull();
	}

	@Test
	void 댓글마다_답글_수_좋아요_수_내_좋아요_여부_닉네임을_함께_내린다() {
		CommentResult popular = createComment(USER_ID, CARD_ID, "좋아요 둘");
		CommentResult likedByOther = createComment(USER_ID, CARD_ID, "다른 사용자 좋아요");
		CommentResult reply = createReply(OTHER_USER_ID, popular.id(), "답글");
		liker.like(USER_ID, popular.id());
		liker.like(OTHER_USER_ID, popular.id());
		liker.like(OTHER_USER_ID, likedByOther.id());
		liker.like(OTHER_USER_ID, reply.id());
		flushAndClear();

		CommentPageResult forUser = finder.findComments(USER_ID, CARD_ID, null);
		CommentPageResult forOther = finder.findComments(OTHER_USER_ID, CARD_ID, null);
		CommentPageResult replies = finder.findReplies(USER_ID, popular.id(), null);

		assertThat(forUser.totalCount()).isEqualTo(3);
		assertThat(forUser.comments()).containsExactly(
				new CommentResult(likedByOther.id(), null, USER_ID, "멍멍이집사", "다른 사용자 좋아요",
						likedByOther.createdAt(), 0, 1, false),
				new CommentResult(popular.id(), null, USER_ID, "멍멍이집사", "좋아요 둘", popular.createdAt(), 1, 2, true));
		assertThat(forOther.comments()).extracting(CommentResult::likedByMe).containsExactly(true, true);
		assertThat(replies.totalCount()).isEqualTo(1);
		assertThat(replies.comments()).containsExactly(
				new CommentResult(reply.id(), popular.id(), OTHER_USER_ID, "두부집사", "답글", reply.createdAt(), 0, 1, false));
	}

	@Test
	void 답글은_오래된_순으로_20건씩_조회하고_스레드_답글_수를_함께_내린다() {
		CommentResult root = createComment(USER_ID, CARD_ID, "댓글");
		List<CommentResult> created = createReplies(root.id(), PAGE_SIZE + 1);
		flushAndClear();

		CommentPageResult firstPage = finder.findReplies(USER_ID, root.id(), null);
		CommentPageResult secondPage = finder.findReplies(USER_ID, root.id(), firstPage.nextCursor());

		List<Long> oldestFirst = created.stream().map(CommentResult::id).toList();
		assertThat(firstPage.comments()).extracting(CommentResult::id).containsExactlyElementsOf(oldestFirst.subList(0, PAGE_SIZE));
		assertThat(firstPage.nextCursor()).isEqualTo(oldestFirst.get(PAGE_SIZE - 1));
		assertThat(firstPage.totalCount()).isEqualTo(PAGE_SIZE + 1);
		assertThat(secondPage.comments()).extracting(CommentResult::id).containsExactly(oldestFirst.getLast());
		assertThat(secondPage.nextCursor()).isNull();
	}

	@Test
	void 답글에_답글을_달면_같은_스레드에_붙고_답한_대상을_남긴다() {
		CommentResult root = createComment(USER_ID, CARD_ID, "댓글");
		CommentResult first = createReply(OTHER_USER_ID, root.id(), "첫째 답글");

		CommentResult second = createReply(USER_ID, first.id(), "둘째 답글");
		flushAndClear();

		CardComment saved = entityManager.find(CardComment.class, second.id());
		assertThat(saved.getRootId()).isEqualTo(root.id());
		assertThat(saved.getParentId()).isEqualTo(first.id());
		assertThat(saved.getCardId()).isEqualTo(CARD_ID);
		assertThat(second.parentId()).isEqualTo(first.id());
		CommentPageResult replies = finder.findReplies(USER_ID, root.id(), null);
		assertThat(replies.comments()).extracting(CommentResult::parentId).containsExactly(root.id(), first.id());
		assertThat(finder.findComments(USER_ID, CARD_ID, null).comments())
				.extracting(CommentResult::replyCount).containsExactly(2L);
	}

	@Test
	void 답글_목록은_최상위_댓글_ID만_받는다() {
		CommentResult root = createComment(USER_ID, CARD_ID, "댓글");
		CommentResult reply = createReply(OTHER_USER_ID, root.id(), "답글");

		assertThatThrownBy(() -> finder.findReplies(USER_ID, reply.id(), null))
				.isInstanceOf(CommentNotFoundException.class);
		assertThatThrownBy(() -> finder.findReplies(USER_ID, ABSENT_COMMENT_ID, null))
				.isInstanceOf(CommentNotFoundException.class);
	}

	@Test
	void 작성_응답은_닉네임과_비어_있는_집계를_담는다() {
		CommentResult comment = createComment(USER_ID, CARD_ID, "  댓글  ");
		CommentResult reply = createReply(OTHER_USER_ID, comment.id(), "  답글  ");

		assertThat(comment.content()).isEqualTo("댓글");
		assertThat(comment.parentId()).isNull();
		assertThat(comment.authorId()).isEqualTo(USER_ID);
		assertThat(comment.authorNickname()).isEqualTo("멍멍이집사");
		assertThat(comment.createdAt()).isNotNull();
		assertThat(comment.replyCount()).isZero();
		assertThat(comment.likeCount()).isZero();
		assertThat(comment.likedByMe()).isFalse();
		assertThat(reply.parentId()).isEqualTo(comment.id());
		assertThat(reply.authorNickname()).isEqualTo("두부집사");
	}

	@Test
	void 작성자의_프로필이_없으면_닉네임을_null로_반환한다() {
		CommentResult created = createComment(USER_WITHOUT_PROFILE_ID, CARD_ID, "댓글");
		flushAndClear();

		CommentPageResult page = finder.findComments(USER_ID, CARD_ID, null);

		assertThat(created.authorNickname()).isNull();
		assertThat(page.comments().getFirst().authorNickname()).isNull();
	}

	@Test
	void 좋아요_추가와_취소는_여러_번_호출해도_같은_상태를_반환한다() {
		CommentResult comment = createComment(USER_ID, CARD_ID, "댓글");

		LikeStateResult first = liker.like(USER_ID, comment.id());
		LikeStateResult repeated = liker.like(USER_ID, comment.id());
		LikeStateResult removed = liker.unlike(USER_ID, comment.id());
		LikeStateResult repeatedRemoval = liker.unlike(USER_ID, comment.id());

		assertThat(first).isEqualTo(new LikeStateResult(1, true));
		assertThat(repeated).isEqualTo(first);
		assertThat(removed).isEqualTo(new LikeStateResult(0, false));
		assertThat(repeatedRemoval).isEqualTo(removed);
	}

	@Test
	void 카드별_댓글_수는_답글을_포함하고_댓글이_없는_카드는_빼고_돌려준다() {
		CommentResult comment = createComment(USER_ID, CARD_ID, "댓글");
		createReply(OTHER_USER_ID, comment.id(), "답글");
		createComment(USER_ID, OTHER_CARD_ID, "다른 카드");
		flushAndClear();

		List<CardCommentCountResult> counts = finder.countComments(Set.of(CARD_ID, OTHER_CARD_ID, EMPTY_CARD_ID));

		assertThat(counts).containsExactlyInAnyOrder(
				new CardCommentCountResult(CARD_ID, 2),
				new CardCommentCountResult(OTHER_CARD_ID, 1));
	}

	@Test
	void 없는_카드와_댓글은_각각_404_예외를_던진다() {
		assertThatThrownBy(() -> creator.createComment(USER_ID, ABSENT_CARD_ID, new CommentCreateRequest("댓글")))
				.isInstanceOf(CardNotFoundException.class);
		assertThatThrownBy(() -> finder.findComments(USER_ID, ABSENT_CARD_ID, null))
				.isInstanceOf(CardNotFoundException.class);
		assertThatThrownBy(() -> creator.createReply(USER_ID, ABSENT_COMMENT_ID, new CommentCreateRequest("답글")))
				.isInstanceOf(CommentNotFoundException.class);
		assertThatThrownBy(() -> liker.like(USER_ID, ABSENT_COMMENT_ID))
				.isInstanceOf(CommentNotFoundException.class);
		assertThatThrownBy(() -> liker.unlike(USER_ID, ABSENT_COMMENT_ID))
				.isInstanceOf(CommentNotFoundException.class);
	}

	@Test
	void 공백으로만_이루어진_본문은_저장하지_않는다() {
		assertThatThrownBy(() -> creator.createComment(USER_ID, CARD_ID, new CommentCreateRequest("   ")))
				.isInstanceOf(InvalidCommentContentException.class);

		CommentPageResult page = finder.findComments(USER_ID, CARD_ID, null);
		assertThat(page.comments()).isEmpty();
		assertThat(page.totalCount()).isZero();
	}

	private CommentResult createComment(Long userId, Long cardId, String content) {
		return creator.createComment(userId, cardId, new CommentCreateRequest(content));
	}

	private CommentResult createReply(Long userId, Long commentId, String content) {
		return creator.createReply(userId, commentId, new CommentCreateRequest(content));
	}

	// 작성 순서대로 담는다
	private List<CommentResult> createComments(Long cardId, int count) {
		List<CommentResult> created = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			created.add(createComment(USER_ID, cardId, "댓글 " + i));
		}
		return created;
	}

	private List<CommentResult> createReplies(Long rootId, int count) {
		List<CommentResult> created = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			created.add(createReply(OTHER_USER_ID, rootId, "답글 " + i));
		}
		return created;
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
