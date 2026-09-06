package com.daesabu.meongcoach.user.adapter.webapi;

import com.daesabu.meongcoach.shared.security.CurrentUserId;
import com.daesabu.meongcoach.user.adapter.webapi.dto.UserWithdrawRequest;
import com.daesabu.meongcoach.user.application.provided.UserWithdrawer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserWithdrawer userWithdrawer;

	// 회원 행은 남지만 클라이언트 관점에서는 계정이 사라지므로 204로 응답한다.
	// 토큰의 회원만 탈퇴할 수 있으므로 식별자는 경로 변수가 아니라 인증 주체에서 받는다.
	// 본문은 Apple 계정 회원만 보내므로 선택으로 두고, 없으면 코드 없이 위임한다.
	// API 필드는 클라이언트가 실제로 보내는 Apple 코드지만, 서비스는 제공자를 가리지 않는 인가 코드로 받는다
	@DeleteMapping("/me")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void withdraw(@CurrentUserId Long userId, @RequestBody(required = false) UserWithdrawRequest request) {
		String appleAuthorizationCode = appleAuthorizationCode(request);
		userWithdrawer.withdraw(userId, appleAuthorizationCode);
	}

	private static String appleAuthorizationCode(UserWithdrawRequest request) {
		if (request == null) {
			return null;
		}
		return request.appleAuthorizationCode();
	}
}
