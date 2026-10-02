package com.daesabu.meongcoach.auth.application;

import com.daesabu.meongcoach.auth.application.required.SocialTokenRevoker;
import com.daesabu.meongcoach.auth.domain.SocialProvider;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 제공자별 {@link SocialTokenRevoker} 구현체를 모아 제공자에 맞는 revoker로 위임한다.
 * 어느 제공자가 revoke 대상인지는 구현체 등록 여부로 정해진다(현재 Apple, 심사 지침 5.1.1(v)).
 * {@link SocialProfileReaders}와 달리 모든 제공자에 구현체를 요구하지 않는다 — revoker가 없는 제공자(카카오·구글)는
 * 연결을 끊지 않고 자격증명만 지우는 것이 의도다.
 */
@Component
public class SocialTokenRevokers {

	private final Map<SocialProvider, SocialTokenRevoker> revokers = new EnumMap<>(SocialProvider.class);

	public SocialTokenRevokers(List<SocialTokenRevoker> revokers) {
		revokers.forEach(revoker -> this.revokers.put(revoker.provider(), revoker));
	}

	public void revokeIfSupported(SocialProvider provider, String authorizationCode) {
		Optional.ofNullable(revokers.get(provider))
				.ifPresent(revoker -> revoker.revoke(authorizationCode));
	}
}
