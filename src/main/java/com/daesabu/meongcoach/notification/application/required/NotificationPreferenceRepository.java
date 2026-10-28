package com.daesabu.meongcoach.notification.application.required;

import com.daesabu.meongcoach.notification.domain.NotificationPreference;
import com.daesabu.meongcoach.notification.domain.shared.NotificationSlot;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

	List<NotificationPreference> findAllByUserId(Long userId);

	@Query("select p.userId from NotificationPreference p where p.slot = :slot and p.enabled = true")
	List<Long> findUserIdsBySlotEnabled(@Param("slot") NotificationSlot slot);

	void deleteAllByUserId(Long userId);
}
