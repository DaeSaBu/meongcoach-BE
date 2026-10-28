package com.daesabu.meongcoach.notification.application.required;

import com.daesabu.meongcoach.notification.domain.DeviceToken;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

	Optional<DeviceToken> findByToken(String token);

	List<DeviceToken> findAllByUserIdInOrderByUserIdAscIdAsc(Collection<Long> userIds);

	void deleteByUserIdAndToken(Long userId, String token);

	void deleteAllByTokenIn(Collection<String> tokens);

	void deleteAllByUserId(Long userId);
}
