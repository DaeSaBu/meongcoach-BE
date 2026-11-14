package com.daesabu.meongcoach.support.application.provided;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

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
import com.daesabu.meongcoach.support.application.provided.dto.ChatMessageRequest;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserRole;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@NonTransactionalApplicationTest
@Sql({
    "/db/migration/V50__create_support_chat.sql",
    "/db/migration/V51__create_support_push_jobs.sql",
    "/db/migration/V52__drop_support_socket_tickets.sql",
    "/db/migration/V53__drop_support_push_jobs.sql"
})
@Sql(statements = {"DROP TABLE support_messages", "DROP TABLE support_conversations"},
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ChatReplyNotificationTest {
    @Autowired ChatRegister chats;
    @Autowired ChatFinder finder;
    @Autowired UserRepository users;
    @Autowired DeviceInstallationRepository installations;
    @Autowired NotificationEventSettingRepository settings;
    @Autowired NotificationSender sender;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    private final List<Long> userIds = new ArrayList<>();

    @BeforeEach
    void 발송에_성공한다() {
        when(sender.send(anyList())).thenReturn(new NotificationDelivery(1, 0, List.of()));
    }

	private Long member(boolean admin) {
		var user = User.registerUser();
		if (admin) {
			ReflectionTestUtils.setField(user, "role", UserRole.ADMIN);
		}
		Long id = users.save(user).getId();
		userIds.add(id);
		return id;
	}

	private Long recipient(boolean enabled) {
		Long user = member(false);
		chats.open(user);
		settings.save(
				NotificationEventSetting.create(
						user, NotificationEventType.SUPPORT_REPLY, enabled));
		installations.save(
				DeviceInstallation.register(
						user,
						new DeviceInstallationRegisterCommand(
								UUID.randomUUID().toString(), DevicePlatform.IOS),
						Instant.now()));
		return user;
	}

	@AfterEach
	void 정리한다() {
		jdbc.update("DELETE FROM support_messages");
		jdbc.update("DELETE FROM support_conversations");
		new TransactionTemplate(transactions)
				.executeWithoutResult(
						status ->
								userIds.forEach(
										id -> {
											installations.deleteAllByUserId(id);
											settings.deleteAllByUserId(id);
											users.deleteById(id);
										}));
	}

    @Test
    void 답변_커밋_후_현재_요청에서_한번_발송한다() {
        Long user = recipient(true);
        Long admin = member(true);
        Thread requestThread = Thread.currentThread();
        when(sender.send(anyList())).thenAnswer(invocation -> {
            assertThat(Thread.currentThread()).isSameAs(requestThread);
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM support_messages", Long.class)).isEqualTo(1);
            return new NotificationDelivery(1, 0, List.of());
        });
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            chats.send(admin, user, new ChatMessageRequest(UUID.randomUUID(), "답변"));
            verifyNoInteractions(sender);
        });
        verify(sender).send(argThat(messages -> messages.size() == 1
                && messages.getFirst().type() == NotificationEventType.SUPPORT_REPLY
                && messages.getFirst().deepLink().value().equals("meongcoach://support-chat")));
    }

    @Test
    void 롤백된_답변은_발송하지_않는다() {
        Long user = recipient(true);
        Long admin = member(true);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            chats.send(admin, user, new ChatMessageRequest(UUID.randomUUID(), "롤백"));
            status.setRollbackOnly();
        });
        assertThat(finder.messages(user, user, 0, 0)).isEmpty();
        verifyNoInteractions(sender);
    }

    @Test
    void 같은_UUID_답변은_저장과_발송을_반복하지_않는다() {
        Long user = recipient(true);
        Long admin = member(true);
        var request = new ChatMessageRequest(UUID.randomUUID(), "답변");
        var first = chats.send(admin, user, request);
        assertThat(chats.send(admin, user, request)).isEqualTo(first);
        assertThat(finder.messages(user, user, 0, 0)).containsExactly(first);
        verify(sender).send(anyList());
    }

    @Test
    void 푸시_예외가_발생해도_답변을_반환하고_재전송해도_푸시는_재시도하지_않는다() {
        Long user = recipient(true);
        Long admin = member(true);
        var request = new ChatMessageRequest(UUID.randomUUID(), "답변");
        when(sender.send(anyList())).thenThrow(new IllegalStateException("발송 실패"));
        var saved = chats.send(admin, user, request);
        assertThat(finder.messages(user, user, 0, 0)).containsExactly(saved);
        assertThat(chats.send(admin, user, request)).isEqualTo(saved);
        verify(sender).send(anyList());
    }

    @Test
    void 일부_기기_발송_실패도_재시도하지_않는다() {
        Long user = recipient(true);
        Long admin = member(true);
        var request = new ChatMessageRequest(UUID.randomUUID(), "답변");
        when(sender.send(anyList())).thenReturn(new NotificationDelivery(1, 1, List.of()));
        var saved = chats.send(admin, user, request);
        assertThat(chats.send(admin, user, request)).isEqualTo(saved);
        verify(sender).send(anyList());
    }

    @Test
    void 알림을_끈_회원에게는_발송하지_않는다() {
        Long user = recipient(false);
        chats.send(member(true), user, new ChatMessageRequest(UUID.randomUUID(), "답변"));
        verifyNoInteractions(sender);
    }

    @Test
    void 회원의_자기_메시지는_발송하지_않는다() {
        Long user = recipient(true);
        chats.send(user, user, new ChatMessageRequest(UUID.randomUUID(), "문의"));
        verifyNoInteractions(sender);
    }

    @Test
    void 영구_무효_기기는_제거하고_답변은_유지한다() {
        Long user = recipient(true);
        Long admin = member(true);
        when(sender.send(anyList())).thenAnswer(invocation -> {
            List<com.daesabu.meongcoach.notification.domain.PushNotification> messages = invocation.getArgument(0);
            return new NotificationDelivery(0, 0, List.of(messages.getFirst().fid()));
        });
        var saved = chats.send(admin, user, new ChatMessageRequest(UUID.randomUUID(), "답변"));
        assertThat(finder.messages(user, user, 0, 0)).containsExactly(saved);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM device_installations WHERE user_id=?", Long.class, user)).isZero();
        verify(sender).send(anyList());
    }
}
