/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EmailTest {

	@Test
	void parsesAndCanonicalizesItsParts() {
		final Email email = Email.of("  Ada.Lovelace@EXAMPLE.TEST  ");

		assertThat(email.name()).isEqualTo("ada.lovelace");
		assertThat(email.server()).isEqualTo("example.test");
		assertThat(email.value()).isEqualTo("ada.lovelace@example.test");
		assertThat(email.toString()).isEqualTo(email.value());
	}

	@Test
	void separatedPartsAreCanonicalized() {
		assertThat(new Email(" ADA ", " EXAMPLE.TEST ").value()).isEqualTo("ada@example.test");
	}

	@Test
	void rejectsInvalidAddresses() {
		assertThatThrownBy(() -> Email.of("not-an-email")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Email.of("name@example")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Email.of(null)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> Email.of(" ")).isInstanceOf(IllegalArgumentException.class);
	}
}
