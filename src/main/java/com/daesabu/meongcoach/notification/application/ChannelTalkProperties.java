package com.daesabu.meongcoach.notification.application;

import com.daesabu.meongcoach.notification.domain.exception.ChannelTalkUnavailableException;
import com.daesabu.meongcoach.notification.domain.exception.InvalidChannelTalkTokenException;
import jakarta.validation.constraints.Pattern;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("meongcoach.channel-talk")
public record ChannelTalkProperties(
		@Pattern(regexp = "local|dev|prod") String environment,
		String webhookToken,
		@Pattern(regexp = "(?:[0-9a-fA-F]{64})?") String memberHashKey
) {
	public void verifyToken(String presented) {
		if (webhookToken == null || webhookToken.isBlank() || presented == null) {
			throw new InvalidChannelTalkTokenException();
		}
		byte[] expected = webhookToken.getBytes(StandardCharsets.UTF_8);
		byte[] actual = presented.getBytes(StandardCharsets.UTF_8);
		if (!MessageDigest.isEqual(expected, actual)) {
			throw new InvalidChannelTalkTokenException();
		}
	}

	public String memberHash(String memberId) {
		if (memberHashKey == null || memberHashKey.isBlank()) {
			throw new ChannelTalkUnavailableException();
		}
		try {
			byte[] key = HexFormat.of().parseHex(memberHashKey);
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(key, "HmacSHA256"));
			byte[] digest = mac.doFinal(memberId.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("회원 해시를 생성할 수 없습니다.", exception);
		}
	}
}
