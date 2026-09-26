package com.daesabu.meongcoach.training.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;

import com.daesabu.meongcoach.training.application.provided.CardCommentCreator;
import com.daesabu.meongcoach.training.application.provided.CardCommentFinder;
import com.daesabu.meongcoach.training.application.provided.CardCommentLikeFinder;
import com.daesabu.meongcoach.training.application.provided.CardCommentLikeUpdater;
import com.daesabu.meongcoach.training.application.provided.CommentLikeResult;
import com.daesabu.meongcoach.training.application.provided.CommentResult;
import com.daesabu.meongcoach.training.application.provided.LikeStateResult;
import com.daesabu.meongcoach.training.application.provided.ReplyResult;
import com.daesabu.meongcoach.training.application.provided.dto.CommentCreateRequest;
import com.daesabu.meongcoach.training.domain.Card;
import com.daesabu.meongcoach.training.domain.CardCreateCommand;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumCreateCommand;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.LessonCreateCommand;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicCreateCommand;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.comment.CardComment;
import com.daesabu.meongcoach.training.domain.exception.CardNotFoundException;
import com.daesabu.meongcoach.training.domain.exception.CommentNotFoundException;
import com.daesabu.meongcoach.training.domain.exception.InvalidCardCommentContentException;
import com.daesabu.meongcoach.user.application.provided.UserProfileFinder;
import java.util.List;
import java.util.Map;
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

	@Autowired
	private CardCommentFinder finder;

	@Autowired
	private CardCommentCreator creator;

	@Autowired
	private CardCommentLikeUpdater likeUpdater;

	@Autowired
	private CardCommentLikeFinder likeFinder;

	@Autowired
	private TestEntityManager entityManager;

	@MockitoBean
	private UserProfileFinder userProfileFinder;

	@BeforeEach
	void stubNicknames() {
		given(userProfileFinder.findNicknames(anySet())).willReturn(Map.of(42L, "멍멍이집사", 43L, "두부집사"));
	}

	@Test
	void 부모_댓글과_답글을_각각_조회하고_작성자를_확인한다() {
		Card card = persistCard();
		CommentResult comment = creator.createComment(42L, card.getId(), new CommentCreateRequest("  댓글  "));
		creator.createReply(43L, comment.id(), new CommentCreateRequest("  답글  "));
		entityManager.flush();
		entityManager.clear();

		List<CommentResult> comments = finder.findComments(card.getId());
		List<ReplyResult> replies = finder.findReplies(comment.id());

		assertThat(comments).hasSize(1);
		CommentResult found = comments.getFirst();
		assertThat(found.content()).isEqualTo("댓글");
		assertThat(found.authorNickname()).isEqualTo("멍멍이집사");
		assertThat(found.replyCount()).isEqualTo(1);
		assertThat(replies).hasSize(1);
		assertThat(replies.getFirst().authorNickname()).isEqualTo("두부집사");
		assertThat(found.createdAt()).isNotNull();
	}

	@Test
	void 작성자의_프로필이_없으면_닉네임을_null로_반환한다() {
		Card card = persistCard();
		CommentResult created = creator.createComment(44L, card.getId(), new CommentCreateRequest("댓글"));

		List<CommentResult> comments = finder.findComments(card.getId());

		assertThat(created.authorNickname()).isNull();
		assertThat(comments.getFirst().authorNickname()).isNull();
	}

	@Test
	void 카드에서_좋아요가_있는_댓글과_답글_상태만_조회한다() {
		Card card = persistCard();
		Card otherCard = persistCard();
		CommentResult popular = creator.createComment(42L, card.getId(), new CommentCreateRequest("좋아요 둘"));
		CommentResult likedByOther = creator.createComment(42L, card.getId(), new CommentCreateRequest("다른 사용자 좋아요"));
		ReplyResult likedReply = creator.createReply(43L, popular.id(), new CommentCreateRequest("답글 좋아요"));
		creator.createReply(43L, popular.id(), new CommentCreateRequest("좋아요 없음"));
		CommentResult otherComment = creator.createComment(42L, otherCard.getId(), new CommentCreateRequest("다른 카드"));
		likeUpdater.like(42L, popular.id());
		likeUpdater.like(43L, popular.id());
		likeUpdater.like(43L, likedByOther.id());
		likeUpdater.like(43L, likedReply.id());
		likeUpdater.like(42L, otherComment.id());
		entityManager.flush();
		entityManager.clear();

		List<CommentLikeResult> forUser42 = likeFinder.findLikeStates(42L, card.getId());
		List<CommentLikeResult> forUser43 = likeFinder.findLikeStates(43L, card.getId());

		assertThat(forUser42).containsExactly(
				new CommentLikeResult(popular.id(), 2, true),
				new CommentLikeResult(likedByOther.id(), 1, false),
				new CommentLikeResult(likedReply.id(), 1, false));
		assertThat(forUser43).containsExactly(
				new CommentLikeResult(popular.id(), 2, true),
				new CommentLikeResult(likedByOther.id(), 1, true),
				new CommentLikeResult(likedReply.id(), 1, true));
		assertThatThrownBy(() -> likeFinder.findLikeStates(42L, -1L))
				.isInstanceOf(CardNotFoundException.class);
	}

	@Test
	void 부모_댓글과_답글을_각각_정렬하고_답글_수를_반환한다() {
		Card card = persistCard();
		CommentResult oldest = creator.createComment(42L, card.getId(), new CommentCreateRequest("첫째"));
		CommentResult middle = creator.createComment(42L, card.getId(), new CommentCreateRequest("둘째"));
		ReplyResult firstReply = creator.createReply(43L, middle.id(), new CommentCreateRequest("첫째 답글"));
		ReplyResult secondReply = creator.createReply(42L, middle.id(), new CommentCreateRequest("둘째 답글"));
		ReplyResult oldestReply = creator.createReply(43L, oldest.id(), new CommentCreateRequest("다른 댓글의 답글"));
		CommentResult newest = creator.createComment(42L, card.getId(), new CommentCreateRequest("셋째"));
		entityManager.flush();
		entityManager.clear();

		List<CommentResult> comments = finder.findComments(card.getId());
		List<ReplyResult> middleReplies = finder.findReplies(middle.id());
		List<ReplyResult> oldestReplies = finder.findReplies(oldest.id());

		assertThat(comments).extracting(CommentResult::id)
				.containsExactly(newest.id(), middle.id(), oldest.id());
		assertThat(comments).extracting(CommentResult::replyCount)
				.containsExactly(0L, 2L, 1L);
		assertThat(middleReplies).extracting(ReplyResult::id)
				.containsExactly(firstReply.id(), secondReply.id());
		assertThat(oldestReplies).extracting(ReplyResult::id)
				.containsExactly(oldestReply.id());
		assertThat(finder.findReplies(newest.id())).isEmpty();
	}

	@Test
	void 카드의_전체_댓글_수는_답글을_포함해_별도로_조회한다() {
		Card card = persistCard();
		assertThat(finder.countComments(card.getId())).isZero();
		CommentResult comment = creator.createComment(42L, card.getId(), new CommentCreateRequest("댓글"));
		creator.createReply(43L, comment.id(), new CommentCreateRequest("답글"));

		assertThat(finder.countComments(card.getId())).isEqualTo(2);
		assertThatThrownBy(() -> finder.countComments(-1L))
				.isInstanceOf(CardNotFoundException.class);
	}

	@Test
	void 답글_목록은_부모_댓글_ID만_받는다() {
		Card card = persistCard();
		CommentResult parent = creator.createComment(42L, card.getId(), new CommentCreateRequest("댓글"));
		ReplyResult reply = creator.createReply(43L, parent.id(), new CommentCreateRequest("답글"));

		assertThatThrownBy(() -> finder.findReplies(-1L))
				.isInstanceOf(CommentNotFoundException.class);
		assertThatThrownBy(() -> finder.findReplies(reply.id()))
				.isInstanceOf(CommentNotFoundException.class);
	}

	@Test
	void 답글에_답글을_달면_최상위_댓글에_연결한다() {
		Card card = persistCard();
		CommentResult parent = creator.createComment(42L, card.getId(), new CommentCreateRequest("댓글"));
		ReplyResult first = creator.createReply(43L, parent.id(), new CommentCreateRequest("첫째 답글"));

		ReplyResult second = creator.createReply(42L, first.id(), new CommentCreateRequest("둘째 답글"));
		CardComment saved = entityManager.find(CardComment.class, second.id());

		assertThat(saved.getParentId()).isEqualTo(parent.id());
		assertThat(saved.getCardId()).isEqualTo(card.getId());
	}

	@Test
	void 좋아요_추가와_취소는_여러_번_호출해도_같은_상태를_반환한다() {
		Card card = persistCard();
		CommentResult comment = creator.createComment(42L, card.getId(), new CommentCreateRequest("댓글"));

		LikeStateResult first = likeUpdater.like(42L, comment.id());
		LikeStateResult repeated = likeUpdater.like(42L, comment.id());
		LikeStateResult removed = likeUpdater.unlike(42L, comment.id());
		LikeStateResult repeatedRemoval = likeUpdater.unlike(42L, comment.id());

		assertThat(first).isEqualTo(new LikeStateResult(1, true));
		assertThat(repeated).isEqualTo(first);
		assertThat(removed).isEqualTo(new LikeStateResult(0, false));
		assertThat(repeatedRemoval).isEqualTo(removed);
	}

	@Test
	void 없는_카드와_댓글은_각각_404_예외를_던진다() {
		assertThatThrownBy(() -> creator.createComment(42L, -1L, new CommentCreateRequest("댓글")))
				.isInstanceOf(CardNotFoundException.class);
		assertThatThrownBy(() -> creator.createReply(42L, -1L, new CommentCreateRequest("답글")))
				.isInstanceOf(CommentNotFoundException.class);
		assertThatThrownBy(() -> likeUpdater.like(42L, -1L))
				.isInstanceOf(CommentNotFoundException.class);
	}

	@Test
	void 공백으로만_이루어진_본문은_저장하지_않는다() {
		Card card = persistCard();

		assertThatThrownBy(() -> creator.createComment(42L, card.getId(), new CommentCreateRequest("   ")))
				.isInstanceOf(InvalidCardCommentContentException.class);
		assertThat(finder.findComments(card.getId())).isEmpty();
	}

	private Card persistCard() {
		TrainingCategory category = entityManager.persist(TrainingCategory.create("카테고리", 1, null, null));
		Topic topic = entityManager.persist(Topic.create(category, new TopicCreateCommand("토픽", 1, null, null, null)));
		Curriculum curriculum = entityManager.persist(Curriculum.create(topic,
				new CurriculumCreateCommand("커리큘럼", 1, null, null)));
		Lesson lesson = entityManager.persist(Lesson.create(curriculum, new LessonCreateCommand("레슨", 1, 5)));
		return entityManager.persist(Card.create(lesson, new CardCreateCommand("카드", 1, "지시문")));
	}
}
