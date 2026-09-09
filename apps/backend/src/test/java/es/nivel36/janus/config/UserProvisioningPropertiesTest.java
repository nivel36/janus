/*
 * Copyright 2026 Abel Ferrer Jiménez
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package es.nivel36.janus.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class UserProvisioningPropertiesTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(PropertiesConfiguration.class)
			.withPropertyValues("janus.user-provisioning.defaults.locale=en-US",
					"janus.user-provisioning.defaults.time-format=H24",
					"janus.user-provisioning.defaults.default-timezone=UTC");

	@Test
	void bindsValidDefaults() {
		this.contextRunner.run(context -> {
			assertThat(context).hasNotFailed();
			final UserProvisioningProperties defaults = context.getBean(UserProvisioningProperties.class);
			assertThat(defaults.locale().toLanguageTag()).isEqualTo("en-US");
			assertThat(defaults.getTimeFormat().name()).isEqualTo("H24");
			assertThat(defaults.defaultTimezone().getId()).isEqualTo("UTC");
		});
	}

	@Test
	void rejectsUnsupportedLocale() {
		this.contextRunner.withPropertyValues("janus.user-provisioning.defaults.locale=zz-ZZ")
				.run(context -> assertThat(context).hasFailed());
	}

	@Test
	void rejectsUnknownTimeFormat() {
		this.contextRunner.withPropertyValues("janus.user-provisioning.defaults.time-format=H13")
				.run(context -> assertThat(context).hasFailed());
	}

	@Test
	void rejectsUnknownTimezone() {
		this.contextRunner.withPropertyValues("janus.user-provisioning.defaults.default-timezone=Mars/Olympus")
				.run(context -> assertThat(context).hasFailed());
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(UserProvisioningProperties.class)
	static class PropertiesConfiguration {
	}
}
