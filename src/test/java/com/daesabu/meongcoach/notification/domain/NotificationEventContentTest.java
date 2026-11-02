package com.daesabu.meongcoach.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationDeepLinkException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NotificationEventContentTest {

	@Test
	void 앱_스킴이_아닌_딥링크면_던진다() {
		assertThatThrownBy(() -> new NotificationEventContent("제목", "본문", "https://evil.example"))
				.isInstanceOf(InvalidNotificationDeepLinkException.class);
	}

	@Test
	void 종류와_딥링크를_메시지_데이터에_넣는다() {
		var content = new NotificationEventContent("제목", "본문", "meongcoach://notice");

		PushNotification message = content.toPushNotifications(List.of("fid"), NotificationEventType.OPERATOR_MESSAGE).getFirst();

		assertThat(message.fid()).isEqualTo("fid");
		assertThat(message.data()).isEqualTo(Map.of("type", "OPERATOR_MESSAGE", "deepLink", "meongcoach://notice"));
		assertThat(message.collapseKey()).isNotBlank();
	}

	@Test
	void 딥링크가_없으면_종류만_넣는다() {
		PushNotification message = new NotificationEventContent("제목", "본문", null).toPushNotifications(List.of("fid"), NotificationEventType.COMMENT).getFirst();

		assertThat(message.data()).isEqualTo(Map.of("type", "COMMENT"));
	}
}
