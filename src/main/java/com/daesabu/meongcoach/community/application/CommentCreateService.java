package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.community.application.provided.CommentCreator;
import com.daesabu.meongcoach.community.application.provided.CommentResult;
import com.daesabu.meongcoach.community.application.provided.dto.CommentCreateRequest;
import com.daesabu.meongcoach.community.application.required.CommentRepository;
import com.daesabu.meongcoach.community.domain.Comment;
import com.daesabu.meongcoach.community.domain.CommentTarget;
import com.daesabu.meongcoach.user.application.provided.UserProfileFinder;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글·답글 작성 서비스. 대상 존재는 대상을 소유한 모듈에, 작성자 닉네임은 user 모듈에 묻는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentCreateService implements CommentCreator {

	private final CommentRepository commentRepository;

	private final CommentTargetChecker targetChecker;

	private final UserProfileFinder userProfileFinder;

	@Override
	@Transactional
	public CommentResult createComment(Long userId, CommentTarget target, CommentCreateRequest request) {
		targetChecker.verify(target);
		Comment comment = Comment.create(request.toCommand(target, userId));
		Comment saved = commentRepository.save(comment);
		return toResult(saved);
	}

	@Override
	@Transactional
	public CommentResult createReply(Long userId, Long commentId, CommentCreateRequest request) {
		Comment parent = commentRepository.getComment(commentId);
		Comment reply = Comment.reply(parent, userId, request.content());
		Comment saved = commentRepository.save(reply);
		return toResult(saved);
	}

	private CommentResult toResult(Comment saved) {
		Map<Long, String> nicknames = userProfileFinder.findNicknames(Set.of(saved.getAuthorId()));
		String authorNickname = nicknames.get(saved.getAuthorId());
		return CommentResult.of(saved, authorNickname);
	}
}
