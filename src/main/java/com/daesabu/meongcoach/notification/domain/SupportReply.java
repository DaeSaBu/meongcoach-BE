package com.daesabu.meongcoach.notification.domain;

import java.util.Optional;
import java.util.regex.Pattern;

public record SupportReply(Long userId, String messageId) {
	public static Optional<SupportReply> from(String memberId, String messageId, String environment) {
		if (memberId == null || messageId == null || messageId.isBlank() || messageId.length() > 200) {
			return Optional.empty();
		}
		String prefix = environment + "-";
		if (!memberId.startsWith(prefix)) {
			return Optional.empty();
		}
		String id = memberId.substring(prefix.length());
		if (!Pattern.matches("[1-9][0-9]{0,18}", id)) {
			return Optional.empty();
		}
		try {
			Long userId = Long.valueOf(id);
			SupportReply reply = new SupportReply(userId, messageId);
			return Optional.of(reply);
		} catch (NumberFormatException exception) {
			return Optional.empty();
		}
	}
}
