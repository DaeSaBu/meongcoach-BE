package com.daesabu.meongcoach.entitlement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daesabu.meongcoach.entitlement.application.required.ActiveEntitlementReader;
import com.daesabu.meongcoach.entitlement.application.required.EntitlementRepository;
import com.daesabu.meongcoach.entitlement.domain.Entitlement;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * open-in-view가 켜진 실제 필터·인터셉터 체인으로 동기화를 실행한다. OSIV에서는 커넥션이 요청 끝까지 반납되지 않으므로,
 * RevenueCat을 부르는 동안 커넥션을 쥐지 않는 것은 DB 접근보다 외부 호출을 먼저 하는 순서로만 보장된다 (learning/OsivConnectionLearningTest).
 * 쓰기 트랜잭션이 프록시로 실제 적용되는지도 회수가 DB에 남는지로 함께 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EntitlementSyncConnectionTest {

	// application-test.yml의 meongcoach.revenuecat.webhook-authorization 값
	private static final String WEBHOOK_AUTHORIZATION = "test-revenuecat-webhook-authorization";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntitlementRepository entitlementRepository;

	@Autowired
	private DataSource dataSource;

	@MockitoBean
	private ActiveEntitlementReader activeEntitlementReader;

	// RevenueCat이 돌려줄 활성 이용권과, 조회 순간 풀에서 빌려 간 커넥션 수
	private final Set<EntitlementType> activeTypes = new HashSet<>();
	private final List<Integer> connectionsDuringRead = new ArrayList<>();

	private Long userId;

	@BeforeEach
	void setUp() {
		userId = userRepository.save(User.registerUser()).getId();
		given(activeEntitlementReader.readActiveTypes(any())).willAnswer(invocation -> {
			connectionsDuringRead.add(activeConnections());
			return Set.copyOf(activeTypes);
		});
	}

	@Test
	void RevenueCat을_조회하는_동안에는_DB_커넥션을_쥐지_않는다() throws Exception {
		activeTypes.add(EntitlementType.PUPPY);

		receiveWebhook();

		assertThat(connectionsDuringRead).containsExactly(0);
	}

	// 쓰기 트랜잭션이 걸리지 않으면 회수는 변경 감지로 저장되지 않고 OSIV가 닫힐 때 버려진다
	@Test
	void 실제_요청으로_동기화하면_부여와_회수가_DB에_남는다() throws Exception {
		activeTypes.add(EntitlementType.PUPPY);
		receiveWebhook();
		activeTypes.clear();

		receiveWebhook();

		assertThat(entitlementRepository.findAllByUserId(userId))
				.singleElement()
				.satisfies(puppy -> assertThat(puppy.getRevokedAt()).isNotNull());
		assertThat(entitlementRepository.findAllByUserId(userId)).noneMatch(Entitlement::isActive);
	}

	private void receiveWebhook() throws Exception {
		mockMvc.perform(post("/api/webhooks/revenuecat")
						.header(HttpHeaders.AUTHORIZATION, WEBHOOK_AUTHORIZATION)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"event": {"type": "NON_RENEWING_PURCHASE", "app_user_id": "%d"}}
								""".formatted(userId)))
				.andExpect(status().isOk());
	}

	private int activeConnections() throws SQLException {
		return dataSource.unwrap(HikariDataSource.class).getHikariPoolMXBean().getActiveConnections();
	}
}
