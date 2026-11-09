package com.daesabu.meongcoach.notification.adapter.webapi.dto;

import com.daesabu.meongcoach.notification.application.provided.dto.SupportChatSessionResult;

public record SupportChatSessionResponse(String memberId, String memberHash) {

	public static SupportChatSessionResponse from(SupportChatSessionResult session) {
		return new SupportChatSessionResponse(session.memberId(), session.memberHash());
	}
}
