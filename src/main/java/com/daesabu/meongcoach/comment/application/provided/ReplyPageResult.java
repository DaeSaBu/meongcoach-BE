package com.daesabu.meongcoach.comment.application.provided;

import java.util.List;

/**
 * 답글 한 페이지. nextCursor가 null이면 마지막 페이지다.
 */
public record ReplyPageResult(List<CommentResult> replies, Long nextCursor) {
}
