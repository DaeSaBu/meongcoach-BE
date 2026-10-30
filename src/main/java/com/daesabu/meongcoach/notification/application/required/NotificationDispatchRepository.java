package com.daesabu.meongcoach.notification.application.required;

import com.daesabu.meongcoach.notification.domain.NotificationDispatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDispatchRepository extends JpaRepository<NotificationDispatch, String> {
}
