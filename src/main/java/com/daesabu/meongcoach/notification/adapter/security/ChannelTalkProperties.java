package com.daesabu.meongcoach.notification.adapter.security;

import jakarta.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("meongcoach.channel-talk")
public record ChannelTalkProperties(@NotBlank String webhookToken, @NotBlank String memberIdPrefix) {

	public boolean matches(String presented) {
		if (presented == null) {
			return false;
		}
		byte[] expected = webhookToken.getBytes(StandardCharsets.UTF_8);
		byte[] actual = presented.getBytes(StandardCharsets.UTF_8);
		return MessageDigest.isEqual(expected, actual);
	}
}
