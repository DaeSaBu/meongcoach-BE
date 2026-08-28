package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.user.application.provided.UserWithdrawer;
import com.daesabu.meongcoach.user.application.required.LocalAccountRepository;
import com.daesabu.meongcoach.user.application.required.SocialAccountRepository;
import com.daesabu.meongcoach.user.application.required.UserProfileRepository;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 탈퇴. 스토어 심사 요건(계정 삭제는 비활성화가 아니어야 하고, 탈퇴 후 재가입이 가능해야 한다)을 맞추기 위해
 * 개인정보·자격증명 행은 실제로 지우고, 회원 행은 타 모듈의 userId 참조 정합성을 위해 상태만 바꿔 남긴다.
 * 탈퇴 회원의 토큰 차단은 {@link RegisteredUserCheckService}가 맡는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserWithdrawService implements UserWithdrawer {

	private final UserRepository userRepository;
	private final SocialAccountRepository socialAccountRepository;
	private final LocalAccountRepository localAccountRepository;
	private final UserProfileRepository userProfileRepository;

	@Override
	@Transactional
	public void withdraw(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException(userId));
		// 자격증명을 지워야 (provider, provider_id)·email 유니크 제약이 풀려 같은 계정으로 재가입할 수 있다
		socialAccountRepository.deleteAllByUser(user);
		localAccountRepository.deleteByUser(user);
		// 온보딩 미완료 회원은 프로필이 없으므로 없으면 무시하는 deleteById를 쓴다
		userProfileRepository.deleteById(userId);
		// 영속 상태 엔티티라 트랜잭션 커밋 시 변경이 반영된다
		user.withdraw();
	}
}
