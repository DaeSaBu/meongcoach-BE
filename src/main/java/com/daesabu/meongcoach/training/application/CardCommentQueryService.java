package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.training.application.provided.CardCommentFinder;
import com.daesabu.meongcoach.training.application.provided.CommentResult;
import com.daesabu.meongcoach.training.application.provided.ReplyResult;
import com.daesabu.meongcoach.training.application.required.CardCommentRepository;
import com.daesabu.meongcoach.training.application.required.CardRepository;
import com.daesabu.meongcoach.training.domain.comment.CardComment;
import com.daesabu.meongcoach.training.domain.exception.CardNotFoundException;
import com.daesabu.meongcoach.training.domain.exception.CommentNotFoundException;
import com.daesabu.meongcoach.user.application.provided.UserProfileFinder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardCommentQueryService implements CardCommentFinder {

	private final CardRepository cardRepository;

	private final CardCommentRepository commentRepository;

	private final UserProfileFinder userProfileFinder;

	@Override
	public List<CommentResult> findComments(Long cardId) {
		verifyCard(cardId);
		List<CardComment> parents = commentRepository.findByCardIdAndParentIdIsNullOrderByIdDesc(cardId);
		List<CardComment> replies = commentRepository.findByCardIdAndParentIdIsNotNull(cardId);
		Map<Long, Long> replyCounts = replies.stream()
				.collect(Collectors.groupingBy(CardComment::getParentId, Collectors.counting()));
		Map<Long, String> nicknames = findNicknames(parents);
		List<CommentResult> results = parents.stream()
				.map(parent -> CommentResult.from(parent, nicknames.get(parent.getUserId()),
						replyCounts.getOrDefault(parent.getId(), 0L)))
				.toList();
		return results;
	}

	@Override
	public long countComments(Long cardId) {
		verifyCard(cardId);
		return commentRepository.countByCardId(cardId);
	}

	@Override
	public List<ReplyResult> findReplies(Long parentId) {
		if (!commentRepository.existsByIdAndParentIdIsNull(parentId)) {
			throw new CommentNotFoundException(parentId);
		}
		List<CardComment> replies = commentRepository.findByParentIdOrderByIdAsc(parentId);
		Map<Long, String> nicknames = findNicknames(replies);
		List<ReplyResult> results = replies.stream()
				.map(reply -> ReplyResult.from(reply, nicknames.get(reply.getUserId())))
				.toList();
		return results;
	}

	private void verifyCard(Long cardId) {
		if (!cardRepository.existsById(cardId)) {
			throw new CardNotFoundException(cardId);
		}
	}

	private Map<Long, String> findNicknames(List<CardComment> comments) {
		Set<Long> authorIds = comments.stream()
				.map(CardComment::getUserId)
				.collect(Collectors.toSet());
		return userProfileFinder.findNicknames(authorIds);
	}
}
