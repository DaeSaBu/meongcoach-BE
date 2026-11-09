package com.daesabu.meongcoach.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SupportReplyTest {
	@Test
	void 상담원_답변의_수신자와_본문을_결정한다() {
		SupportReply reply = reply(" 답변 ", "42", null, false, false);

		assertThat(reply.recipientUserId()).hasValue(42L);
		assertThat(reply.notificationBody()).isEqualTo("답변");
	}

	@ParameterizedTest
	@CsvSource({"upsert,message,userChat,manager", "push,userChat,userChat,manager",
			"push,message,group,manager", "push,message,userChat,user", "push,message,userChat,bot"})
	void 상담원_답변이_아니면_발송하지_않는다(String event, String type, String chatType, String senderType) {
		SupportReply reply = new SupportReply(event, type, chatType, senderType, "답변", null, false, false, "42");

		assertThat(reply.recipientUserId()).isEmpty();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"abc", "0", "-1", "01", "1.5", "12345678901234567890"})
	void 유효하지_않은_회원_ID에는_발송하지_않는다(String memberId) {
		assertThat(reply("답변", memberId, null, false, false).recipientUserId()).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {"private", "silent", "silentToUser", "doNotSendApp", "doNotPost"})
	void 알림을_억제한_답변은_발송하지_않는다(String option) {
		assertThat(reply("답변", "42", List.of(option), false, false).recipientUserId()).isEmpty();
	}

	@Test
	void 방송과_마케팅_메시지는_발송하지_않는다() {
		assertThat(reply("답변", "42", null, true, false).recipientUserId()).isEmpty();
		assertThat(reply("답변", "42", null, false, true).recipientUserId()).isEmpty();
	}

	@Test
	void 본문은_백_자까지만_표시하며_이모지를_유지한다() {
		String text = "가".repeat(99) + "🐶";

		assertThat(reply(text, "42", null, false, false).notificationBody()).isEqualTo(text);
		assertThat(reply(text + "답변", "42", null, false, false).notificationBody()).isEqualTo(text + "…");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"  ", "\n"})
	void 글이_없으면_기본_문구를_사용한다(String text) {
		assertThat(reply(text, "42", null, false, false).notificationBody()).isEqualTo("새 메시지가 도착했어요.");
	}

	private SupportReply reply(String text, String memberId, List<String> options, boolean broadcast, boolean marketing) {
		return new SupportReply("push", "message", "userChat", "manager", text, options, broadcast, marketing, memberId);
	}
}
