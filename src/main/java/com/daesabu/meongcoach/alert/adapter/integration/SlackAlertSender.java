package com.daesabu.meongcoach.alert.adapter.integration;

import com.daesabu.meongcoach.alert.application.required.AlertSender;
import com.daesabu.meongcoach.alert.domain.Alert;
import com.daesabu.meongcoach.alert.domain.exception.AlertDeliveryFailedException;
import com.daesabu.meongcoach.alert.domain.shared.AlertField;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Gatherers;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@ConditionalOnProperty(prefix = "meongcoach.alert.slack", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SlackAlertSender implements AlertSender {

	private static final int FIELDS_PER_SECTION = 10;
	private static final int FIELD_VALUE_MAX_LENGTH = 500;
	private static final String ELLIPSIS = "…";
	private static final DateTimeFormatter OCCURRED_AT_FORMATTER = DateTimeFormatter
			.ofPattern("yyyy-MM-dd HH:mm:ss 'KST'")
			.withZone(ZoneId.of("Asia/Seoul"));

	private final SlackAlertProperties properties;
	private final RestClient restClient;
	private final String environmentName;

	public SlackAlertSender(SlackAlertProperties properties, RestClient.Builder restClientBuilder,
			Environment environment) {
		this.properties = properties;
		this.restClient = restClientBuilder.build();
		this.environmentName = resolveEnvironmentName(environment);
	}

	private static String resolveEnvironmentName(Environment environment) {
		String[] activeProfiles = environment.getActiveProfiles();
		if (activeProfiles.length > 0) {
			return String.join(",", activeProfiles);
		}
		return String.join(",", environment.getDefaultProfiles());
	}

	@Override
	public void send(Alert alert) {
		Map<String, Object> payload = toPayload(alert);
		try {
			restClient.post()
					.uri(properties.graduationWebhookUrl())
					.contentType(MediaType.APPLICATION_JSON)
					.body(payload)
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientResponseException e) {
			throw new AlertDeliveryFailedException("status=" + e.getStatusCode().value());
		} catch (RestClientException e) {
			throw new AlertDeliveryFailedException("reason=" + e.getClass().getSimpleName());
		}
	}

	private Map<String, Object> toPayload(Alert alert) {
		List<Map<String, Object>> blocks = new ArrayList<>();
		blocks.add(headerBlock(alert));
		blocks.addAll(sectionBlocks(alert.fields()));
		blocks.add(contextBlock(alert.occurredAt()));
		return Map.of("text", alert.title(), "blocks", blocks);
	}

	private static Map<String, Object> headerBlock(Alert alert) {
		return Map.of("type", "header", "text", Map.of("type", "plain_text", "text", alert.title()));
	}

	private static List<Map<String, Object>> sectionBlocks(List<AlertField> fields) {
		return fields.stream()
				.gather(Gatherers.windowFixed(FIELDS_PER_SECTION))
				.map(SlackAlertSender::sectionBlock)
				.toList();
	}

	private static Map<String, Object> sectionBlock(List<AlertField> fields) {
		List<Map<String, String>> texts = fields.stream()
				.map(SlackAlertSender::fieldText)
				.toList();
		return Map.of("type", "section", "fields", texts);
	}

	private static Map<String, String> fieldText(AlertField field) {
		String text = "*" + field.name() + "*\n" + truncate(field.value());
		return Map.of("type", "mrkdwn", "text", text);
	}

	private static String truncate(String value) {
		if (value.length() <= FIELD_VALUE_MAX_LENGTH) {
			return value;
		}
		return value.substring(0, FIELD_VALUE_MAX_LENGTH - ELLIPSIS.length()) + ELLIPSIS;
	}

	private Map<String, Object> contextBlock(Instant occurredAt) {
		String text = environmentName + " · " + OCCURRED_AT_FORMATTER.format(occurredAt);
		return Map.of("type", "context", "elements", List.of(Map.of("type", "mrkdwn", "text", text)));
	}
}
