package com.daesabu.meongcoach.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationDeepLinkException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PushNotificationTest {

	@Test
	void 앱_스킴이_아닌_딥링크면_대상이_없어도_던진다() {
		assertThatThrownBy(() -> PushNotification.ofEvent(List.of(), NotificationEventType.COMMENT, "제목", "본문", "https://evil.example"))
				.isInstanceOf(InvalidNotificationDeepLinkException.class);
	}

	@Test
	void 종류와_딥링크를_데이터에_넣어_FID별로_만든다() {
		PushNotification notification = PushNotification.ofEvent(
				List.of("fid"), NotificationEventType.OPERATOR_MESSAGE, "제목", "본문", "meongcoach://notice").getFirst();

		assertThat(notification.fid()).isEqualTo("fid");
		assertThat(notification.data()).isEqualTo(Map.of("type", "OPERATOR_MESSAGE", "deepLink", "meongcoach://notice"));
		assertThat(notification.collapseKey()).isNotBlank();
	}

	@Test
	void 딥링크가_없으면_종류만_넣는다() {
		PushNotification notification = PushNotification.ofEvent(
				List.of("fid"), NotificationEventType.COMMENT, "제목", "본문", null).getFirst();

		assertThat(notification.data()).isEqualTo(Map.of("type", "COMMENT"));
	}
}
