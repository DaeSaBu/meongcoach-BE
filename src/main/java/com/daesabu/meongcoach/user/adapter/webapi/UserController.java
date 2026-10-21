package com.daesabu.meongcoach.user.adapter.webapi;

import com.daesabu.meongcoach.shared.security.CurrentUserId;
import com.daesabu.meongcoach.user.adapter.webapi.dto.UserMeResponse;
import com.daesabu.meongcoach.user.application.provided.UserFinder;
import com.daesabu.meongcoach.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserFinder userFinder;

	@GetMapping("/me")
	public UserMeResponse me(@CurrentUserId Long userId) {
		User user = userFinder.findById(userId);
		return UserMeResponse.from(user);
	}
}
