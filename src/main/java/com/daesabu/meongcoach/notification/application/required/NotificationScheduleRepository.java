package com.daesabu.meongcoach.notification.application.required;

import com.daesabu.meongcoach.notification.domain.NotificationSchedule;
import com.daesabu.meongcoach.notification.domain.NotificationTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationScheduleRepository extends JpaRepository<NotificationSchedule, Long> {

	List<NotificationSchedule> findAllByUserId(Long userId);

	Optional<NotificationSchedule> findByUserIdAndHourAndMinute(Long userId, int hour, int minute);

	@Query("select p.userId from NotificationSchedule p where p.hour = :#{#time.hour()} and p.minute = :#{#time.minute()} and p.enabled = true")
	List<Long> findUserIdsByTimeEnabled(@Param("time") NotificationTime time);

	void deleteAllByUserId(Long userId);
}
