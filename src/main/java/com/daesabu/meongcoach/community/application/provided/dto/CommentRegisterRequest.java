package com.daesabu.meongcoach.community.application.provided.dto;

import jakarta.validation.constraints.Positive;

public record CommentRegisterRequest(String content, @Positive Long parentId) {
}
