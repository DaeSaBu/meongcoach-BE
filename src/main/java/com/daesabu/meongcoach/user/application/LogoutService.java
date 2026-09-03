package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.user.application.provided.Logout;
import com.daesabu.meongcoach.user.application.required.RefreshTokenRepository;
import com.daesabu.meongcoach.user.application.required.TokenProvider;
import com.daesabu.meongcoach.user.domain.RefreshToken;
import com.daesabu.meongcoach.user.domain.exception.InvalidRefreshTokenException;
import com.daesabu.meongcoach.user.domain.vo.RefreshTokenId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그아웃. 제시된 리프레시 토큰 하나만 폐기하므로 다른 기기의 로그인은 유지된다.
 * 액세스 토큰은 저장하지 않아 만료(1시간)까지 유효하며, 클라이언트가 버리는 것으로 끝난다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LogoutService implements Logout {

	private final TokenProvider tokenProvider;
	private final RefreshTokenRepository refreshTokenRepository;

	@Override
	@Transactional
	public void logout(String refreshToken) {
		RefreshTokenId tokenId = tokenProvider.extractTokenId(refreshToken);
		RefreshToken stored = refreshTokenRepository.findByTokenId(tokenId)
				.orElseThrow(InvalidRefreshTokenException::new);
		// 회원 등록 여부는 확인하지 않는다. 폐기는 탈퇴 회원의 토큰에도 해가 없다
		stored.revoke();
	}
}
