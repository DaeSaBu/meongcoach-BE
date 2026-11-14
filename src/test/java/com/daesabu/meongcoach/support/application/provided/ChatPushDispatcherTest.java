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
import com.zaxxer.hikari.HikariDataSource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
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
	"/db/migration/V51__create_support_push_jobs.sql", "/db/migration/V52__drop_support_socket_tickets.sql"
})
@Sql(
		statements = {
			"DROP TABLE support_push_jobs",
			"DROP TABLE support_messages",
			"DROP TABLE support_conversations"
		},
		executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ChatPushDispatcherTest {
	@Autowired ChatRegister chats;
	@Autowired ChatPushDispatcher dispatcher;
	@Autowired ChatPushRegister jobs;
	@Autowired UserRepository users;
	@Autowired DeviceInstallationRepository installations;
	@Autowired NotificationEventSettingRepository settings;
	@Autowired NotificationSender sender;
	@Autowired JdbcTemplate jdbc;
	@Autowired PlatformTransactionManager transactions;
	@Autowired DataSource dataSource;
	private final List<Long> userIds = new ArrayList<>();

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

	private long reply(Long user) {
		Long admin = member(true);
		return chats.send(admin, user, new ChatMessageRequest(UUID.randomUUID(), "답변")).id();
	}

	private String state(long message) {
		return jdbc.queryForObject(
				"SELECT state FROM support_push_jobs WHERE message_id=?", String.class, message);
	}

	private void due() {
		jdbc.update(
				"UPDATE support_push_jobs SET available_at=clock_timestamp()-interval '1 second'");
	}

	@AfterEach
	void 정리한다() {
		jdbc.update("DELETE FROM support_push_jobs");
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
	void 원문이_롤백되면_발송_작업도_남지_않는다() {
		Long user = recipient(true);
		Long admin = member(true);
		new TransactionTemplate(transactions)
				.executeWithoutResult(
						status -> {
							chats.send(
									admin, user, new ChatMessageRequest(UUID.randomUUID(), "롤백"));
							status.setRollbackOnly();
						});
		assertThat(jdbc.queryForObject("SELECT count(*) FROM support_push_jobs", Long.class))
				.isZero();
		assertThat(jdbc.queryForObject("SELECT count(*) FROM support_messages", Long.class))
				.isZero();
	}

	@Test
	void 같은_UUID_답변_재전송은_작업을_중복_생성하지_않는다() {
		Long user = recipient(true);
		Long admin = member(true);
		var request = new ChatMessageRequest(UUID.randomUUID(), "답변");
		chats.send(admin, user, request);
		chats.send(admin, user, request);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM support_push_jobs", Long.class))
				.isEqualTo(1);
	}

	@Test
	void 외부_실패는_백오프_후_재시도하고_완료된_작업은_다시_보내지_않는다() {
		long message = reply(recipient(true));
		when(sender.send(anyList()))
				.thenThrow(new IllegalStateException("일시 장애"))
				.thenReturn(new NotificationDelivery(1, 0, List.of()));
		assertThat(dispatcher.dispatchNext()).isTrue();
		assertThat(state(message)).isEqualTo("PENDING");
		assertThat(
						jdbc.queryForObject(
								"SELECT extract(epoch FROM available_at-clock_timestamp()) FROM"
									+ " support_push_jobs",
								Double.class))
				.isBetween(20.0, 30.0);
		assertThat(dispatcher.dispatchNext()).isFalse();
		due();
		assertThat(dispatcher.dispatchNext()).isTrue();
		assertThat(state(message)).isEqualTo("SENT");
		assertThat(dispatcher.dispatchNext()).isFalse();
		verify(sender, times(2)).send(anyList());
	}

	@Test
	void 외부_발송_중에는_DB_트랜잭션과_커넥션을_점유하지_않는다() throws Exception {
		long message = reply(recipient(true));
		var pool = dataSource.unwrap(HikariDataSource.class);
		when(sender.send(anyList()))
				.thenAnswer(
						invocation -> {
							assertThat(
											TransactionSynchronizationManager
													.isActualTransactionActive())
									.isFalse();
							assertThat(pool.getHikariPoolMXBean().getActiveConnections()).isZero();
							return new NotificationDelivery(1, 0, List.of());
						});
		assertThat(dispatcher.dispatchNext()).isTrue();
		assertThat(state(message)).isEqualTo("SENT");
	}

	@Test
	void 부분_발송_실패도_작업을_완료하지_않고_회원_전체를_재시도한다() {
		long message = reply(recipient(true));
		when(sender.send(anyList()))
				.thenReturn(new NotificationDelivery(1, 1, List.of()))
				.thenReturn(new NotificationDelivery(2, 0, List.of()));
		dispatcher.dispatchNext();
		assertThat(state(message)).isEqualTo("PENDING");
		assertThat(jdbc.queryForObject("SELECT last_error FROM support_push_jobs", String.class))
				.isEqualTo("PARTIAL_DELIVERY");
		due();
		dispatcher.dispatchNext();
		assertThat(state(message)).isEqualTo("SENT");
		verify(sender, times(2)).send(anyList());
	}

	@Test
	void 발송_전에_알림을_끄면_외부_발송을_하지_않고_완료한다() {
		Long user = recipient(true);
		long message = reply(user);
		new TransactionTemplate(transactions)
				.executeWithoutResult(
						status ->
								settings.findByUserIdAndType(
												user, NotificationEventType.SUPPORT_REPLY)
										.orElseThrow()
										.change(false));
		dispatcher.dispatchNext();
		assertThat(state(message)).isEqualTo("SENT");
		verifyNoInteractions(sender);
	}

	@Test
	void 두_워커는_잠긴_작업을_건너뛰고_서로_다른_작업을_선점한다() {
		long first = reply(recipient(true));
		long second = reply(recipient(true));
		try (var executor = Executors.newSingleThreadExecutor()) {
			new TransactionTemplate(transactions)
					.executeWithoutResult(
							status -> {
								var claimed = jobs.claim().orElseThrow();
								assertThat(claimed.messageId()).isEqualTo(first);
								var future = executor.submit(jobs::claim);
								try {
									assertThat(
													future.get(2, TimeUnit.SECONDS)
															.orElseThrow()
															.messageId())
											.isEqualTo(second);
								} catch (Exception exception) {
									throw new AssertionError(exception);
								}
							});
		}
	}

	@Test
	void 임대가_만료되면_회수하고_이전_토큰으로_완료하거나_실패처리할_수_없다() {
		long message = reply(recipient(true));
		var old = jobs.claim().orElseThrow();
		jdbc.update(
				"UPDATE support_push_jobs SET lease_until=clock_timestamp()-interval '1 second'");
		assertThat(jobs.complete(old)).isFalse();
		assertThat(jobs.fail(old, "OLD")).isFalse();
		var next = jobs.claim().orElseThrow();
		assertThat(next.leaseToken()).isNotEqualTo(old.leaseToken());
		assertThat(next.attempts()).isEqualTo(2);
		assertThat(jobs.complete(old)).isFalse();
		assertThat(jobs.fail(old, "OLD")).isFalse();
		assertThat(jobs.complete(next)).isTrue();
		assertThat(state(message)).isEqualTo("SENT");
	}

	@Test
	void 마지막_시도_중_서버가_종료되면_임대_만료_후_영구실패로_보관한다() {
		long message = reply(recipient(true));
		jdbc.update("UPDATE support_push_jobs SET attempts=7");
		assertThat(jobs.claim().orElseThrow().attempts()).isEqualTo(8);
		jdbc.update(
				"UPDATE support_push_jobs SET lease_until=clock_timestamp()-interval '1 second'");
		assertThat(jobs.claim()).isEmpty();
		assertThat(state(message)).isEqualTo("DEAD");
		assertThat(jdbc.queryForObject("SELECT last_error FROM support_push_jobs", String.class))
				.isEqualTo("LEASE_EXPIRED_MAX_ATTEMPTS");
	}

	@Test
	void 여덟번_실패한_작업은_자동_재시도하지_않고_보관한다() {
		long message = reply(recipient(true));
		when(sender.send(anyList())).thenThrow(new IllegalStateException("장애"));
		for (int i = 0; i < 8; i++) {
			due();
			assertThat(dispatcher.dispatchNext()).isTrue();
		}
		assertThat(state(message)).isEqualTo("DEAD");
		assertThat(dispatcher.dispatchNext()).isFalse();
		verify(sender, times(8)).send(anyList());
	}
}
