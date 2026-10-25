package com.daesabu.meongcoach.user.application.provided;

import com.daesabu.meongcoach.user.domain.UserProfileCreateCommand;
import java.util.Set;

public record UserProfileRegisterRequest(String nickname) {

	public UserProfileCreateCommand toCommand() {
		return new UserProfileCreateCommand(nickname, null, null, Set.of(), Set.of());
	}
}
