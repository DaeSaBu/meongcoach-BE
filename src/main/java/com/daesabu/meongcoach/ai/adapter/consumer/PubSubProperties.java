package com.daesabu.meongcoach.ai.adapter.consumer;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("meongcoach.ai.pubsub")
public record PubSubProperties(
		@NotBlank String videoSubscription
) {
}
