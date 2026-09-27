package com.daesabu.meongcoach.trainingcomment.domain;

public record CardCommentCreateCommand(Long cardId, Long authorId, String content) {
}
