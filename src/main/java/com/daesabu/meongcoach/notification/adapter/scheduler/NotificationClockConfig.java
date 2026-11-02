package com.daesabu.meongcoach.notification.adapter.scheduler;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationClockConfig {

	@Bean
	public Clock notificationClock() {
		return Clock.systemUTC();
	}
}
