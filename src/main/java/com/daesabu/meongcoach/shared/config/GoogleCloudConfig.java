package com.daesabu.meongcoach.shared.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleCloudConfig {

	private static final List<String> SCOPES = List.of(
			"https://www.googleapis.com/auth/cloud-platform",
			"https://www.googleapis.com/auth/devstorage.read_only");

	@Bean
	public GoogleCredentials googleCredentials(GoogleCloudProperties properties) throws IOException {
		byte[] serviceAccountKey = Base64.getMimeDecoder().decode(properties.serviceAccountKey());
		return ServiceAccountCredentials.fromStream(new ByteArrayInputStream(serviceAccountKey))
				.createScoped(SCOPES)
				.createWithQuotaProject(properties.projectId());
	}

	@Bean
	public Storage storage(GoogleCloudProperties properties, GoogleCredentials credentials) {
		return StorageOptions.newBuilder()
				.setProjectId(properties.projectId())
				.setCredentials(credentials)
				.build()
				.getService();
	}
}
