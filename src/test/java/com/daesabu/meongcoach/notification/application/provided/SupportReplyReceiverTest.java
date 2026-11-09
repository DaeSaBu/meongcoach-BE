package com.daesabu.meongcoach.notification.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.daesabu.meongcoach.notification.application.provided.dto.ChannelTalkWebhookRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceInstallationRegisterRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.NotificationEventSettingUpdateRequest;
import com.daesabu.meongcoach.notification.application.required.NotificationSender;
import com.daesabu.meongcoach.notification.domain.NotificationDelivery;
import com.daesabu.meongcoach.notification.domain.PushNotification;
import com.daesabu.meongcoach.notification.domain.exception.InvalidChannelTalkTokenException;
import com.daesabu.meongcoach.notification.domain.exception.NotificationDeliveryException;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class SupportReplyReceiverTest {
	@Autowired SupportReplyReceiver receiver;
	@Autowired NotificationRegister register;
	@Autowired NotificationSender sender;
	@Autowired UserRepository users;

	private ChannelTalkWebhookRequest request(Long userId, String messageId, List<String> options) {
		return new ChannelTalkWebhookRequest("push", "message",
				new ChannelTalkWebhookRequest.Message(messageId, "userChat", "manager", options, false, null),
				new ChannelTalkWebhookRequest.Refers(new ChannelTalkWebhookRequest.User("local-" + userId)));
	}

	@Test
	void 같은_답변을_여러_번_받아도_한_번만_보내고_문의_화면을_연결한다() {
		Long userId = users.save(User.registerUser()).getId();
		register.registerDeviceInstallation(userId, new DeviceInstallationRegisterRequest("support-fid", "IOS"));
		List<PushNotification> delivered = new ArrayList<>();
		given(sender.send(anyList())).willAnswer(invocation -> {
			List<PushNotification> messages = invocation.getArgument(0);
			delivered.addAll(messages);
			return new NotificationDelivery(messages.size(), 0, List.of());
		});

		receiver.receive("test-channel-talk-token", request(userId, "reply-once", List.of()));
		receiver.receive("test-channel-talk-token", request(userId, "reply-once", List.of()));

		assertThat(delivered).hasSize(1);
		assertThat(delivered.getFirst().fid()).isEqualTo("support-fid");
		assertThat(delivered.getFirst().type().name()).isEqualTo("SUPPORT_REPLY");
		assertThat(delivered.getFirst().deepLink().value()).isEqualTo("meongcoach://support-chat");
	}

	@Test
	void 문의_답변을_끈_회원에게는_보내지_않는다() {
		Long userId = users.save(User.registerUser()).getId();
		register.registerDeviceInstallation(userId, new DeviceInstallationRegisterRequest("disabled-support-fid", "ANDROID"));
		register.updateEventSetting(userId, new NotificationEventSettingUpdateRequest("SUPPORT_REPLY", false));

		receiver.receive("test-channel-talk-token", request(userId, "disabled-reply", List.of()));

		verifyNoInteractions(sender);
	}

	@Test
	void 내부_메모와_잘못된_토큰으로는_보내지_않는다() {
		receiver.receive("test-channel-talk-token", request(42L, "private-reply", List.of("private")));

		assertThatThrownBy(() -> receiver.receive("wrong", request(42L, "bad-token", List.of())))
				.isInstanceOf(InvalidChannelTalkTokenException.class);
		verifyNoInteractions(sender);
	}

	@Test
	void 전송_실패는_웹훅_성공으로_처리하지_않는다() {
		Long userId = users.save(User.registerUser()).getId();
		register.registerDeviceInstallation(userId, new DeviceInstallationRegisterRequest("failed-support-fid", "IOS"));
		given(sender.send(anyList())).willReturn(new NotificationDelivery(0, 1, List.of()));

		assertThatThrownBy(() -> receiver.receive("test-channel-talk-token", request(userId, "failed-reply", List.of())))
				.isInstanceOf(NotificationDeliveryException.class);
	}
}
