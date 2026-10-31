package com.daesabu.meongcoach.notification.application.required;

import com.daesabu.meongcoach.notification.domain.NotificationPreference;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

	List<NotificationPreference> findAllByUserId(Long userId);

	Optional<NotificationPreference> findByUserIdAndHour(Long userId, int hour);

	@Query("select p.userId from NotificationPreference p where p.hour = :hour and p.enabled = true")
	List<Long> findUserIdsByHourEnabled(@Param("hour") int hour);

	void deleteAllByUserId(Long userId);
}
