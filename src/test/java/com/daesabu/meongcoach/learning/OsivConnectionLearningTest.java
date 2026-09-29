package com.daesabu.meongcoach.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.auth.application.required.TokenProvider;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import com.daesabu.meongcoach.user.application.provided.UserFinder;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 학습 테스트. open-in-view(OSIV)가 켜진 이 애플리케이션에서 영속성 컨텍스트가 언제 열리고 DB 커넥션을 언제 빌려 언제 돌려주는지 확인한다.
 * 운영 코드의 호출 순서에 기대지 않도록 테스트 전용 컨트롤러·서비스로 "DB 먼저"와 "외부 호출 먼저"를 재현한다.
 *
 * <p>확인하는 사실
 * <ul>
 *     <li>OSIV는 컨트롤러 진입 전에 영속성 컨텍스트를 열지만, 커넥션은 트랜잭션이 시작될 때 처음 빌린다.</li>
 *     <li>OSIV 요청 안에서는 트랜잭션이 커밋돼도 커넥션을 돌려주지 않고, 요청이 끝날 때 돌려준다.</li>
 *     <li>그래서 DB를 처음 건드리기 전에 외부 호출을 끝내면, 외부 호출 동안 커넥션을 쥐지 않는다.</li>
 *     <li>OSIV가 없는 곳(웹 요청 밖)에서는 트랜잭션이 끝나면 커넥션도 돌아온다.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
class OsivConnectionLearningTest {

	private static final String CONTROLLER_ENTRY = "컨트롤러 진입";
	private static final String AFTER_DB_TRANSACTION = "DB 조회 트랜잭션 커밋 후";
	private static final String DURING_EXTERNAL_CALL = "외부 호출 중";
	private static final String INSIDE_TRANSACTION_BEFORE_QUERY = "트랜잭션 시작 직후, 쿼리 전";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TokenProvider tokenProvider;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProbeService probeService;

	@Autowired
	private Probe probe;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private Long userId;

	private String accessToken;

	@BeforeEach
	void setUp() {
		User user = User.registerUser();
		user.promoteToUser();
		userId = userRepository.save(user).getId();
		accessToken = tokenProvider.issue(userId).accessToken();
		probe.clear();
	}

	@Test
	void OSIV_요청에서_DB를_먼저_쓰면_트랜잭션이_커밋돼도_외부_호출_동안_커넥션을_쥐고_있다() throws Exception {
		mockMvc.perform(post("/learning/osiv/db-first")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk());

		// 영속성 컨텍스트는 컨트롤러 전에 열리지만, 커넥션은 아직 빌리지 않았다
		assertThat(probe.at(CONTROLLER_ENTRY)).isEqualTo(new Snapshot(CONTROLLER_ENTRY, true, 0, 0));
		// 트랜잭션은 끝났지만 영속성 컨텍스트가 요청 끝까지 살아 있어 조회한 회원과 커넥션을 계속 쥔다
		Snapshot afterDb = probe.at(AFTER_DB_TRANSACTION);
		assertThat(afterDb.persistenceContextBound()).isTrue();
		assertThat(afterDb.entityCount()).isPositive();
		assertThat(afterDb.activeConnections()).isEqualTo(1);
		// DB를 쓰지 않는 외부 호출 구간에서도 커넥션은 반납되지 않는다
		assertThat(probe.at(DURING_EXTERNAL_CALL).activeConnections()).isEqualTo(1);
		// 요청이 끝나 OSIV가 영속성 컨텍스트를 닫을 때 돌려준다
		assertThat(probe.activeConnections()).isZero();
	}

	@Test
	void OSIV_요청에서_외부_호출을_DB보다_먼저_하면_외부_호출_동안_커넥션을_쥐지_않는다() throws Exception {
		mockMvc.perform(post("/learning/osiv/external-first")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk());

		assertThat(probe.at(DURING_EXTERNAL_CALL)).isEqualTo(new Snapshot(DURING_EXTERNAL_CALL, true, 0, 0));
		assertThat(probe.at(AFTER_DB_TRANSACTION).activeConnections()).isEqualTo(1);
		assertThat(probe.activeConnections()).isZero();
	}

	@Test
	void OSIV가_없는_웹_요청_밖에서는_트랜잭션이_끝나면_영속성_컨텍스트와_커넥션도_함께_정리된다() {
		probeService.dbThenExternal(userId);

		assertThat(probe.at(AFTER_DB_TRANSACTION)).isEqualTo(new Snapshot(AFTER_DB_TRANSACTION, false, 0, 0));
		assertThat(probe.at(DURING_EXTERNAL_CALL).activeConnections()).isZero();
	}

	@Test
	void 커넥션은_첫_쿼리가_아니라_트랜잭션을_시작할_때_빌린다() {
		TransactionTemplate readOnlyTransaction = new TransactionTemplate(transactionManager);
		readOnlyTransaction.setReadOnly(true);

		readOnlyTransaction.executeWithoutResult(status -> probe.record(INSIDE_TRANSACTION_BEFORE_QUERY));

		assertThat(probe.at(INSIDE_TRANSACTION_BEFORE_QUERY).activeConnections()).isEqualTo(1);
		assertThat(probe.activeConnections()).isZero();
	}

	@TestConfiguration
	@Import({ProbeController.class, ProbeService.class, Probe.class})
	static class ProbeConfig {
	}

	@RestController
	@RequestMapping("/learning/osiv")
	@RequiredArgsConstructor
	static class ProbeController {

		private final ProbeService probeService;
		private final Probe probe;

		@PostMapping("/db-first")
		void dbFirst(@CurrentUserId Long userId) {
			probe.record(CONTROLLER_ENTRY);
			probeService.dbThenExternal(userId);
		}

		@PostMapping("/external-first")
		void externalFirst(@CurrentUserId Long userId) {
			probe.record(CONTROLLER_ENTRY);
			probeService.externalThenDb(userId);
		}
	}

	// 트랜잭션을 걸지 않은 서비스. isActiveUser는 UserQueryService의 읽기 전용 트랜잭션 안에서 실행된다
	@RequiredArgsConstructor
	static class ProbeService {

		private final UserFinder userFinder;
		private final Probe probe;

		void dbThenExternal(Long userId) {
			userFinder.isActiveUser(userId);
			probe.record(AFTER_DB_TRANSACTION);
			probe.record(DURING_EXTERNAL_CALL);
		}

		void externalThenDb(Long userId) {
			probe.record(DURING_EXTERNAL_CALL);
			userFinder.isActiveUser(userId);
			probe.record(AFTER_DB_TRANSACTION);
		}
	}

	/**
	 * 호출 시점의 영속성 컨텍스트와 커넥션 상태를 기록한다.
	 * OSIV와 JPA 트랜잭션은 EntityManagerFactory를 키로 EntityManager를 스레드에 묶으므로, 묶여 있으면 영속성 컨텍스트가 열려 있는 것이다.
	 */
	@RequiredArgsConstructor
	static class Probe {

		private final EntityManagerFactory entityManagerFactory;
		private final DataSource dataSource;
		private final List<Snapshot> snapshots = new ArrayList<>();

		void record(String point) {
			snapshots.add(new Snapshot(point, isPersistenceContextBound(), entityCount(), activeConnections()));
		}

		Snapshot at(String point) {
			return snapshots.stream()
					.filter(snapshot -> snapshot.point().equals(point))
					.findFirst()
					.orElseThrow();
		}

		void clear() {
			snapshots.clear();
		}

		int activeConnections() {
			try {
				return dataSource.unwrap(HikariDataSource.class).getHikariPoolMXBean().getActiveConnections();
			} catch (SQLException e) {
				throw new IllegalStateException(e);
			}
		}

		private boolean isPersistenceContextBound() {
			return TransactionSynchronizationManager.hasResource(entityManagerFactory);
		}

		private long entityCount() {
			if (!isPersistenceContextBound()) {
				return 0;
			}
			EntityManagerHolder holder = (EntityManagerHolder) TransactionSynchronizationManager.getResource(
					entityManagerFactory);
			return holder.getEntityManager().unwrap(Session.class).getStatistics().getEntityCount();
		}
	}

	record Snapshot(String point, boolean persistenceContextBound, long entityCount, int activeConnections) {
	}
}
