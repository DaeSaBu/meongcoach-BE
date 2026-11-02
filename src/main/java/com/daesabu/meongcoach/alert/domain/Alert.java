package com.daesabu.meongcoach.alert.domain;

import com.daesabu.meongcoach.alert.domain.shared.AlertField;
import com.daesabu.meongcoach.alert.domain.shared.AlertLevel;
import java.time.Instant;
import java.util.List;

public record Alert(AlertLevel level, String title, List<AlertField> fields, Instant occurredAt) {

	public Alert {
		fields = List.copyOf(fields);
	}

	public static Alert of(AlertCommand command, Instant occurredAt) {
		return new Alert(command.level(), command.title(), command.fields(), occurredAt);
	}
}
