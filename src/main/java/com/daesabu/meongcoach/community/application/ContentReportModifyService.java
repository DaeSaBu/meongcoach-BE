package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.community.application.provided.CommentFinder;
import com.daesabu.meongcoach.community.application.provided.ContentReportRegister;
import com.daesabu.meongcoach.community.application.provided.PostFinder;
import com.daesabu.meongcoach.community.domain.Comment;
import com.daesabu.meongcoach.community.domain.Post;
import com.daesabu.meongcoach.community.domain.exception.SelfReportNotAllowedException;
import com.daesabu.meongcoach.report.application.provided.ReportRegister;
import com.daesabu.meongcoach.report.application.provided.dto.ReportRegisterRequest;
import com.daesabu.meongcoach.report.domain.shared.ReportTargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentReportModifyService implements ContentReportRegister {

	private final PostFinder postFinder;
	private final CommentFinder commentFinder;
	private final ReportRegister reportRegister;

	@Override
	@Transactional
	public void reportPost(Long userId, Long postId, ReportRegisterRequest request) {
		Post post = postFinder.findPost(postId);
		validateNotAuthor(userId, post.getUserId());

		reportRegister.register(userId, ReportTargetType.COMMUNITY_POST, postId, request);
	}

	@Override
	@Transactional
	public void reportComment(Long userId, Long commentId, ReportRegisterRequest request) {
		Comment comment = commentFinder.findComment(commentId);
		validateNotAuthor(userId, comment.getUserId());

		reportRegister.register(userId, ReportTargetType.COMMUNITY_COMMENT, commentId, request);
	}

	private void validateNotAuthor(Long userId, Long authorId) {
		if (authorId.equals(userId)) {
			throw new SelfReportNotAllowedException();
		}
	}
}
