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

	// 로그인 응답은 토큰만 내려주므로, 클라이언트는 로그인 직후 이 응답으로 온보딩 화면 진입 여부를 정한다.
	// 온보딩 상태의 단일 원천은 users.role이라 프로필 존재 여부는 보지 않는다
	@GetMapping("/me")
	public UserMeResponse me(@CurrentUserId Long userId) {
		User user = userFinder.findById(userId);
		return UserMeResponse.from(user);
	}
}
