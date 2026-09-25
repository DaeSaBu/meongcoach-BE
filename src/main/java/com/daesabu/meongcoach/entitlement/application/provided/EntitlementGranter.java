package com.daesabu.meongcoach.entitlement.application.provided;

import com.daesabu.meongcoach.entitlement.application.provided.dto.EntitlementGrantRequest;
import jakarta.validation.Valid;

public interface EntitlementGranter {
	void grant(@Valid EntitlementGrantRequest entitlementGrantRequest);
}
