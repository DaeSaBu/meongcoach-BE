package com.daesabu.meongcoach.community.application.required;

import java.time.LocalDateTime;

/**
 * 댓글 목록 쿼리의 행. 답글 수·좋아요 수·내 좋아요 여부까지 DB가 집계한 값이며 작성자 닉네임만 비어 있다.
 */
public record CardCommentSummary(Long id, Long parentId, Long authorId, String content, LocalDateTime createdAt,
                                 long replyCount, long likeCount, boolean likedByMe) {
}
