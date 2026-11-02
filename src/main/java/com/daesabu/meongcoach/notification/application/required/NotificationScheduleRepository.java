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

	@Query("select p.userId from NotificationSchedule p where p.hour = :hour and p.minute = :minute and p.enabled = true")
	List<Long> findUserIdsByHourAndMinuteEnabled(@Param("hour") int hour, @Param("minute") int minute);

	default List<Long> findUserIdsByTimeEnabled(NotificationTime time) {
		return findUserIdsByHourAndMinuteEnabled(time.hour(), time.minute());
	}

	void deleteAllByUserId(Long userId);
}
