package com.daesabu.meongcoach.notification.adapter.integration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class FirebaseNotificationConfig {
	@Bean(destroyMethod = "delete")
	@Lazy
	public FirebaseApp notificationFirebaseApp(@Value("${FIREBASE_SERVICE_ACCOUNT_JSON:}") String serviceAccount) throws IOException {
		GoogleCredentials credentials = credentials(serviceAccount);
		var options = FirebaseOptions.builder().setCredentials(credentials).build();
		return FirebaseApp.initializeApp(options, "notification");
	}

	private GoogleCredentials credentials(String serviceAccount) throws IOException {
		if (serviceAccount.isBlank()) {
			return GoogleCredentials.getApplicationDefault();
		}
		var stream = new ByteArrayInputStream(serviceAccount.getBytes(StandardCharsets.UTF_8));
		return GoogleCredentials.fromStream(stream);
	}

	@Bean
	@Lazy
	public FirebaseMessaging notificationFirebaseMessaging(FirebaseApp notificationFirebaseApp) {
		return FirebaseMessaging.getInstance(notificationFirebaseApp);
	}
}
