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


	private ChannelTalkWebhookPayload payload(String type, String chatType, String personType, String memberId) {
		return new ChannelTalkWebhookPayload("push", type, new Entity(chatType, personType, "답변", null, null, null), new Refers(new User(memberId)));
	}

	@Test
	void 상담_채팅의_매니저_메시지면_회원_ID를_돌려준다() {
		ChannelTalkWebhookPayload payload = payload("message", "userChat", "manager", "42");

		assertThat(payload.recipientUserId()).hasValue(42L);
	}

	@ParameterizedTest
	@ValueSource(strings = {"user", "bot"})
	void 고객이나_봇이_보낸_메시지는_대상이_아니다(String personType) {
		ChannelTalkWebhookPayload payload = payload("message", "userChat", personType, "42");

		assertThat(payload.recipientUserId()).isEmpty();
	}

	@Test
	void 팀_채팅_메시지는_대상이_아니다() {
		ChannelTalkWebhookPayload payload = payload("message", "group", "manager", "42");

		assertThat(payload.recipientUserId()).isEmpty();
	}

	@Test
	void 메시지_이벤트가_아니면_대상이_아니다() {
		ChannelTalkWebhookPayload payload = payload("userChat", "userChat", "manager", "42");

		assertThat(payload.recipientUserId()).isEmpty();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"abc", "0", "-1", "01", "1.5", "12345678901234567890"})
	void 양의_정수_회원_ID가_아니면_대상이_아니다(String memberId) {
		ChannelTalkWebhookPayload payload = payload("message", "userChat", "manager", memberId);

		assertThat(payload.recipientUserId()).isEmpty();
	}

	@Test
	void 메시지나_고객_정보가_빠지면_대상이_아니다() {
		assertThat(new ChannelTalkWebhookPayload("push", "message", null, new Refers(new User("42"))).recipientUserId()).isEmpty();
		assertThat(new ChannelTalkWebhookPayload("push", "message", new Entity("userChat", "manager", "답변", null, null, null), null).recipientUserId()).isEmpty();
		assertThat(new ChannelTalkWebhookPayload("push", "message", new Entity("userChat", "manager", "답변", null, null, null), new Refers(null)).recipientUserId()).isEmpty();
	}

	private ChannelTalkWebhookPayload message(String plainText) {
		return new ChannelTalkWebhookPayload("push", "message", new Entity("userChat", "manager", plainText, null, null, null), null);
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
		assertThat(new ChannelTalkWebhookPayload("push", "message", null, null).notificationBody()).isEqualTo("새 메시지가 도착했어요.");
	}

	@ParameterizedTest
	@ValueSource(strings = {"private", "silent", "silentToUser", "doNotSendApp", "doNotPost"})
	void 내부_메모와_알림_억제_메시지는_보내지_않는다(String option) {
		Entity entity = new Entity("userChat", "manager", "내부 메모", java.util.List.of(option), null, null);
		var payload = new ChannelTalkWebhookPayload("push", "message", entity, new Refers(new User("42")));

		assertThat(payload.recipientUserId()).isEmpty();
	}

	@Test
	void 수정_이벤트와_마케팅_메시지는_보내지_않는다() {
		Entity entity = new Entity("userChat", "manager", "답변", null, null, null);
		Refers refers = new Refers(new User("42"));
		assertThat(new ChannelTalkWebhookPayload("upsert", "message", entity, refers).recipientUserId()).isEmpty();
		Entity broadcast = new Entity("userChat", "manager", "답변", null, true, null);
		assertThat(new ChannelTalkWebhookPayload("push", "message", broadcast, refers).recipientUserId()).isEmpty();
		Entity marketing = new Entity("userChat", "manager", "답변", null, null, new Object());
		assertThat(new ChannelTalkWebhookPayload("push", "message", marketing, refers).recipientUserId()).isEmpty();
	}
}
