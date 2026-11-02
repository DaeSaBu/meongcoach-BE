package com.daesabu.meongcoach.alert.domain;

import com.daesabu.meongcoach.alert.domain.shared.AlertField;
import java.time.Instant;
import java.util.List;

public record Alert(String title, List<AlertField> fields, Instant occurredAt) {

	public Alert {
		fields = List.copyOf(fields);
	}

	public static Alert of(AlertCommand command, Instant occurredAt) {
		return new Alert(command.title(), command.fields(), occurredAt);
	}
}
