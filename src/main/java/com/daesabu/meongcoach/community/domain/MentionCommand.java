package com.daesabu.meongcoach.community.domain;

public record MentionCommand(MentionTargetType targetType, Long targetId) {
}
