package com.daesabu.meongcoach.comment.application.provided;

import java.util.List;

/**
 * 댓글 한 페이지. 최상위 댓글 목록이면 totalCount는 답글을 포함한 카드 전체 댓글 수이고, 답글 목록이면 스레드의 답글 수다.
 * nextCursor가 null이면 마지막 페이지다.
 */
public record CommentPageResult(List<CommentResult> comments, long totalCount, Long nextCursor) {
}
