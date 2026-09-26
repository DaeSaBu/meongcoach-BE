package com.daesabu.meongcoach.comment.application;

import com.daesabu.meongcoach.comment.application.provided.CardCommentCountResult;
import com.daesabu.meongcoach.comment.application.provided.CardCommentFinder;
import com.daesabu.meongcoach.comment.application.provided.CommentPageResult;
import com.daesabu.meongcoach.comment.application.provided.CommentResult;
import com.daesabu.meongcoach.comment.application.provided.ReplyPageResult;
import com.daesabu.meongcoach.comment.application.required.CardCommentRepository;
import com.daesabu.meongcoach.comment.application.required.CardCommentSummary;
import com.daesabu.meongcoach.comment.domain.CardComment;
import com.daesabu.meongcoach.comment.domain.exception.CardNotFoundException;
import com.daesabu.meongcoach.comment.domain.exception.CommentNotFoundException;
import com.daesabu.meongcoach.training.application.provided.CardFinder;
import com.daesabu.meongcoach.user.application.provided.UserProfileFinder;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글 조회 서비스. 집계가 끝난 한 페이지를 리포지토리에서 받아 작성자 닉네임만 user 모듈에서 합친다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardCommentQueryService implements CardCommentFinder {

	private static final int PAGE_SIZE_MIN = 1;

	private static final int PAGE_SIZE_MAX = 50;

	private final CardCommentRepository commentRepository;

	private final CardFinder cardFinder;

	private final UserProfileFinder userProfileFinder;

	@Override
	public CommentPageResult findComments(Long userId, Long cardId, Long cursor, int size) {
		if (!cardFinder.existsCard(cardId)) {
			throw new CardNotFoundException(cardId);
		}
		Long beforeId = Objects.requireNonNullElse(cursor, Long.MAX_VALUE);
		int pageSize = clampPageSize(size);
		List<CardCommentSummary> rows = commentRepository.findTopLevelBefore(cardId, userId, beforeId,
				Limit.of(pageSize + 1));
		List<CommentResult> comments = toResults(rows, pageSize);
		Long nextCursor = nextCursor(rows, pageSize);
		long totalCount = commentRepository.countByCardId(cardId);
		return new CommentPageResult(comments, totalCount, nextCursor);
	}

	@Override
	public ReplyPageResult findReplies(Long userId, Long commentId, Long cursor, int size) {
		CardComment root = commentRepository.getComment(commentId);
		if (root.isReply()) {
			throw new CommentNotFoundException(commentId);
		}
		Long afterId = Objects.requireNonNullElse(cursor, 0L);
		int pageSize = clampPageSize(size);
		List<CardCommentSummary> rows = commentRepository.findRepliesAfter(commentId, userId, afterId,
				Limit.of(pageSize + 1));
		List<CommentResult> replies = toResults(rows, pageSize);
		Long nextCursor = nextCursor(rows, pageSize);
		return new ReplyPageResult(replies, nextCursor);
	}

	@Override
	public List<CardCommentCountResult> countComments(Set<Long> cardIds) {
		return commentRepository.countByCardIds(cardIds);
	}

	private int clampPageSize(int size) {
		return Math.clamp(size, PAGE_SIZE_MIN, PAGE_SIZE_MAX);
	}

	// 페이지 크기보다 한 건 더 읽었으면 다음 페이지가 있고, 마지막으로 내려준 항목의 id가 다음 커서다
	private Long nextCursor(List<CardCommentSummary> rows, int pageSize) {
		if (rows.size() <= pageSize) {
			return null;
		}
		return rows.get(pageSize - 1).id();
	}

	private List<CommentResult> toResults(List<CardCommentSummary> rows, int pageSize) {
		List<CardCommentSummary> page = rows.stream().limit(pageSize).toList();
		Map<Long, String> nicknames = findNicknames(page);
		return page.stream()
				.map(row -> toResult(row, nicknames.get(row.authorId())))
				.toList();
	}

	private Map<Long, String> findNicknames(List<CardCommentSummary> rows) {
		Set<Long> authorIds = rows.stream()
				.map(CardCommentSummary::authorId)
				.collect(Collectors.toSet());
		return userProfileFinder.findNicknames(authorIds);
	}

	private CommentResult toResult(CardCommentSummary row, String authorNickname) {
		return new CommentResult(row.id(), row.parentId(), row.authorId(), authorNickname, row.content(),
				row.createdAt(), row.replyCount(), row.likeCount(), row.likedByMe());
	}
}
