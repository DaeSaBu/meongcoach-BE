package com.daesabu.meongcoach.user.adapter.client;

import com.daesabu.meongcoach.user.application.required.SocialProfileReader;
import com.daesabu.meongcoach.user.domain.SocialAccountLinkCommand;
import com.daesabu.meongcoach.user.domain.SocialProvider;
import com.daesabu.meongcoach.user.domain.exception.InvalidSocialTokenException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** local/dev 환경에서 소셜 SDK 없이 고정 개발 계정으로 로그인한다. */
@Component
@Profile({"local", "dev"})
public class DevSocialProfileReader implements SocialProfileReader {

	private static final int MINIMUM_TOKEN_LENGTH = 32;
	private static final String PROVIDER_ID = "dev-user";

	private final byte[] expectedToken;

	public DevSocialProfileReader(@Value("${meongcoach.dev-login-token}") String expectedToken) {
		if (expectedToken == null || expectedToken.length() < MINIMUM_TOKEN_LENGTH) {
			throw new IllegalArgumentException("DEV_LOGIN_TOKEN은 32자 이상이어야 합니다.");
		}
		this.expectedToken = expectedToken.getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public SocialProvider provider() {
		return SocialProvider.KAKAO;
	}

	@Override
	public SocialAccountLinkCommand read(String credential) {
		byte[] actualToken = credential == null ? new byte[0] : credential.getBytes(StandardCharsets.UTF_8);
		if (!MessageDigest.isEqual(expectedToken, actualToken)) {
			throw new InvalidSocialTokenException();
		}
		return new SocialAccountLinkCommand(SocialProvider.KAKAO, PROVIDER_ID, "dev@meongcoach.local");
	}
}
