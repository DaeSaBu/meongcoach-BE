package com.daesabu.meongcoach.comment.application;

import com.daesabu.meongcoach.comment.application.provided.CardCommentLiker;
import com.daesabu.meongcoach.comment.application.provided.LikeStateResult;
import com.daesabu.meongcoach.comment.application.required.CardCommentLikeRepository;
import com.daesabu.meongcoach.comment.application.required.CardCommentRepository;
import com.daesabu.meongcoach.comment.domain.CardCommentLike;
import com.daesabu.meongcoach.comment.domain.CardCommentLikeId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글 좋아요 서비스. 댓글과 사용자의 조합이 식별자라 있으면 두지 않고 없으면 만드는 것으로 멱등을 보장한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardCommentLikeService implements CardCommentLiker {

	private final CardCommentRepository commentRepository;

	private final CardCommentLikeRepository likeRepository;

	@Override
	@Transactional
	public LikeStateResult like(Long userId, Long commentId) {
		// 없는 댓글이면 여기서 CommentNotFoundException이 난다
		commentRepository.getComment(commentId);
		CardCommentLikeId id = new CardCommentLikeId(commentId, userId);
		if (!likeRepository.existsById(id)) {
			likeRepository.save(CardCommentLike.create(commentId, userId));
		}
		return state(commentId, true);
	}

	@Override
	@Transactional
	public LikeStateResult unlike(Long userId, Long commentId) {
		commentRepository.getComment(commentId);
		CardCommentLikeId id = new CardCommentLikeId(commentId, userId);
		likeRepository.findById(id).ifPresent(likeRepository::delete);
		return state(commentId, false);
	}

	private LikeStateResult state(Long commentId, boolean likedByMe) {
		long likeCount = likeRepository.countByCommentId(commentId);
		return new LikeStateResult(likeCount, likedByMe);
	}
}
