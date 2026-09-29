package com.daesabu.meongcoach.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.auth.application.required.TokenProvider;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.Filter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 학습 테스트. 요청이 필터에 도착해서 응답으로 나갈 때까지 EntityManager(영속성 컨텍스트)·트랜잭션·DB 커넥션이
 * 언제 생기고 언제 끝나는지 단계마다 한 줄씩 출력한다. open-in-view가 켜진 경우와 꺼진 경우를 나란히 비교한다.
 *
 * <p>흐름: 필터 → (시큐리티 필터) → 컨트롤러 → 트랜잭션 없는 서비스 → 트랜잭션① → 외부 호출(가정) → 트랜잭션② → 컨트롤러 반환 → 필터.
 * 출력은 IDE 실행 창이나 {@code build/test-results/test/TEST-*OsivTimelineLearningTest*.xml}의 system-out에서 본다.
 */
class OsivTimelineLearningTest {

	private static final String TIMELINE_PATH = "/learning/timeline";

	@Nested
	@SpringBootTest
	@AutoConfigureMockMvc
	@Import(TimelineConfig.class)
	@NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
	class OSIV가_켜져_있으면 {

		@Autowired
		private MockMvc mockMvc;

		@Autowired
		private TokenProvider tokenProvider;

		@Autowired
		private UserRepository userRepository;

		@Autowired
		private Timeline timeline;

		@Test
		void 요청_하나가_EntityManager_하나를_끝까지_쓰고_커넥션은_응답_뒤에_반납된다() throws Exception {
			requestTimeline(mockMvc, tokenProvider, userRepository, timeline);

			timeline.print("open-in-view = true");
			assertThat(timeline.at(Point.FIRST_TRANSACTION_ENDED).entityManagerId())
					.isEqualTo(timeline.at(Point.SECOND_TRANSACTION_ENDED).entityManagerId());
			assertThat(timeline.at(Point.EXTERNAL_CALL).activeConnections()).isEqualTo(1);
			assertThat(timeline.at(Point.FILTER_EXIT).activeConnections()).isZero();
		}
	}

	@Nested
	@SpringBootTest(properties = "spring.jpa.open-in-view=false")
	@AutoConfigureMockMvc
	@Import(TimelineConfig.class)
	@NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
	class OSIV가_꺼져_있으면 {

		@Autowired
		private MockMvc mockMvc;

		@Autowired
		private TokenProvider tokenProvider;

		@Autowired
		private UserRepository userRepository;

		@Autowired
		private Timeline timeline;

		@Test
		void 트랜잭션마다_EntityManager가_새로_생기고_끝나면_커넥션도_반납된다() throws Exception {
			requestTimeline(mockMvc, tokenProvider, userRepository, timeline);

			timeline.print("open-in-view = false");
			assertThat(timeline.at(Point.FIRST_TRANSACTION_QUERIED).entityManagerId())
					.isNotEqualTo(timeline.at(Point.SECOND_TRANSACTION_STARTED).entityManagerId());
			assertThat(timeline.at(Point.EXTERNAL_CALL).entityManagerId()).isEqualTo(Snapshot.NO_ENTITY_MANAGER);
			assertThat(timeline.at(Point.EXTERNAL_CALL).activeConnections()).isZero();
		}
	}

	private static void requestTimeline(MockMvc mockMvc, TokenProvider tokenProvider, UserRepository userRepository,
			Timeline timeline) throws Exception {
		User user = User.registerUser();
		user.promoteToUser();
		Long userId = userRepository.save(user).getId();
		String accessToken = tokenProvider.issue(userId).accessToken();
		timeline.clear();

		mockMvc.perform(post(TIMELINE_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk());
	}

	enum Point {
		FILTER_ENTRY("① 필터 진입 (요청 도착, 시큐리티 필터 전)"),
		CONTROLLER_ENTRY("② 컨트롤러 진입"),
		SERVICE_ENTRY("③ 서비스 진입 (트랜잭션 없음)"),
		FIRST_TRANSACTION_STARTED("④ 트랜잭션① 시작 직후 (쿼리 전)"),
		FIRST_TRANSACTION_QUERIED("⑤ 트랜잭션① 회원 조회 후"),
		FIRST_TRANSACTION_ENDED("⑥ 트랜잭션① 커밋 후"),
		EXTERNAL_CALL("⑦ 외부 API 호출 중 (DB 안 씀)"),
		SECOND_TRANSACTION_STARTED("⑧ 트랜잭션② 시작 직후 (쿼리 전)"),
		SECOND_TRANSACTION_ENDED("⑨ 트랜잭션② 커밋 후"),
		CONTROLLER_RETURN("⑩ 컨트롤러 반환 직전"),
		FILTER_EXIT("⑪ 필터 종료 (응답 반환, 인터셉터 정리 후)");

		private final String label;

		Point(String label) {
			this.label = label;
		}
	}

	@TestConfiguration
	@Import({TimelineController.class, FlowService.class, TransactionalReader.class, Timeline.class})
	static class TimelineConfig {

		// 가장 먼저 실행되는 필터로 요청 도착과 응답 반환 시점을 잡는다. OSIV는 MVC 인터셉터라 이 필터 안쪽에서 열리고 닫힌다
		@Bean
		FilterRegistrationBean<Filter> timelineFilter(Timeline timeline) {
			FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>((request, response, chain) -> {
				timeline.mark(Point.FILTER_ENTRY);
				chain.doFilter(request, response);
				timeline.mark(Point.FILTER_EXIT);
			});
			registration.addUrlPatterns(TIMELINE_PATH);
			registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
			return registration;
		}
	}

	@RestController
	@RequestMapping(TIMELINE_PATH)
	@RequiredArgsConstructor
	static class TimelineController {

		private final FlowService flowService;
		private final Timeline timeline;

		@PostMapping
		void handle(@CurrentUserId Long userId) {
			timeline.mark(Point.CONTROLLER_ENTRY);
			flowService.run(userId);
			timeline.mark(Point.CONTROLLER_RETURN);
		}
	}

	// 트랜잭션을 걸지 않은 서비스. 트랜잭션은 TransactionalReader를 부를 때만 열린다
	@RequiredArgsConstructor
	static class FlowService {

		private final TransactionalReader transactionalReader;
		private final Timeline timeline;

		void run(Long userId) {
			timeline.mark(Point.SERVICE_ENTRY);
			transactionalReader.read(userId, Point.FIRST_TRANSACTION_STARTED, Point.FIRST_TRANSACTION_QUERIED);
			timeline.mark(Point.FIRST_TRANSACTION_ENDED);
			timeline.mark(Point.EXTERNAL_CALL);
			transactionalReader.read(userId, Point.SECOND_TRANSACTION_STARTED, null);
			timeline.mark(Point.SECOND_TRANSACTION_ENDED);
		}
	}

	@RequiredArgsConstructor
	static class TransactionalReader {

		private final UserRepository userRepository;
		private final Timeline timeline;

		@Transactional(readOnly = true)
		public void read(Long userId, Point started, Point queried) {
			timeline.mark(started);
			userRepository.findById(userId);
			if (queried == null) {
				return;
			}
			timeline.mark(queried);
		}
	}

	/**
	 * 시점마다 스레드에 묶인 EntityManager, 진행 중인 트랜잭션, 풀에서 빌려 간 커넥션 수를 기록한다.
	 * OSIV와 JPA 트랜잭션은 EntityManagerFactory를 키로 EntityManager를 스레드에 묶으므로, 묶여 있지 않으면 영속성 컨텍스트가 없는 것이다.
	 */
	@RequiredArgsConstructor
	static class Timeline {

		private final EntityManagerFactory entityManagerFactory;
		private final DataSource dataSource;
		private final List<Snapshot> snapshots = new ArrayList<>();

		void mark(Point point) {
			EntityManager entityManager = boundEntityManager();
			snapshots.add(new Snapshot(point, entityManagerId(entityManager),
					TransactionSynchronizationManager.isActualTransactionActive(), activeConnections(),
					entityCount(entityManager)));
		}

		Snapshot at(Point point) {
			return snapshots.stream()
					.filter(snapshot -> snapshot.point() == point)
					.findFirst()
					.orElseThrow();
		}

		void clear() {
			snapshots.clear();
		}

		void print(String title) {
			System.out.println();
			System.out.println("==================== " + title + " ====================");
			System.out.printf("%-38s | %-12s | %-6s | %-6s | %s%n", "시점", "EntityManager", "트랜잭션", "커넥션",
					"컨텍스트 엔티티 수");
			snapshots.forEach(snapshot -> System.out.printf("%-38s | %-12s | %-6s | %-6d | %d%n",
					snapshot.point().label, snapshot.entityManagerId(), transactionLabel(snapshot),
					snapshot.activeConnections(), snapshot.entityCount()));
			System.out.println();
		}

		private String transactionLabel(Snapshot snapshot) {
			if (snapshot.transactionActive()) {
				return "진행 중";
			}
			return "없음";
		}

		private EntityManager boundEntityManager() {
			EntityManagerHolder holder = (EntityManagerHolder) TransactionSynchronizationManager.getResource(
					entityManagerFactory);
			if (holder == null) {
				return null;
			}
			return holder.getEntityManager();
		}

		private String entityManagerId(EntityManager entityManager) {
			if (entityManager == null) {
				return Snapshot.NO_ENTITY_MANAGER;
			}
			return "EM@" + Integer.toHexString(System.identityHashCode(entityManager));
		}

		private long entityCount(EntityManager entityManager) {
			if (entityManager == null) {
				return 0;
			}
			return entityManager.unwrap(Session.class).getStatistics().getEntityCount();
		}

		private int activeConnections() {
			try {
				return dataSource.unwrap(HikariDataSource.class).getHikariPoolMXBean().getActiveConnections();
			} catch (SQLException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	record Snapshot(Point point, String entityManagerId, boolean transactionActive, int activeConnections,
	                long entityCount) {

		static final String NO_ENTITY_MANAGER = "없음";
	}
}
