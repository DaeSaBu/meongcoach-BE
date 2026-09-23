package com.daesabu.meongcoach.auth.application;

import com.daesabu.meongcoach.auth.application.required.SocialProfileReader;
import com.daesabu.meongcoach.auth.domain.SocialProfile;
import com.daesabu.meongcoach.auth.domain.SocialProvider;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * 제공자별 {@link SocialProfileReader} 구현체를 모아 제공자에 맞는 리더로 위임한다.
 * 구현체는 스프링이 주입하므로 제공자를 늘릴 때는 enum 상수와 구현체 빈만 추가하면 된다.
 * 리더가 없는 제공자는 요청 시점이 아니라 기동 시점에 드러나도록 생성자에서 검증한다.
 */
@Component
public class SocialProfileReaders {

	private final Map<SocialProvider, SocialProfileReader> readers = new EnumMap<>(SocialProvider.class);

	public SocialProfileReaders(List<SocialProfileReader> readers) {
		readers.forEach(reader -> this.readers.put(reader.provider(), reader));
		validateAllProvidersSupported();
	}

	public SocialProfile read(SocialProvider provider, String credential) {
		SocialProfileReader reader = readers.get(provider);

		return reader.read(credential);
	}

	private void validateAllProvidersSupported() {
		List<SocialProvider> unsupported = Stream.of(SocialProvider.values())
				.filter(provider -> !readers.containsKey(provider))
				.toList();
		if (unsupported.isEmpty()) {
			return;
		}

		throw new IllegalStateException("리더 구현체가 없는 소셜 제공자가 있습니다: " + unsupported);
	}
}
