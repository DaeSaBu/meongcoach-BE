package com.daesabu.meongcoach.llm.adapter.integration;

import com.daesabu.meongcoach.llm.application.provided.PromptTemplateLoader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class ClassPathPromptTemplateLoader implements PromptTemplateLoader {

	private static final String INCLUDE_PREFIX = "{{include:";
	private static final Pattern INCLUDE_DIRECTIVE = Pattern.compile("\\{\\{include:([a-z0-9/-]+)}}");
	private static final String FRAGMENT_LOCATION_FORMAT = "prompts/%s.md";

	@Override
	public String load(String location) {
		String template = read(location);
		String resolved = INCLUDE_DIRECTIVE.matcher(template).replaceAll(this::readFragment);
		if (resolved.contains(INCLUDE_PREFIX)) {
			throw new IllegalStateException("프롬프트의 include 지시문을 풀 수 없습니다: " + location);
		}
		return resolved;
	}

	private String readFragment(MatchResult directive) {
		String fragment = read(FRAGMENT_LOCATION_FORMAT.formatted(directive.group(1)));
		return Matcher.quoteReplacement(fragment);
	}

	private String read(String location) {
		try {
			String content = new ClassPathResource(location).getContentAsString(StandardCharsets.UTF_8);
			if (content.isBlank()) {
				throw new IllegalStateException("프롬프트 파일이 비어 있습니다: " + location);
			}
			return content.strip();
		} catch (IOException e) {
			throw new IllegalStateException("프롬프트 파일을 읽을 수 없습니다: " + location, e);
		}
	}
}
