package com.daesabu.meongcoach.notification.domain;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public final class SupportReply {
	private final Long recipientUserId;
	private final String text;

	private SupportReply(Long recipientUserId, String text) {
		this.recipientUserId = recipientUserId;
		this.text = text;
	}

	public Long recipientUserId() {
		return recipientUserId;
	}

	private static final List<String> SUPPRESSED_OPTIONS = List.of("private", "silent", "silentToUser", "doNotSendApp", "doNotPost");

	private static final String MESSAGE_TYPE = "message";
	private static final String USER_CHAT = "userChat";
	private static final String MANAGER = "manager";
	private static final Pattern USER_ID = Pattern.compile("[1-9]\\d{0,17}");
	private static final String EMPTY_MESSAGE_BODY = "새 메시지가 도착했어요.";
	private static final int MAX_BODY_LENGTH = 100;
	private static final String ELLIPSIS = "…";

	public static Optional<SupportReply> from(SupportReplyCommand command) {
		if (!isManagerReply(command)) {
			return Optional.empty();
		}
		String memberId = command.memberId();
		if (memberId == null || !USER_ID.matcher(memberId).matches()) {
			return Optional.empty();
		}
		Long recipientUserId = Long.valueOf(memberId);
		SupportReply reply = new SupportReply(recipientUserId, command.text());
		return Optional.of(reply);
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

	private static boolean isManagerReply(SupportReplyCommand command) {
		return "push".equals(command.event()) && MESSAGE_TYPE.equals(command.type())
				&& USER_CHAT.equals(command.chatType()) && MANAGER.equals(command.senderType()) && !isSuppressed(command);
	}

	private static boolean isSuppressed(SupportReplyCommand command) {
		if (command.broadcast() || command.marketing()) {
			return true;
		}
		List<String> options = command.options();
		if (options == null) {
			return false;
		}
		return options.stream().anyMatch(SUPPRESSED_OPTIONS::contains);
	}
}
