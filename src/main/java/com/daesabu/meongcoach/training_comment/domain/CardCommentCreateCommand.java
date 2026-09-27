package com.daesabu.meongcoach.cardcomment.domain;

public record CardCommentCreateCommand(Long cardId, Long authorId, String content) {
}
