package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.user.application.provided.AuthToken;
import com.daesabu.meongcoach.user.application.required.LocalAccountRepository;
import com.daesabu.meongcoach.user.application.required.RefreshTokenRepository;
import com.daesabu.meongcoach.user.application.required.SocialAccountRepository;
import com.daesabu.meongcoach.user.application.required.TokenProvider;
import com.daesabu.meongcoach.user.domain.LocalAccount;
import com.daesabu.meongcoach.user.domain.RefreshToken;
import com.daesabu.meongcoach.user.domain.SocialAccount;
import com.daesabu.meongcoach.user.domain.User;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 토큰 쌍 발급의 단일 창구. JWT 발급과 리프레시 토큰 행 저장을 항상 함께 수행해,
 * 로그인·재발급 어느 경로로 발급된 리프레시 토큰이든 재발급 시 저장 이력으로 확인할 수 있게 한다.
 * 액세스 토큰에 실을 이메일도 여기서 읽는다 — 로그인·재발급이 모두 이 창구를 거치므로 발급 경로마다 갈리지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthTokenIssueService {

	private final TokenProvider tokenProvider;
	private final RefreshTokenRepository refreshTokenRepository;
	private final SocialAccountRepository socialAccountRepository;
	private final LocalAccountRepository localAccountRepository;

	@Transactional
	public AuthToken issue(User user) {
		String email = resolveEmail(user);
		AuthToken token = tokenProvider.issue(user.getId(), email);
		RefreshToken refreshToken = RefreshToken.issue(user, token.refreshTokenId(), token.refreshTokenExpiresAt());
		refreshTokenRepository.save(refreshToken);
		return token;
	}

	// 소셜 계정을 먼저 본다. 로컬 계정은 심사용 테스트 계정 전용이라 한 회원이 둘을 함께 갖지 않는다.
	// 카카오·애플은 동의가 없으면 이메일을 내려주지 않으므로 이메일이 없는 회원도 정상이다
	private String resolveEmail(User user) {
		List<SocialAccount> socialAccounts = socialAccountRepository.findAllByUser(user);
		Optional<String> socialEmail = socialAccounts.stream()
				.map(SocialAccount::getEmail)
				.filter(Objects::nonNull)
				.findFirst();
		if (socialEmail.isPresent()) {
			return socialEmail.get();
		}
		Optional<LocalAccount> localAccount = localAccountRepository.findByUser(user);
		if (localAccount.isEmpty()) {
			return null;
		}
		return localAccount.get().getEmail().address();
	}
}
