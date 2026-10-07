/*
 * Copyright 2026 Abel Ferrer Jiménez Licensed under the Apache License, Version
 * 2.0 (the "License");
 */
package es.nivel36.janus.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EmailAddressesTest {
	@Test
	void normalizationIsBoundedAndCanExpandUnicode() {
		assertThat(EmailAddresses.canonicalize("  USER@EXAMPLE.TEST  ")).isEqualTo("user@example.test");
		assertThat(EmailAddresses.canonicalize("A".repeat(254))).isEqualTo("a".repeat(254));
		assertThatThrownBy(() -> EmailAddresses.canonicalize("A".repeat(255)))
				.isInstanceOf(IllegalArgumentException.class);
		// U+0130 becomes two UTF-16 code units when lowercased with Locale.ROOT.
		assertThatThrownBy(() -> EmailAddresses.canonicalize("A".repeat(253) + "İ"))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
