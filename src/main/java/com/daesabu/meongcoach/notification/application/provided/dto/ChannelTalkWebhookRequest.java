package com.daesabu.meongcoach.notification.application.provided.dto;

import com.daesabu.meongcoach.notification.domain.SupportReply;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChannelTalkWebhookRequest(String event, String type, Message entity, Refers refers) {
	public Optional<SupportReply> reply(String environment) {
		if (!"push".equals(event) || !"message".equals(type) || entity == null || refers == null || refers.user() == null) {
			return Optional.empty();
		}
		if (!entity.isPublicReply()) {
			return Optional.empty();
		}
		String memberId = refers.user().memberId();
		String messageId = entity.id();
		return SupportReply.from(memberId, messageId, environment);
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Message(String id, String chatType, String personType, List<String> options, Boolean broadcastedMsg, Object marketing) {
		private static final List<String> SUPPRESSED_OPTIONS = List.of("private", "silent", "silentToUser", "doNotSendApp", "doNotPost");

		public boolean isPublicReply() {
			if (!"userChat".equals(chatType) || !"manager".equals(personType) || Boolean.TRUE.equals(broadcastedMsg) || marketing != null) {
				return false;
			}
			if (options == null) {
				return true;
			}
			return options.stream().noneMatch(SUPPRESSED_OPTIONS::contains);
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Refers(User user) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record User(String memberId) {
	}
}
