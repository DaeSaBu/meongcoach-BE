package com.daesabu.meongcoach.support.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.daesabu.meongcoach.notification.application.required.DeviceInstallationRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationEventSettingRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationSender;
import com.daesabu.meongcoach.notification.domain.DeviceInstallation;
import com.daesabu.meongcoach.notification.domain.DeviceInstallationRegisterCommand;
import com.daesabu.meongcoach.notification.domain.DevicePlatform;
import com.daesabu.meongcoach.notification.domain.NotificationDelivery;
import com.daesabu.meongcoach.notification.domain.NotificationEventSetting;
import com.daesabu.meongcoach.notification.domain.NotificationEventType;
import com.daesabu.meongcoach.notification.domain.PushNotification;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.support.application.provided.dto.ChannelTalkWebhookRequest;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class SupportReplyNotifierTest {
	@Autowired SupportReplyNotifier notifier;
	@Autowired NotificationSender sender;
	@Autowired UserRepository users;
	@Autowired DeviceInstallationRepository installations;
	@Autowired NotificationEventSettingRepository settings;

	@Test
	void 상담원_답변을_수신_회원의_기기로_발송한다() {
		Long userId = saveUser(true);
		given(sender.send(anyList())).willReturn(new NotificationDelivery(1, 0, List.of()));

		notifier.notifyReply(request(userId, "manager"));

		ArgumentCaptor<List<PushNotification>> captor = ArgumentCaptor.captor();
		verify(sender).send(captor.capture());
		assertThat(captor.getValue()).singleElement().satisfies(message -> {
			assertThat(message.fid()).isEqualTo("support-fid");
			assertThat(message.type()).isEqualTo(NotificationEventType.SUPPORT_REPLY);
			assertThat(message.title()).isEqualTo("문의하신 내용에 답변이 왔어요");
			assertThat(message.body()).isEqualTo("답변");
			assertThat(message.deepLink().value()).isEqualTo("meongcoach://support-chat");
		});
	}

	@Test
	void 고객_메시지는_발송하지_않는다() {
		Long userId = saveUser(true);

		notifier.notifyReply(request(userId, "user"));

		verifyNoInteractions(sender);
	}

	@Test
	void 문의_답변_수신을_끄면_발송하지_않는다() {
		Long userId = saveUser(false);

		notifier.notifyReply(request(userId, "manager"));

		verifyNoInteractions(sender);
	}

	private Long saveUser(boolean enabled) {
		Long userId = users.save(User.registerUser()).getId();
		installations.save(DeviceInstallation.register(userId,
				new DeviceInstallationRegisterCommand("support-fid", DevicePlatform.IOS), Instant.now()));
		settings.save(NotificationEventSetting.create(userId, NotificationEventType.SUPPORT_REPLY, enabled));
		return userId;
	}

	private ChannelTalkWebhookRequest request(Long userId, String senderType) {
		var entity = new ChannelTalkWebhookRequest.Entity("userChat", senderType, " 답변 ", null, null, null);
		var refers = new ChannelTalkWebhookRequest.Refers(new ChannelTalkWebhookRequest.User(userId.toString()));
		return new ChannelTalkWebhookRequest("push", "message", entity, refers);
	}
}
