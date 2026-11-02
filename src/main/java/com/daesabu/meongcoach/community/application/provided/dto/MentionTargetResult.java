package com.daesabu.meongcoach.community.application.provided.dto;

import com.daesabu.meongcoach.community.domain.MentionTargetType;

public record MentionTargetResult(MentionTargetType targetType, Long targetId, MentionLevelResult category,
		MentionLevelResult topic, MentionLevelResult curriculum, MentionLevelResult lesson) {
}
