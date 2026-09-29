package com.daesabu.meongcoach.entitlement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import com.daesabu.meongcoach.entitlement.application.required.EntitlementRepository;
import com.daesabu.meongcoach.entitlement.domain.Entitlement;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.user.application.UserQueryService;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.exception.UserNotFoundException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class EntitlementSyncServiceTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntitlementRepository entitlementRepository;

	// RevenueCat이 돌려줄 활성 이용권 종류. 테스트마다 채운다
	private final Set<EntitlementType> activeTypes = new HashSet<>();
	private final AtomicInteger readCount = new AtomicInteger();

	private EntitlementSyncService service;

	private Long userId;

	@BeforeEach
	void setUp() {
		service = new EntitlementSyncService(new UserQueryService(userRepository), this::readActiveTypes,
				new EntitlementModifyService(entitlementRepository));
		userId = userRepository.save(User.registerUser()).getId();
	}

	private Set<EntitlementType> readActiveTypes(Long requestedUserId) {
		readCount.incrementAndGet();
		return Set.copyOf(activeTypes);
	}

	@Test
	void RevenueCat의_활성_이용권으로_회원의_이용권을_맞춘다() {
		activeTypes.addAll(EnumSet.of(EntitlementType.PUPPY, EntitlementType.JUNIOR));

		service.synchronize(userId);

		assertThat(entitlementRepository.findAllByUserId(userId))
				.extracting(Entitlement::getType)
				.containsExactlyInAnyOrder(EntitlementType.PUPPY, EntitlementType.JUNIOR);
	}

	@Test
	void 환불로_RevenueCat에서_빠진_이용권은_다음_동기화에서_회수한다() {
		activeTypes.add(EntitlementType.PUPPY);
		service.synchronize(userId);
		activeTypes.clear();

		service.synchronize(userId);

		assertThat(entitlementRepository.findAllByUserId(userId)).noneMatch(Entitlement::isActive);
	}

	@Test
	void 탈퇴한_회원이면_이용권을_바꾸지_않는다() {
		User withdrawnUser = userRepository.save(User.registerUser());
		withdrawnUser.withdraw();
		activeTypes.add(EntitlementType.PUPPY);

		service.synchronize(withdrawnUser.getId());

		assertThat(entitlementRepository.findAllByUserId(withdrawnUser.getId())).isEmpty();
	}

	@Test
	void 없는_회원이면_회원_없음_예외를_던진다() {
		assertThatThrownBy(() -> service.synchronize(Long.MAX_VALUE))
				.isInstanceOf(UserNotFoundException.class);
	}

	// 결제 직후 앱 호출과 웹훅이 거의 동시에 오면 둘 다 같은 이용권을 새로 만들려다 한쪽이 유니크 제약에 걸린다
	@Test
	void 동시_부여로_유니크_제약에_걸리면_한_번_다시_맞춘다() {
		EntitlementModifyService entitlementModifyService = mock(EntitlementModifyService.class);
		willThrow(new DataIntegrityViolationException("uk_entitlements_user_id_type"))
				.willDoNothing()
				.given(entitlementModifyService).synchronize(any(), any(), any());
		EntitlementSyncService retryingService = new EntitlementSyncService(new UserQueryService(userRepository),
				this::readActiveTypes, entitlementModifyService);

		retryingService.synchronize(userId);

		then(entitlementModifyService).should(times(2)).synchronize(any(), any(), any());
		assertThat(readCount).hasValue(1);
	}
}
