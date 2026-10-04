/*
 * Copyright 2026 Abel Ferrer Jiménez Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the
 * License.
 */
package es.nivel36.janus.config;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import es.nivel36.janus.service.TimeFormat;
import es.nivel36.janus.validation.LanguageTag;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Bound initial preferences under janus.user-provisioning.defaults. Setters
 * retain raw configuration values and may temporarily accept null during
 * binding; they do not validate or provision users. Before consumption, Bean
 * Validation requires a supported BCP 47 locale, nonnull time format and a
 * valid nonnull timezone identifier. Conversion methods return typed defaults
 * without changing the configuration or existing profiles.
 */
@Validated
@ConfigurationProperties("janus.user-provisioning.defaults")
public class UserProvisioningProperties {

	/**
	 * Creates an unbound configuration object with unset preference values. Binding
	 * and validation must complete before its typed defaults are consumed.
	 */
	public UserProvisioningProperties() {
	}

	@NotBlank
	@LanguageTag
	private String locale;

	@NotNull
	private TimeFormat timeFormat;

	@NotBlank
	private String defaultTimezone;

	/**
	 * Returns the raw configured locale without conversion or validation.
	 *
	 * @return locale string, possibly null before binding completes
	 */
	public String getLocale() {
		return this.locale;
	}

	/**
	 * Stores the raw locale for subsequent configuration validation.
	 *
	 * @param locale configured BCP 47 tag; may be null during binding, but must be
	 *               nonblank and supported before the configuration is used
	 */
	public void setLocale(final String locale) {
		this.locale = locale;
	}

	/**
	 * Returns the configured time format without changing it.
	 *
	 * @return time format, possibly null before binding completes
	 */
	public TimeFormat getTimeFormat() {
		return this.timeFormat;
	}

	/**
	 * Stores the time format for subsequent configuration validation.
	 *
	 * @param timeFormat configured preference; may be null during binding, but must
	 *                   be nonnull before the configuration is used
	 */
	public void setTimeFormat(final TimeFormat timeFormat) {
		this.timeFormat = timeFormat;
	}

	/**
	 * Returns the raw configured timezone without conversion or validation.
	 *
	 * @return timezone identifier, possibly null before binding completes
	 */
	public String getDefaultTimezone() {
		return this.defaultTimezone;
	}

	/**
	 * Stores the raw timezone for subsequent configuration validation.
	 *
	 * @param defaultTimezone configured identifier; may be null during binding, but
	 *                        must be nonblank and accepted by ZoneId before use
	 */
	public void setDefaultTimezone(final String defaultTimezone) {
		this.defaultTimezone = defaultTimezone;
	}

	/**
	 * Converts the already validated locale without changing configuration. The raw
	 * value must have passed nonblank, BCP 47 and supported-locale validation; this
	 * method does not itself enforce the full configuration constraints.
	 *
	 * @return                      locale corresponding to the configured language
	 *                              tag
	 * @throws NullPointerException if the raw locale has not been supplied
	 */
	public Locale locale() {
		return Locale.forLanguageTag(this.locale);
	}

	/**
	 * Converts the configured timezone without changing configuration. The raw
	 * value must have passed nonblank and valid-timezone validation.
	 *
	 * @return                             timezone corresponding to the configured
	 *                                     identifier
	 * @throws NullPointerException        if the raw timezone has not been supplied
	 * @throws java.time.DateTimeException if the identifier cannot be resolved
	 */
	public ZoneId defaultTimezone() {
		return ZoneId.of(this.defaultTimezone);
	}

	/**
	 * Tests configured locale availability without changing configuration. Null is
	 * accepted here so the separate NotBlank constraint can report absence.
	 *
	 * @return true when unset or exactly equal to an available locale's BCP 47 tag
	 */
	@AssertTrue(message = "must identify a supported locale")
	boolean isLocaleSupported() {
		return this.locale == null
				|| Locale.availableLocales().anyMatch(candidate -> candidate.toLanguageTag().equals(this.locale));
	}

	/**
	 * Tests timezone resolution without changing configuration. Null is accepted
	 * here so the separate NotBlank constraint can report absence.
	 *
	 * @return true when unset or accepted by ZoneId.of; false for invalid
	 *         identifiers
	 */
	@AssertTrue(message = "must be a valid timezone identifier")
	boolean isDefaultTimezoneValid() {
		if (this.defaultTimezone == null) {
			return true;
		}
		try {
			ZoneId.of(this.defaultTimezone);
			return true;
		} catch (final DateTimeException _) {
			return false;
		}
	}
}
