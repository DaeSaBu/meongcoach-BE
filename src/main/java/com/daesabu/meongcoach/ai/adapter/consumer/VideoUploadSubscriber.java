package com.daesabu.meongcoach.ai.adapter.consumer;

import com.google.api.gax.batching.FlowControlSettings;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.pubsub.v1.Subscriber;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "meongcoach.ai.pubsub", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VideoUploadSubscriber implements SmartLifecycle {

	private static final long MAX_OUTSTANDING_MESSAGES = 1L;
	private static final int PARALLEL_PULL_COUNT = 1;

	private final Subscriber subscriber;
	private volatile boolean running;

	public VideoUploadSubscriber(PubSubProperties properties, GoogleCredentials credentials,
			VideoUploadPubSubConsumer consumer) {
		this.subscriber = Subscriber.newBuilder(properties.videoSubscription(), consumer)
				.setCredentialsProvider(FixedCredentialsProvider.create(credentials))
				.setFlowControlSettings(FlowControlSettings.newBuilder()
						.setMaxOutstandingElementCount(MAX_OUTSTANDING_MESSAGES)
						.build())
				.setParallelPullCount(PARALLEL_PULL_COUNT)
				.build();
	}

	@Override
	public void start() {
		subscriber.startAsync().awaitRunning();
		running = true;
	}

	@Override
	public void stop() {
		subscriber.stopAsync().awaitTerminated();
		running = false;
	}

	@Override
	public boolean isRunning() {
		return running;
	}
}
