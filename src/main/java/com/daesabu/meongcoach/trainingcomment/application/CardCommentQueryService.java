package com.daesabu.meongcoach.trainingcomment.application;

import com.daesabu.meongcoach.training.application.provided.CardFinder;
import com.daesabu.meongcoach.trainingcomment.application.provided.CardCommentCountResult;
import com.daesabu.meongcoach.trainingcomment.application.provided.CardCommentFinder;
import com.daesabu.meongcoach.trainingcomment.application.provided.CommentPageResult;
import com.daesabu.meongcoach.trainingcomment.application.provided.CommentResult;
import com.daesabu.meongcoach.trainingcomment.application.required.CardCommentRepository;
import com.daesabu.meongcoach.trainingcomment.application.required.CardCommentSummary;
import com.daesabu.meongcoach.trainingcomment.domain.CardComment;
import com.daesabu.meongcoach.trainingcomment.domain.exception.CardNotFoundException;
import com.daesabu.meongcoach.trainingcomment.domain.exception.CommentNotFoundException;
import com.daesabu.meongcoach.user.application.provided.UserProfileFinder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글 조회 서비스. 집계가 끝난 한 페이지를 리포지토리에서 받아 작성자 닉네임만 user 모듈에서 합친다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardCommentQueryService implements CardCommentFinder {

	// 페이지 크기는 클라이언트가 정하지 않는다
	private static final Pageable PAGE = PageRequest.ofSize(20);

	private final CardCommentRepository commentRepository;

	private final CardFinder cardFinder;

	private final UserProfileFinder userProfileFinder;

	@Override
	public CommentPageResult findComments(Long userId, Long cardId, Long cursor) {
		if (!cardFinder.existsCard(cardId)) {
			throw new CardNotFoundException(cardId);
		}
		Slice<CardCommentSummary> slice = commentRepository.findTopLevel(cardId, userId, cursor, PAGE);
		long totalCount = commentRepository.countByCardId(cardId);
		return toPage(slice, totalCount);
	}

	@Override
	public CommentPageResult findReplies(Long userId, Long commentId, Long cursor) {
		CardComment root = commentRepository.getComment(commentId);
		if (root.isReply()) {
			throw new CommentNotFoundException(commentId);
		}
		Slice<CardCommentSummary> slice = commentRepository.findReplies(commentId, userId, cursor, PAGE);
		long totalCount = commentRepository.countByParentId(commentId);
		return toPage(slice, totalCount);
	}

	@Override
	public List<CardCommentCountResult> countComments(Set<Long> cardIds) {
		return commentRepository.countByCardIds(cardIds);
	}

	private CommentPageResult toPage(Slice<CardCommentSummary> slice, long totalCount) {
		List<CommentResult> comments = toResults(slice.getContent());
		Long nextCursor = nextCursor(slice);
		return new CommentPageResult(comments, totalCount, nextCursor);
	}

	// 다음 페이지가 있으면 마지막으로 내려준 항목의 id가 다음 커서다
	private Long nextCursor(Slice<CardCommentSummary> slice) {
		if (!slice.hasNext()) {
			return null;
		}
		return slice.getContent().getLast().id();
	}

	private List<CommentResult> toResults(List<CardCommentSummary> rows) {
		Map<Long, String> nicknames = findNicknames(rows);
		return rows.stream()
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
