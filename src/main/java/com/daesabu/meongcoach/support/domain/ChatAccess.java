package com.daesabu.meongcoach.support.domain;

import java.time.Instant;

public record ChatAccess(long userId, boolean operator, Instant expiresAt) {}
