package com.daesabu.meongcoach.support.domain;

import java.util.UUID;

public record ChatPushJob(long messageId, long userId, int attempts, UUID leaseToken) {}
