package com.daesabu.meongcoach.notification.application.required;

import com.daesabu.meongcoach.notification.domain.NotificationSchedule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationScheduleRepository extends JpaRepository<NotificationSchedule, Long> {

	List<NotificationSchedule> findAllByUserId(Long userId);

	Optional<NotificationSchedule> findByUserIdAndHour(Long userId, int hour);

	@Query("select p.userId from NotificationSchedule p where p.hour = :hour and p.enabled = true")
	List<Long> findUserIdsByHourEnabled(@Param("hour") int hour);

	void deleteAllByUserId(Long userId);
}
