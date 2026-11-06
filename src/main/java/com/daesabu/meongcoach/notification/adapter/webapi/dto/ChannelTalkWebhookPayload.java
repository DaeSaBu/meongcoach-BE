package com.daesabu.meongcoach.notification.adapter.webapi.dto;

import java.util.Optional;
import java.util.regex.Pattern;

public record ChannelTalkWebhookPayload(String type, Entity entity, Refers refers) {

	private static final String MESSAGE_TYPE = "message";
	private static final String USER_CHAT = "userChat";
	private static final String MANAGER = "manager";
	private static final Pattern USER_ID = Pattern.compile("[1-9]\\d{0,17}");
	private static final String EMPTY_MESSAGE_BODY = "새 메시지가 도착했어요.";
	private static final int MAX_BODY_LENGTH = 100;
	private static final String ELLIPSIS = "…";

	public Optional<Long> recipientUserId(String memberIdPrefix) {
		if (!isManagerReply()) {
			return Optional.empty();
		}
		String memberId = memberId();
		if (memberId == null || !memberId.startsWith(memberIdPrefix)) {
			return Optional.empty();
		}
		String userId = memberId.substring(memberIdPrefix.length());
		if (!USER_ID.matcher(userId).matches()) {
			return Optional.empty();
		}
		return Optional.of(Long.parseLong(userId));
	}

	public String notificationBody() {
		if (entity == null || entity.plainText() == null || entity.plainText().isBlank()) {
			return EMPTY_MESSAGE_BODY;
		}
		String message = entity.plainText().strip();
		if (message.length() <= MAX_BODY_LENGTH) {
			return message;
		}
		return message.substring(0, MAX_BODY_LENGTH) + ELLIPSIS;
	}

	private boolean isManagerReply() {
		return MESSAGE_TYPE.equals(type) && entity != null
				&& USER_CHAT.equals(entity.chatType()) && MANAGER.equals(entity.personType());
	}

	private String memberId() {
		if (refers == null || refers.user() == null) {
			return null;
		}
		return refers.user().memberId();
	}

	public record Entity(String chatType, String personType, String plainText) {
	}

	public record Refers(User user) {
	}

	public record User(String memberId) {
	}
}
