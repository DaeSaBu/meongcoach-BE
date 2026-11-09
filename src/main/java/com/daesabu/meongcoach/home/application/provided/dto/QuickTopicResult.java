package com.daesabu.meongcoach.home.application.provided.dto;

import com.daesabu.meongcoach.home.domain.QuickTopicRole;
import com.daesabu.meongcoach.training.application.provided.dto.TopicProgressResult;

public record QuickTopicResult(TopicProgressResult topic, QuickTopicRole role) {
}
