package com.daesabu.meongcoach.entitlement.domain;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.time.Instant;

public record ActiveEntitlement(EntitlementType type, Instant expiresAt) {
}
