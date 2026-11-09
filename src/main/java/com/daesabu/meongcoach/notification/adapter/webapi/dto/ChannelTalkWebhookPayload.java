package com.daesabu.meongcoach.notification.adapter.webapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChannelTalkWebhookPayload(String event, String type, Entity entity, Refers refers) {

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
		String memberId = memberId();
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
		if (entity == null || entity.plainText() == null || entity.plainText().isBlank()) {
			return EMPTY_MESSAGE_BODY;
		}
		String message = entity.plainText().strip();
		if (message.codePointCount(0, message.length()) <= MAX_BODY_LENGTH) {
			return message;
		}
		int endIndex = message.offsetByCodePoints(0, MAX_BODY_LENGTH);
		return message.substring(0, endIndex) + ELLIPSIS;
	}

	private boolean isManagerReply() {
		return "push".equals(event) && MESSAGE_TYPE.equals(type) && entity != null
				&& USER_CHAT.equals(entity.chatType()) && MANAGER.equals(entity.personType()) && !entity.isSuppressed();
	}

	private String memberId() {
		if (refers == null || refers.user() == null) {
			return null;
		}
		return refers.user().memberId();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Entity(String chatType, String personType, String plainText, List<String> options, Boolean broadcastedMsg, Object marketing) {
		private static final List<String> SUPPRESSED_OPTIONS = List.of("private", "silent", "silentToUser", "doNotSendApp", "doNotPost");

		public boolean isSuppressed() {
			if (Boolean.TRUE.equals(broadcastedMsg) || marketing != null) {
				return true;
			}
			if (options == null) {
				return false;
			}
			return options.stream().anyMatch(SUPPRESSED_OPTIONS::contains);
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Refers(User user) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record User(String memberId) {
	}
}
