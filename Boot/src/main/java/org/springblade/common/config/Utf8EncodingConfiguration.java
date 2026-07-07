/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Forces UTF-8 before request body wrappers and security filters read payloads.
 */
@Configuration(proxyBeanMethods = false)
public class Utf8EncodingConfiguration {

	@Bean
	public FilterRegistrationBean<CharacterEncodingFilter> utf8CharacterEncodingFilter() {
		CharacterEncodingFilter filter = new CharacterEncodingFilter();
		filter.setEncoding(StandardCharsets.UTF_8.name());
		filter.setForceEncoding(true);

		FilterRegistrationBean<CharacterEncodingFilter> registration = new FilterRegistrationBean<>();
		registration.setFilter(filter);
		registration.setName("utf8CharacterEncodingFilter");
		registration.addUrlPatterns("/*");
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return registration;
	}

	@Bean
	public FilterRegistrationBean<OncePerRequestFilter> utf8JsonRequestBodyFilter() {
		FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>();
		registration.setFilter(new Utf8JsonRequestBodyFilter());
		registration.setName("utf8JsonRequestBodyFilter");
		registration.addUrlPatterns("/*");
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
		return registration;
	}

	private static class Utf8JsonRequestBodyFilter extends OncePerRequestFilter {

		@Override
		protected boolean shouldNotFilter(HttpServletRequest request) {
			String contentType = request.getContentType();
			if (contentType == null) {
				return true;
			}
			String lowerContentType = contentType.toLowerCase(Locale.ROOT);
			return lowerContentType.startsWith("multipart/")
				|| lowerContentType.startsWith("application/x-www-form-urlencoded")
				|| !(lowerContentType.contains("json")
				|| lowerContentType.startsWith("text/")
				|| lowerContentType.contains("xml"));
		}

		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
			filterChain.doFilter(new Utf8CachedBodyRequest(request), response);
		}
	}

	private static class Utf8CachedBodyRequest extends HttpServletRequestWrapper {

		private final byte[] body;

		Utf8CachedBodyRequest(HttpServletRequest request) throws IOException {
			super(request);
			this.body = StreamUtils.copyToByteArray(request.getInputStream());
		}

		@Override
		public ServletInputStream getInputStream() {
			ByteArrayInputStream inputStream = new ByteArrayInputStream(body);
			return new ServletInputStream() {
				@Override
				public boolean isFinished() {
					return inputStream.available() == 0;
				}

				@Override
				public boolean isReady() {
					return true;
				}

				@Override
				public void setReadListener(ReadListener readListener) {
					// Synchronous request processing does not require async callbacks.
				}

				@Override
				public int read() {
					return inputStream.read();
				}
			};
		}

		@Override
		public BufferedReader getReader() {
			String encoding = getCharacterEncoding();
			return new BufferedReader(new InputStreamReader(getInputStream(),
				encoding == null ? StandardCharsets.UTF_8 : java.nio.charset.Charset.forName(encoding)));
		}
	}

}
