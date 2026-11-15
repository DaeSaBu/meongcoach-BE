package com.daesabu.meongcoach.analysis.adapter.consumer;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("meongcoach.analysis.pubsub")
public record PubSubProperties(
		@NotBlank String videoSubscription
) {
}
