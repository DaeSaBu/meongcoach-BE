package com.daesabu.meongcoach.notification.domain;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public record SupportReply(String event, String type, String chatType, String senderType, String text,
                           List<String> options, boolean broadcast, boolean marketing, String memberId) {
	private static final List<String> SUPPRESSED_OPTIONS = List.of("private", "silent", "silentToUser", "doNotSendApp", "doNotPost");

	private static final String MESSAGE_TYPE = "message";
	private static final String USER_CHAT = "userChat";
	private static final String MANAGER = "manager";
	private static final Pattern USER_ID = Pattern.compile("[1-9]\\d{0,17}");
	private static final String EMPTY_MESSAGE_BODY = "새 메시지가 도착했어요.";
	private static final int MAX_BODY_LENGTH = 100;
	private static final String ELLIPSIS = "…";

	public Optional<Long> recipientUserId() {
		if (!isManagerReply()) {
			return Optional.empty();
		}
		if (memberId == null) {
			return Optional.empty();
		}
		if (!USER_ID.matcher(memberId).matches()) {
			return Optional.empty();
		}
		Long recipientId = Long.valueOf(memberId);
		return Optional.of(recipientId);
	}

	public String notificationBody() {
		if (text == null || text.isBlank()) {
			return EMPTY_MESSAGE_BODY;
		}
		String message = text.strip();
		if (message.codePointCount(0, message.length()) <= MAX_BODY_LENGTH) {
			return message;
		}
		int endIndex = message.offsetByCodePoints(0, MAX_BODY_LENGTH);
		return message.substring(0, endIndex) + ELLIPSIS;
	}

	private boolean isManagerReply() {
		return "push".equals(event) && MESSAGE_TYPE.equals(type)
				&& USER_CHAT.equals(chatType) && MANAGER.equals(senderType) && !isSuppressed();
	}

	private boolean isSuppressed() {
		if (broadcast || marketing) {
			return true;
		}
		if (options == null) {
			return false;
		}
		return options.stream().anyMatch(SUPPRESSED_OPTIONS::contains);
	}
}
