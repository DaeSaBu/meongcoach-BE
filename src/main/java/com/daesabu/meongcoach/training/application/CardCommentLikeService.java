package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.training.application.provided.CardCommentLikeFinder;
import com.daesabu.meongcoach.training.application.provided.CardCommentLikeUpdater;
import com.daesabu.meongcoach.training.application.provided.CommentLikeResult;
import com.daesabu.meongcoach.training.application.provided.LikeStateResult;
import com.daesabu.meongcoach.training.application.required.CardCommentLikeRepository;
import com.daesabu.meongcoach.training.application.required.CardCommentRepository;
import com.daesabu.meongcoach.training.application.required.CardRepository;
import com.daesabu.meongcoach.training.domain.comment.CardCommentLike;
import com.daesabu.meongcoach.training.domain.exception.CardNotFoundException;
import com.daesabu.meongcoach.training.domain.exception.CommentNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardCommentLikeService implements CardCommentLikeUpdater, CardCommentLikeFinder {

	private final CardRepository cardRepository;

	private final CardCommentRepository commentRepository;

	private final CardCommentLikeRepository likeRepository;

	@Override
	public List<CommentLikeResult> findLikeStates(Long userId, Long cardId) {
		if (!cardRepository.existsById(cardId)) {
			throw new CardNotFoundException(cardId);
		}
		List<CardCommentLike> likes = likeRepository.findAllByCardIdOrderByCommentIdAsc(cardId);
		Map<Long, List<CardCommentLike>> likesByCommentId = likes.stream()
				.collect(Collectors.groupingBy(CardCommentLike::getCommentId, TreeMap::new, Collectors.toList()));
		List<CommentLikeResult> results = likesByCommentId.values().stream()
				.map(commentLikes -> CommentLikeResult.from(commentLikes, userId))
				.toList();
		return results;
	}

	@Override
	@Transactional
	public LikeStateResult like(Long userId, Long commentId) {
		verifyComment(commentId);
		likeRepository.like(commentId, userId);
		long likeCount = likeRepository.countByCommentId(commentId);
		return new LikeStateResult(likeCount, true);
	}

	@Override
	@Transactional
	public LikeStateResult unlike(Long userId, Long commentId) {
		verifyComment(commentId);
		likeRepository.unlike(commentId, userId);
		long likeCount = likeRepository.countByCommentId(commentId);
		return new LikeStateResult(likeCount, false);
	}

	private void verifyComment(Long commentId) {
		if (!commentRepository.existsById(commentId)) {
			throw new CommentNotFoundException(commentId);
		}
	}
}
