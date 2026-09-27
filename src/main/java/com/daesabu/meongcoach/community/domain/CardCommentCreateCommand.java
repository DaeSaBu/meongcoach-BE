package com.daesabu.meongcoach.community.domain;

public record CardCommentCreateCommand(Long cardId, Long authorId, String content) {
}
