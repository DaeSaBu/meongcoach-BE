package com.daesabu.meongcoach.training_comment.domain;

public record CardCommentCreateCommand(Long cardId, Long authorId, String content) {
}
