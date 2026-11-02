package com.daesabu.meongcoach.alert.application.provided.dto;

import com.daesabu.meongcoach.alert.domain.AlertCommand;
import com.daesabu.meongcoach.alert.domain.shared.AlertField;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AlertRequest(
		@NotBlank @Size(max = 140) String title,
		@NotNull @Size(max = 20) List<@Valid AlertField> fields
) {

	public static AlertRequest of(String title, AlertField... fields) {
		return new AlertRequest(title, List.of(fields));
	}

	public AlertCommand toCommand() {
		return new AlertCommand(title, fields);
	}
}
