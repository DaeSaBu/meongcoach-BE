package com.daesabu.meongcoach.entitlement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.entitlement.application.required.ActiveEntitlementReader;
import com.daesabu.meongcoach.entitlement.application.required.EntitlementRepository;
import com.daesabu.meongcoach.entitlement.domain.Entitlement;
import com.daesabu.meongcoach.entitlement.domain.exception.EntitlementProviderUnavailableException;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class EntitlementSyncServiceTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntitlementRepository entitlementRepository;

	private final Set<EntitlementType> activeTypes = new HashSet<>();

	private Long userId;

	@BeforeEach
	void setUp() {
		userId = userRepository.save(User.registerUser()).getId();
	}

	private EntitlementSyncService service(ActiveEntitlementReader activeEntitlementReader) {
		return new EntitlementSyncService(activeEntitlementReader, new EntitlementModifyService(entitlementRepository));
	}

	private EntitlementSyncService service() {
		return service(requestedUserId -> Set.copyOf(activeTypes));
	}

	@Test
	void RevenueCat의_활성_이용권으로_회원의_이용권을_맞춘다() {
		activeTypes.addAll(EnumSet.of(EntitlementType.PUPPY, EntitlementType.JUNIOR));

		service().synchronize(userId);

		assertThat(entitlementRepository.findAllByUserId(userId))
				.extracting(Entitlement::getType)
				.containsExactlyInAnyOrder(EntitlementType.PUPPY, EntitlementType.JUNIOR);
	}

	@Test
	void RevenueCat에서_빠진_이용권은_다음_동기화에서_회수한다() {
		activeTypes.add(EntitlementType.PUPPY);
		service().synchronize(userId);
		activeTypes.clear();

		service().synchronize(userId);

		assertThat(entitlementRepository.findAllByUserId(userId)).noneMatch(Entitlement::isActive);
	}

	@Test
	void RevenueCat에서_이용권을_받아_오지_못하면_기존_이용권을_회수하지_않는다() {
		activeTypes.add(EntitlementType.PUPPY);
		service().synchronize(userId);
		EntitlementSyncService unavailableService = service(requestedUserId -> {
			throw new EntitlementProviderUnavailableException();
		});

		assertThatThrownBy(() -> unavailableService.synchronize(userId))
				.isInstanceOf(EntitlementProviderUnavailableException.class);
		assertThat(entitlementRepository.findAllByUserId(userId)).allMatch(Entitlement::isActive);
	}
}
