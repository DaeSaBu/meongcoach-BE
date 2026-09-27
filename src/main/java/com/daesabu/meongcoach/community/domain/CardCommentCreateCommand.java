package com.daesabu.meongcoach.comment.domain;

public record CardCommentCreateCommand(Long cardId, Long authorId, String content) {
}
