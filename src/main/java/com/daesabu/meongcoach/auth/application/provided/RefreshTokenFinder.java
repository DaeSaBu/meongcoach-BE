package com.daesabu.meongcoach.auth.application.provided;

import com.daesabu.meongcoach.auth.application.provided.dto.RefreshTokenFindRequest;
import com.daesabu.meongcoach.auth.domain.RefreshToken;
import jakarta.validation.Valid;

public interface RefreshTokenFinder {
	RefreshToken findByTokenId(@Valid RefreshTokenFindRequest refreshTokenFindRequest);
}
