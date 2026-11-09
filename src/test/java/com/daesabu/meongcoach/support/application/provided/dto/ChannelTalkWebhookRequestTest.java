package com.daesabu.meongcoach.support.application.provided.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.support.domain.SupportReplyCommand;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChannelTalkWebhookRequestTest {
	@Test
	void 웹훅_필드를_상담_답변으로_변환한다() {
		var entity = new ChannelTalkWebhookRequest.Entity("userChat", "manager", "답변", List.of("private"), true, new Object());
		var refers = new ChannelTalkWebhookRequest.Refers(new ChannelTalkWebhookRequest.User("42"));
		var request = new ChannelTalkWebhookRequest("push", "message", entity, refers);

		assertThat(request.toCommand()).isEqualTo(new SupportReplyCommand("push", "message", "userChat", "manager", "답변",
				List.of("private"), true, true, "42"));
	}

	@Test
	void 메시지가_빠져도_판별할_수_있는_객체로_변환한다() {
		var request = new ChannelTalkWebhookRequest("push", "message", null, null);

		assertThat(request.toCommand()).isEqualTo(new SupportReplyCommand("push", "message", null, null, null, null, false, false, null));
	}

	@Test
	void 고객_정보와_선택_필드가_없으면_빈_값을_유지한다() {
		var entity = new ChannelTalkWebhookRequest.Entity("userChat", "manager", "답변", null, null, null);
		var request = new ChannelTalkWebhookRequest("push", "message", entity, new ChannelTalkWebhookRequest.Refers(null));

		assertThat(request.toCommand()).isEqualTo(new SupportReplyCommand("push", "message", "userChat", "manager", "답변",
				null, false, false, null));
	}
}
