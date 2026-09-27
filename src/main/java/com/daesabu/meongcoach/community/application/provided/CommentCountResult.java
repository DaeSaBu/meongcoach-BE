package com.daesabu.meongcoach.community.application.provided;

/**
 * 대상 하나의 댓글 수. 답글을 포함한다.
 */
public record CommentCountResult(Long targetId, long totalCount) {
}
