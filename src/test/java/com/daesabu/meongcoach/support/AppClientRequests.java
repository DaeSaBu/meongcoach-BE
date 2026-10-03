package com.daesabu.meongcoach.support;

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

public final class AppClientRequests {

	public static final String APP_VERSION_HEADER = "X-App-Version";
	public static final String APP_PLATFORM_HEADER = "X-App-Platform";
	public static final String SUPPORTED_APP_VERSION = "2.0.0";
	public static final String APP_PLATFORM = "ios";

	private AppClientRequests() {
	}

	public static MockHttpServletRequestBuilder appGet(String path) {
		return withAppHeaders(MockMvcRequestBuilders.get(path));
	}

	public static MockHttpServletRequestBuilder appPost(String path) {
		return withAppHeaders(MockMvcRequestBuilders.post(path));
	}

	public static MockHttpServletRequestBuilder appPut(String path) {
		return withAppHeaders(MockMvcRequestBuilders.put(path));
	}

	public static MockHttpServletRequestBuilder appDelete(String path) {
		return withAppHeaders(MockMvcRequestBuilders.delete(path));
	}

	private static MockHttpServletRequestBuilder withAppHeaders(MockHttpServletRequestBuilder request) {
		return request.header(APP_PLATFORM_HEADER, APP_PLATFORM)
				.header(APP_VERSION_HEADER, SUPPORTED_APP_VERSION);
	}
}
