package com.daesabu.meongcoach.community.application.provided;

import com.daesabu.meongcoach.report.application.provided.dto.ReportRegisterRequest;
import jakarta.validation.Valid;

/**
 * 게시글·댓글 신고 능력.
 */
public interface ContentReportRegister {

	/**
	 * 회원이 게시글을 신고한 기록을 저장한다. 게시글이 없거나 삭제됐으면 {@code PostNotFoundException}, 본인이 쓴 게시글이면
	 * {@code SelfReportNotAllowedException}, 사유가 지원하지 않는 값이면 {@code InvalidReportReasonException}, 이미 신고한 게시글이면
	 * {@code ReportAlreadyRegisteredException}을 던진다.
	 */
	void reportPost(Long userId, Long postId, @Valid ReportRegisterRequest request);

	/**
	 * 회원이 댓글 또는 답글을 신고한 기록을 저장한다. 댓글이 없거나 삭제됐거나 게시글이 삭제됐으면 {@code CommentNotFoundException}, 본인이 쓴 댓글이면
	 * {@code SelfReportNotAllowedException}, 사유가 지원하지 않는 값이면 {@code InvalidReportReasonException}, 이미 신고한 댓글이면
	 * {@code ReportAlreadyRegisteredException}을 던진다.
	 */
	void reportComment(Long userId, Long commentId, @Valid ReportRegisterRequest request);
}
