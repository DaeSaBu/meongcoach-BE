package com.daesabu.meongcoach.support.domain;

public record ChatMessageSentEvent(long messageId, long conversationId, long senderId) {}
