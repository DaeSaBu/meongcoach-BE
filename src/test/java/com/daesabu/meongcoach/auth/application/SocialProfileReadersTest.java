package com.daesabu.meongcoach.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.auth.application.required.SocialProfileReader;
import com.daesabu.meongcoach.auth.domain.Email;
import com.daesabu.meongcoach.auth.domain.SocialProfile;
import com.daesabu.meongcoach.auth.domain.SocialProvider;
import java.util.List;
import org.junit.jupiter.api.Test;

class SocialProfileReadersTest {

	private static final String CREDENTIAL = "id-token";

	@Test
	void 제공자에_맞는_리더로_자격증명을_읽는다() {
		SocialProfileReaders readers = new SocialProfileReaders(List.of(
				new StubSocialProfileReader(SocialProvider.KAKAO),
				new StubSocialProfileReader(SocialProvider.GOOGLE),
				new StubSocialProfileReader(SocialProvider.APPLE)));

		SocialProfile profile = readers.read(SocialProvider.GOOGLE, CREDENTIAL);

		assertThat(profile.provider()).isEqualTo(SocialProvider.GOOGLE);
	}

	@Test
	void 리더가_없는_제공자가_있으면_생성할_수_없다() {
		List<SocialProfileReader> readers = List.of(new StubSocialProfileReader(SocialProvider.KAKAO));

		assertThatThrownBy(() -> new SocialProfileReaders(readers))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("GOOGLE");
	}

	private record StubSocialProfileReader(SocialProvider provider) implements SocialProfileReader {

		@Override
		public SocialProfile read(String credential) {
			return new SocialProfile(provider, "provider-id", new Email("a@b.com"));
		}
	}
}
