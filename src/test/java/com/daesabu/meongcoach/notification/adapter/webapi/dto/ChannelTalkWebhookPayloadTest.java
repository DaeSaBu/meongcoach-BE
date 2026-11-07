package com.daesabu.meongcoach.notification.adapter.webapi.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.notification.adapter.webapi.dto.ChannelTalkWebhookPayload.Entity;
import com.daesabu.meongcoach.notification.adapter.webapi.dto.ChannelTalkWebhookPayload.Refers;
import com.daesabu.meongcoach.notification.adapter.webapi.dto.ChannelTalkWebhookPayload.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ChannelTalkWebhookPayloadTest {

	private static final String PREFIX = "development-";

	private ChannelTalkWebhookPayload payload(String type, String chatType, String personType, String memberId) {
		return new ChannelTalkWebhookPayload(type, new Entity(chatType, personType, "답변"), new Refers(new User(memberId)));
	}

	@Test
	void 상담_채팅의_매니저_메시지면_접두어를_뗀_회원_ID를_돌려준다() {
		ChannelTalkWebhookPayload payload = payload("message", "userChat", "manager", "development-42");

		assertThat(payload.recipientUserId(PREFIX)).hasValue(42L);
	}

	@ParameterizedTest
	@ValueSource(strings = {"user", "bot"})
	void 고객이나_봇이_보낸_메시지는_대상이_아니다(String personType) {
		ChannelTalkWebhookPayload payload = payload("message", "userChat", personType, "development-42");

		assertThat(payload.recipientUserId(PREFIX)).isEmpty();
	}

	@Test
	void 팀_채팅_메시지는_대상이_아니다() {
		ChannelTalkWebhookPayload payload = payload("message", "group", "manager", "development-42");

		assertThat(payload.recipientUserId(PREFIX)).isEmpty();
	}

	@Test
	void 메시지_이벤트가_아니면_대상이_아니다() {
		ChannelTalkWebhookPayload payload = payload("userChat", "userChat", "manager", "development-42");

		assertThat(payload.recipientUserId(PREFIX)).isEmpty();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"production-42", "42", "development-", "development-abc", "development--1", "development-12345678901234567890"})
	void 이_환경의_회원_ID가_아니면_대상이_아니다(String memberId) {
		ChannelTalkWebhookPayload payload = payload("message", "userChat", "manager", memberId);

		assertThat(payload.recipientUserId(PREFIX)).isEmpty();
	}

	@Test
	void 메시지나_고객_정보가_빠지면_대상이_아니다() {
		assertThat(new ChannelTalkWebhookPayload("message", null, new Refers(new User("development-42"))).recipientUserId(PREFIX)).isEmpty();
		assertThat(new ChannelTalkWebhookPayload("message", new Entity("userChat", "manager", "답변"), null).recipientUserId(PREFIX)).isEmpty();
		assertThat(new ChannelTalkWebhookPayload("message", new Entity("userChat", "manager", "답변"), new Refers(null)).recipientUserId(PREFIX)).isEmpty();
	}

	private ChannelTalkWebhookPayload message(String plainText) {
		return new ChannelTalkWebhookPayload("message", new Entity("userChat", "manager", plainText), null);
	}

	@Test
	void 알림_본문은_앞뒤_공백을_지운_메시지다() {
		assertThat(message("  안녕하세요, 훈련사입니다.\n").notificationBody()).isEqualTo("안녕하세요, 훈련사입니다.");
	}

	@Test
	void 백_자를_넘는_메시지는_백_자까지_자르고_말줄임표를_붙인다() {
		assertThat(message("가".repeat(100)).notificationBody()).isEqualTo("가".repeat(100));
		assertThat(message("가".repeat(101)).notificationBody()).isEqualTo("가".repeat(100) + "…");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"  ", "\n"})
	void 글이_없는_메시지는_기본_문구를_쓴다(String plainText) {
		assertThat(message(plainText).notificationBody()).isEqualTo("새 메시지가 도착했어요.");
		assertThat(new ChannelTalkWebhookPayload("message", null, null).notificationBody()).isEqualTo("새 메시지가 도착했어요.");
	}
}
