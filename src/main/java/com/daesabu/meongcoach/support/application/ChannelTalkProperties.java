package com.daesabu.meongcoach.support.application;

import com.daesabu.meongcoach.support.domain.exception.ChannelTalkUnavailableException;
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
public record ChannelTalkProperties(String webhookToken, @Pattern(regexp = "(?:[0-9a-fA-F]{64})?") String memberHashKey) {

	public boolean matches(String presented) {
		if (presented == null || webhookToken == null || webhookToken.isBlank()) {
			return false;
		}
		byte[] expected = webhookToken.getBytes(StandardCharsets.UTF_8);
		byte[] actual = presented.getBytes(StandardCharsets.UTF_8);
		return MessageDigest.isEqual(expected, actual);
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
