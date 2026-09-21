package com.daesabu.meongcoach.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.auth.application.required.SocialTokenRevoker;
import com.daesabu.meongcoach.auth.domain.SocialProvider;
import java.util.List;
import org.junit.jupiter.api.Test;

class SocialTokenRevokersTest {

	private static final String CODE = "c1a2b3.0.abcd.efgh";

	@Test
	void revoker가_등록된_제공자는_인가_코드를_그대로_넘겨_revoke한다() {
		RecordingTokenRevoker appleRevoker = new RecordingTokenRevoker();
		SocialTokenRevokers revokers = new SocialTokenRevokers(List.of(appleRevoker));

		revokers.revokeIfSupported(SocialProvider.APPLE, CODE);

		assertThat(appleRevoker.revokedCode).isEqualTo(CODE);
	}

	@Test
	void revoker가_없는_제공자는_아무것도_하지_않는다() {
		RecordingTokenRevoker appleRevoker = new RecordingTokenRevoker();
		SocialTokenRevokers revokers = new SocialTokenRevokers(List.of(appleRevoker));

		revokers.revokeIfSupported(SocialProvider.KAKAO, CODE);

		assertThat(appleRevoker.revokedCode).isNull();
	}

	private static class RecordingTokenRevoker implements SocialTokenRevoker {

		private String revokedCode;

		@Override
		public SocialProvider provider() {
			return SocialProvider.APPLE;
		}

		@Override
		public void revoke(String authorizationCode) {
			this.revokedCode = authorizationCode;
		}
	}
}
