package com.daesabu.meongcoach.notification.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.daesabu.meongcoach.notification.application.provided.dto.EventNotificationDispatchRequest;
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
import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationEventTypeException;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class EventNotificationDispatcherTest {

	@Autowired EventNotificationDispatcher dispatcher;
	@Autowired NotificationSender sender;
	@Autowired UserRepository users;
	@Autowired DeviceInstallationRepository installations;
	@Autowired NotificationEventSettingRepository settings;

	private Long saveUser(String fid, boolean enabled) {
		Long userId = users.save(User.registerUser()).getId();
		installations.save(DeviceInstallation.register(userId, new DeviceInstallationRegisterCommand(fid, DevicePlatform.IOS), Instant.now()));
		settings.save(NotificationEventSetting.create(userId, NotificationEventType.OPERATOR_MESSAGE, enabled));
		return userId;
	}

	private List<String> capture() {
		List<String> fids = new ArrayList<>();
		given(sender.send(anyList())).willAnswer(invocation -> {
			List<PushNotification> messages = invocation.getArgument(0);
			messages.forEach(message -> fids.add(message.fid()));
			return new NotificationDelivery(messages.size(), 0, List.of());
		});
		return fids;
	}

	private EventNotificationDispatchRequest request(String kind, List<Long> userIds) {
		return new EventNotificationDispatchRequest("OPERATOR_MESSAGE", kind, userIds, "제목", "본문", null);
	}

	@Test
	void ALL은_해당_종류를_켠_회원에게만_보낸다() {
		saveUser("on-fid", true);
		saveUser("off-fid", false);
		List<String> fids = capture();

		assertThat(dispatcher.dispatch(request("ALL", null)).fids()).isEqualTo(1);

		assertThat(fids).containsExactly("on-fid");
	}

	@Test
	void USERS는_지정한_회원_중_켠_회원에게만_보내고_없는_회원은_건너뛴다() {
		Long on = saveUser("on-fid", true);
		Long off = saveUser("off-fid", false);
		saveUser("other-fid", true);
		List<String> fids = capture();

		assertThat(dispatcher.dispatch(request("USERS", List.of(on, off, 999_999L))).fids()).isEqualTo(1);

		assertThat(fids).containsExactly("on-fid");
	}

	@Test
	void 설정을_껐거나_행이_없으면_FCM을_호출하지_않는다() {
		Long off = saveUser("off-fid", false);

		assertThat(dispatcher.dispatch(request("USERS", List.of(off))).fids()).isZero();

		verifyNoInteractions(sender);
	}

	@Test
	void 같은_요청을_다시_보내면_다시_발송한다() {
		saveUser("on-fid", true);
		capture();

		dispatcher.dispatch(request("ALL", null));
		dispatcher.dispatch(request("ALL", null));

		verify(sender, times(2)).send(anyList());
	}

	@Test
	void 없는_종류면_던진다() {
		var request = new EventNotificationDispatchRequest("MARKETING", "ALL", null, "제목", "본문", null);

		assertThatThrownBy(() -> dispatcher.dispatch(request)).isInstanceOf(InvalidNotificationEventTypeException.class);
	}
}
