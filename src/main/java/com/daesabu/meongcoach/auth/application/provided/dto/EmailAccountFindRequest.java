package com.daesabu.meongcoach.auth.application.provided.dto;

import com.daesabu.meongcoach.auth.domain.Email;
import jakarta.validation.constraints.NotNull;

public record EmailAccountFindRequest(
		@NotNull Email email
) {
}
