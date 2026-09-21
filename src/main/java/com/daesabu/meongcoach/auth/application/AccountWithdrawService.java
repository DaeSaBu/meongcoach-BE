package com.daesabu.meongcoach.auth.application;

import com.daesabu.meongcoach.auth.application.provided.AccountWithdrawer;
import com.daesabu.meongcoach.auth.application.required.LocalAccountRepository;
import com.daesabu.meongcoach.auth.application.required.RefreshTokenRepository;
import com.daesabu.meongcoach.auth.application.required.SocialAccountRepository;
import com.daesabu.meongcoach.auth.application.required.SocialTokenRevoker;
import com.daesabu.meongcoach.auth.domain.RefreshToken;
import com.daesabu.meongcoach.auth.domain.SocialAccount;
import com.daesabu.meongcoach.auth.domain.SocialProvider;
import com.daesabu.meongcoach.user.application.provided.UserWithdrawer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계정 탈퇴. 스토어 심사 요건(계정 삭제는 비활성화가 아니어야 하고, 탈퇴 후 재가입이 가능해야 한다)을 맞추기 위해
 * 자격증명 행은 실제로 지우고 리프레시 토큰을 전부 폐기한 뒤, 회원 쪽 처리(프로필 삭제·상태 변경)를 {@link UserWithdrawer}에 위임한다.
 * user 모듈이 auth를 참조할 수 없으므로 탈퇴 흐름은 auth가 조율하며, 전체가 한 트랜잭션이다.
 * {@link SocialTokenRevoker}가 등록된 제공자(현재 Apple, 심사 지침 5.1.1(v))의 계정은 revoke가 먼저 성공해야 하며,
 * 실패하면 탈퇴 전체를 중단한다. 어느 제공자가 revoke 대상인지는 구현체 등록 여부로 정해지므로 여기서는 제공자를 가리지 않는다.
 */
@Service
@Transactional(readOnly = true)
public class AccountWithdrawService implements AccountWithdrawer {

	private final SocialAccountRepository socialAccountRepository;
	private final LocalAccountRepository localAccountRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final UserWithdrawer userWithdrawer;
	private final Map<SocialProvider, SocialTokenRevoker> revokers;

	public AccountWithdrawService(SocialAccountRepository socialAccountRepository,
	                              LocalAccountRepository localAccountRepository,
	                              RefreshTokenRepository refreshTokenRepository,
	                              UserWithdrawer userWithdrawer,
	                              List<SocialTokenRevoker> revokers) {
		this.socialAccountRepository = socialAccountRepository;
		this.localAccountRepository = localAccountRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.userWithdrawer = userWithdrawer;
		this.revokers = revokers.stream()
				.collect(Collectors.toUnmodifiableMap(SocialTokenRevoker::provider, Function.identity()));
	}

	@Override
	@Transactional
	public void withdraw(Long userId, String socialAuthorizationCode) {
		// 소셜 계정을 지우기 전에 revoke해야 실패했을 때 자격증명이 남아 새 코드로 다시 시도할 수 있다.
		// 외부 호출이 트랜잭션 안에서 일어나지만 아직 쓴 것이 없고 전역 HTTP 타임아웃(연결 2초·응답 3초)이 상한이라 감수한다
		List<SocialAccount> socialAccounts = socialAccountRepository.findAllByUserId(userId);
		socialAccounts.forEach(account -> revokeIfSupported(account, socialAuthorizationCode));
		// 자격증명을 지워야 (provider, provider_id)·email 유니크 제약이 풀려 같은 계정으로 재가입할 수 있다
		socialAccountRepository.deleteAll(socialAccounts);
		localAccountRepository.deleteByUserId(userId);
		// 탈퇴 즉시 남은 리프레시 토큰으로 재발급할 수 없게 한다
		refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId).forEach(RefreshToken::revoke);
		userWithdrawer.withdraw(userId);
	}

	// revoker가 없는 제공자(카카오·구글)는 연결을 끊지 않고 그대로 삭제만 한다
	private void revokeIfSupported(SocialAccount account, String authorizationCode) {
		Optional.ofNullable(revokers.get(account.getProvider()))
				.ifPresent(revoker -> revoker.revoke(authorizationCode));
	}
}
