package com.daesabu.meongcoach.alert.domain.shared;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Objects;

public record AlertField(
		@NotBlank @Size(max = 50) String name,
		@NotNull String value
) {

	public static AlertField of(String name, Object value) {
		return new AlertField(name, Objects.toString(value, "-"));
	}
}
