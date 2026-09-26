package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.training.application.provided.CardCommentCreator;
import com.daesabu.meongcoach.training.application.provided.CommentResult;
import com.daesabu.meongcoach.training.application.provided.ReplyResult;
import com.daesabu.meongcoach.training.application.provided.dto.CommentCreateRequest;
import com.daesabu.meongcoach.training.application.required.CardCommentRepository;
import com.daesabu.meongcoach.training.application.required.CardRepository;
import com.daesabu.meongcoach.training.domain.comment.CardComment;
import com.daesabu.meongcoach.training.domain.exception.CardNotFoundException;
import com.daesabu.meongcoach.training.domain.exception.CommentNotFoundException;
import com.daesabu.meongcoach.user.application.provided.UserProfileFinder;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class CardCommentCreateService implements CardCommentCreator {

	private final CardRepository cardRepository;

	private final CardCommentRepository commentRepository;

	private final UserProfileFinder userProfileFinder;

	@Override
	@Transactional
	public CommentResult createComment(Long userId, Long cardId, CommentCreateRequest request) {
		cardRepository.findById(cardId).orElseThrow(() -> new CardNotFoundException(cardId));
		CardComment comment = CardComment.create(cardId, userId, request.content());
		CardComment saved = commentRepository.save(comment);
		String nickname = findNickname(userId);
		return CommentResult.from(saved, nickname, 0);
	}

	@Override
	@Transactional
	public ReplyResult createReply(Long userId, Long commentId, CommentCreateRequest request) {
		CardComment targetComment = commentRepository.findById(commentId)
				.orElseThrow(() -> new CommentNotFoundException(commentId));
		CardComment reply = CardComment.createReply(targetComment, userId, request.content());
		CardComment saved = commentRepository.save(reply);
		String nickname = findNickname(userId);
		return ReplyResult.from(saved, nickname);
	}

	private String findNickname(Long userId) {
		Map<Long, String> nicknames = userProfileFinder.findNicknames(Set.of(userId));
		return nicknames.get(userId);
	}
}
