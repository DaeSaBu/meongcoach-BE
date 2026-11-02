package com.daesabu.meongcoach.notification.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

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
import com.daesabu.meongcoach.support.NonTransactionalApplicationTest;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@NonTransactionalApplicationTest
class EventNotificationDispatcherTransactionTest {

	@Autowired EventNotificationDispatcher dispatcher;
	@Autowired DataSource dataSource;
	@Autowired NotificationSender sender;
	@Autowired NotificationRegister register;
	@Autowired UserRepository users;
	@Autowired DeviceInstallationRepository installations;
	@Autowired NotificationEventSettingRepository settings;

	private Long userId;

	@AfterEach
	void tearDown() {
		register.deleteAllByUserId(userId);
		users.deleteById(userId);
	}

	@Test
	void 전송_중에_트랜잭션과_DB_연결을_잡지_않는다() {
		userId = users.save(User.registerUser()).getId();
		installations.save(DeviceInstallation.register(userId, new DeviceInstallationRegisterCommand("transaction-fid", DevicePlatform.IOS), Instant.now()));
		settings.save(NotificationEventSetting.create(userId, NotificationEventType.OPERATOR_MESSAGE, true));
		AtomicReference<Boolean> transactionActive = new AtomicReference<>();
		AtomicInteger activeConnections = new AtomicInteger(-1);
		given(sender.send(anyList())).willAnswer(invocation -> {
			transactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
			activeConnections.set(dataSource.unwrap(HikariDataSource.class).getHikariPoolMXBean().getActiveConnections());
			return new NotificationDelivery(1, 0, List.of());
		});
		EventNotificationDispatchRequest request = new EventNotificationDispatchRequest("OPERATOR_MESSAGE", "ALL", null, "제목", "본문", null);

		dispatcher.dispatch(request);

		assertThat(transactionActive).hasValue(false);
		assertThat(activeConnections).hasValue(0);
	}
}
