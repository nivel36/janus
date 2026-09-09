/*
 * Copyright 2026 Abel Ferrer Jiménez
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package es.nivel36.janus.config;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import es.nivel36.janus.service.TimeFormat;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Defaults applied whenever Janus provisions an application user. */
@Validated
@ConfigurationProperties("janus.user-provisioning.defaults")
public class UserProvisioningProperties {

	@NotBlank
	@Pattern(regexp = "^[a-z]{2,3}-[A-Z]{2}$")
	private String locale;

	@NotNull
	private TimeFormat timeFormat;

	@NotBlank
	private String defaultTimezone;

	public String getLocale() {
		return this.locale;
	}

	public void setLocale(final String locale) {
		this.locale = locale;
	}

	public TimeFormat getTimeFormat() {
		return this.timeFormat;
	}

	public void setTimeFormat(final TimeFormat timeFormat) {
		this.timeFormat = timeFormat;
	}

	public String getDefaultTimezone() {
		return this.defaultTimezone;
	}

	public void setDefaultTimezone(final String defaultTimezone) {
		this.defaultTimezone = defaultTimezone;
	}

	public Locale locale() {
		return Locale.forLanguageTag(this.locale);
	}

	public ZoneId defaultTimezone() {
		return ZoneId.of(this.defaultTimezone);
	}

	@AssertTrue(message = "must identify a supported locale")
	boolean isLocaleSupported() {
		return this.locale == null || Locale.availableLocales()
				.anyMatch(candidate -> candidate.toLanguageTag().equals(this.locale));
	}

	@AssertTrue(message = "must be a valid timezone identifier")
	boolean isDefaultTimezoneValid() {
		if (this.defaultTimezone == null) {
			return true;
		}
		try {
			ZoneId.of(this.defaultTimezone);
			return true;
		} catch (final DateTimeException invalidTimezone) {
			return false;
		}
	}
}
