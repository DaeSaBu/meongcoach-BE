package com.daesabu.meongcoach.user.adapter.webapi;

import com.daesabu.meongcoach.shared.security.CurrentUserId;
import com.daesabu.meongcoach.user.adapter.webapi.dto.UserMeResponse;
import com.daesabu.meongcoach.user.adapter.webapi.dto.UserProfileResponse;
import com.daesabu.meongcoach.user.application.provided.UserFinder;
import com.daesabu.meongcoach.user.application.provided.UserProfileFinder;
import com.daesabu.meongcoach.user.application.provided.UserProfileUpdateRequest;
import com.daesabu.meongcoach.user.application.provided.UserProfileUpdater;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserFinder userFinder;
	private final UserProfileFinder userProfileFinder;
	private final UserProfileUpdater userProfileUpdater;

	@GetMapping("/me")
	public UserMeResponse me(@CurrentUserId Long userId) {
		User user = userFinder.findById(userId);
		return UserMeResponse.from(user);
	}

	@GetMapping("/profile")
	public UserProfileResponse profile(@CurrentUserId Long userId) {
		UserProfile profile = userProfileFinder.findProfile(userId);
		return UserProfileResponse.from(profile);
	}

	@PutMapping("/profile")
	public UserProfileResponse updateProfile(@CurrentUserId Long userId,
			@Valid @RequestBody UserProfileUpdateRequest request) {
		UserProfile profile = userProfileUpdater.update(userId, request);
		return UserProfileResponse.from(profile);
	}
}
