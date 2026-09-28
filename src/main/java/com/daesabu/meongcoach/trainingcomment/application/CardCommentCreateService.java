package com.daesabu.meongcoach.trainingcomment.application;

import com.daesabu.meongcoach.training.application.provided.CardFinder;
import com.daesabu.meongcoach.trainingcomment.application.provided.CardCommentCreator;
import com.daesabu.meongcoach.trainingcomment.application.provided.CommentResult;
import com.daesabu.meongcoach.trainingcomment.application.provided.dto.CommentCreateRequest;
import com.daesabu.meongcoach.trainingcomment.application.required.CardCommentRepository;
import com.daesabu.meongcoach.trainingcomment.domain.CardComment;
import com.daesabu.meongcoach.trainingcomment.domain.exception.CardNotFoundException;
import com.daesabu.meongcoach.user.application.provided.UserProfileFinder;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * 댓글·답글 작성 서비스. 카드 존재는 training 모듈에, 작성자 닉네임은 user 모듈에 묻는다.
 */
@Service
@RequiredArgsConstructor
@Validated
@Transactional(readOnly = true)
public class CardCommentCreateService implements CardCommentCreator {

	private final CardCommentRepository commentRepository;

	private final CardFinder cardFinder;

	private final UserProfileFinder userProfileFinder;

	@Override
	@Transactional
	public CommentResult createComment(Long userId, Long cardId, CommentCreateRequest request) {
		if (!cardFinder.existsCard(cardId)) {
			throw new CardNotFoundException(cardId);
		}
		CardComment comment = CardComment.create(request.toCommand(cardId, userId));
		CardComment saved = commentRepository.save(comment);
		return toResult(saved);
	}

	@Override
	@Transactional
	public CommentResult createReply(Long userId, Long commentId, CommentCreateRequest request) {
		CardComment target = commentRepository.getComment(commentId);
		CardComment reply = CardComment.reply(target, userId, request.content());
		CardComment saved = commentRepository.save(reply);
		return toResult(saved);
	}

	private CommentResult toResult(CardComment saved) {
		Map<Long, String> nicknames = userProfileFinder.findNicknames(Set.of(saved.getAuthorId()));
		String authorNickname = nicknames.get(saved.getAuthorId());
		return CommentResult.of(saved, authorNickname);
	}
}
