package com.daesabu.meongcoach.support.application.provided.dto;

import com.daesabu.meongcoach.support.domain.SupportReplyCommand;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChannelTalkWebhookRequest(String event, String type, Entity entity, Refers refers) {

	public SupportReplyCommand toCommand() {
		String memberId = memberId();
		if (entity == null) {
			return new SupportReplyCommand(event, type, null, null, null, null, false, false, memberId);
		}
		String chatType = entity.chatType();
		String senderType = entity.personType();
		String text = entity.plainText();
		List<String> options = entity.options();
		boolean broadcast = Boolean.TRUE.equals(entity.broadcastedMsg());
		boolean marketing = entity.marketing() != null;
		return new SupportReplyCommand(event, type, chatType, senderType, text, options, broadcast, marketing, memberId);
	}

	private String memberId() {
		if (refers == null || refers.user() == null) {
			return null;
		}
		return refers.user().memberId();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Entity(String chatType, String personType, String plainText, List<String> options, Boolean broadcastedMsg, Object marketing) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Refers(User user) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record User(String memberId) {
	}
}
