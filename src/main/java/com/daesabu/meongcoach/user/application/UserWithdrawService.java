package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.user.application.provided.UserWithdrawer;
import com.daesabu.meongcoach.user.application.required.LocalAccountRepository;
import com.daesabu.meongcoach.user.application.required.RefreshTokenRepository;
import com.daesabu.meongcoach.user.application.required.SocialAccountRepository;
import com.daesabu.meongcoach.user.application.required.SocialTokenRevoker;
import com.daesabu.meongcoach.user.application.required.UserProfileRepository;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.RefreshToken;
import com.daesabu.meongcoach.user.domain.SocialProvider;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.exception.AppleAuthorizationCodeRequiredException;
import com.daesabu.meongcoach.user.domain.exception.UserNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 탈퇴. 스토어 심사 요건(계정 삭제는 비활성화가 아니어야 하고, 탈퇴 후 재가입이 가능해야 한다)을 맞추기 위해
 * 개인정보·자격증명 행은 실제로 지우고, 회원 행은 타 모듈의 userId 참조 정합성을 위해 상태만 바꿔 남긴다.
 * 리프레시 토큰은 여기서 전부 폐기하고, 저장하지 않는 액세스 토큰은 {@link RegisteredUserCheckService}가 만료까지 막는다.
 * Apple 계정 회원은 심사 지침 5.1.1(v)에 따라 Apple 토큰 revoke를 먼저 하고, 실패하면 탈퇴 전체를 중단한다.
 */
@Service
@Transactional(readOnly = true)
public class UserWithdrawService implements UserWithdrawer {

	private final UserRepository userRepository;
	private final SocialAccountRepository socialAccountRepository;
	private final LocalAccountRepository localAccountRepository;
	private final UserProfileRepository userProfileRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final Map<SocialProvider, SocialTokenRevoker> revokers;

	public UserWithdrawService(UserRepository userRepository,
	                           SocialAccountRepository socialAccountRepository,
	                           LocalAccountRepository localAccountRepository,
	                           UserProfileRepository userProfileRepository,
	                           RefreshTokenRepository refreshTokenRepository,
	                           List<SocialTokenRevoker> revokers) {
		this.userRepository = userRepository;
		this.socialAccountRepository = socialAccountRepository;
		this.localAccountRepository = localAccountRepository;
		this.userProfileRepository = userProfileRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.revokers = revokers.stream()
				.collect(Collectors.toUnmodifiableMap(SocialTokenRevoker::provider, Function.identity()));
	}

	@Override
	@Transactional
	public void withdraw(Long userId, String appleAuthorizationCode) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException(userId));
		// 소셜 계정을 지우기 전에 revoke해야 실패했을 때 자격증명이 남아 새 코드로 다시 시도할 수 있다.
		// 외부 호출이 트랜잭션 안에서 일어나지만 아직 쓴 것이 없고 전역 HTTP 타임아웃(연결 2초·응답 3초)이 상한이라 감수한다
		socialAccountRepository.findByUserAndProvider(user, SocialProvider.APPLE)
				.ifPresent(account -> revokeApple(appleAuthorizationCode));
		// 자격증명을 지워야 (provider, provider_id)·email 유니크 제약이 풀려 같은 계정으로 재가입할 수 있다
		socialAccountRepository.deleteAllByUser(user);
		localAccountRepository.deleteByUser(user);
		// 온보딩 미완료 회원은 프로필이 없으므로 없으면 무시하는 deleteById를 쓴다
		userProfileRepository.deleteById(userId);
		// 탈퇴 즉시 남은 리프레시 토큰으로 재발급할 수 없게 한다
		refreshTokenRepository.findAllByUserAndRevokedAtIsNull(user).forEach(RefreshToken::revoke);
		// 영속 상태 엔티티라 트랜잭션 커밋 시 변경이 반영된다
		user.withdraw();
	}

	private void revokeApple(String authorizationCode) {
		if (authorizationCode == null || authorizationCode.isBlank()) {
			throw new AppleAuthorizationCodeRequiredException();
		}
		revokers.get(SocialProvider.APPLE).revoke(authorizationCode);
	}
}
