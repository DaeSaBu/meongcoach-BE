package com.daesabu.meongcoach.notification.application.required;

import com.daesabu.meongcoach.notification.domain.NotificationEventSetting;
import com.daesabu.meongcoach.notification.domain.NotificationEventType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationEventSettingRepository extends JpaRepository<NotificationEventSetting, Long> {

	List<NotificationEventSetting> findAllByUserId(Long userId);

	Optional<NotificationEventSetting> findByUserIdAndType(Long userId, NotificationEventType type);

	@Query("select s.userId from NotificationEventSetting s where s.type = :type and s.enabled = true")
	List<Long> findUserIdsByTypeEnabled(@Param("type") NotificationEventType type);

	@Query("select s.userId from NotificationEventSetting s where s.type = :type and s.enabled = true and s.userId in :userIds")
	List<Long> findUserIdsByTypeEnabledAndUserIdIn(@Param("type") NotificationEventType type, @Param("userIds") Collection<Long> userIds);

	void deleteAllByUserId(Long userId);
}
