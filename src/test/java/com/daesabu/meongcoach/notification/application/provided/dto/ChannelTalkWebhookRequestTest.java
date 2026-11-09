package com.daesabu.meongcoach.notification.application.provided.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ChannelTalkWebhookRequestTest {
	private ChannelTalkWebhookRequest request(String event, String type, String chatType, String personType, List<String> options) {
		return new ChannelTalkWebhookRequest(event, type,
				new ChannelTalkWebhookRequest.Message("reply-1", chatType, personType, options, false, null),
				new ChannelTalkWebhookRequest.Refers(new ChannelTalkWebhookRequest.User("dev-42")));
	}

	@Test
	void 상담원의_공개_답변만_받는다() {
		assertThat(request("push", "message", "userChat", "manager", null).reply("dev")).isPresent();
		assertThat(request("upsert", "message", "userChat", "manager", null).reply("dev")).isEmpty();
		assertThat(request("push", "userChat", "userChat", "manager", null).reply("dev")).isEmpty();
		assertThat(request("push", "message", "group", "manager", null).reply("dev")).isEmpty();
		assertThat(request("push", "message", "userChat", "user", null).reply("dev")).isEmpty();
		assertThat(request("push", "message", "userChat", "bot", null).reply("dev")).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {"private", "silent", "silentToUser", "doNotSendApp", "doNotPost"})
	void 내부_메모와_알림_억제_옵션은_보내지_않는다(String option) {
		assertThat(request("push", "message", "userChat", "manager", List.of(option)).reply("dev")).isEmpty();
	}

	@Test
	void 일괄_메시지와_마케팅은_보내지_않는다() {
		var refers = new ChannelTalkWebhookRequest.Refers(new ChannelTalkWebhookRequest.User("dev-42"));
		var broadcast = new ChannelTalkWebhookRequest.Message("reply-1", "userChat", "manager", null, true, null);
		var marketing = new ChannelTalkWebhookRequest.Message("reply-1", "userChat", "manager", null, false, new Object());

		assertThat(new ChannelTalkWebhookRequest("push", "message", broadcast, refers).reply("dev")).isEmpty();
		assertThat(new ChannelTalkWebhookRequest("push", "message", marketing, refers).reply("dev")).isEmpty();
	}

	@Test
	void 익명_사용자나_연결_정보가_없는_이벤트는_건너뛴다() {
		assertThat(new ChannelTalkWebhookRequest("push", "message", null, null).reply("dev")).isEmpty();
		var entity = new ChannelTalkWebhookRequest.Message("reply-1", "userChat", "manager", null, false, null);
		assertThat(new ChannelTalkWebhookRequest("push", "message", entity, new ChannelTalkWebhookRequest.Refers(null)).reply("dev")).isEmpty();
	}
}
