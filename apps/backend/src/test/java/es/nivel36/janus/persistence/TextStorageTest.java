/*
 * Copyright 2026 Abel Ferrer Jiménez Licensed under the Apache License, Version
 * 2.0 (the "License");
 */
package es.nivel36.janus.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/** Tests storage constraints independently of request validation and JPA. */
class TextStorageTest {
	private Connection connection;

	@BeforeEach
	void createDatabase() throws Exception {
		this.connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID(), "sa", "");
		ScriptUtils.executeSqlScript(this.connection, new ClassPathResource("sql/schema-h2.sql"));
		update("INSERT INTO schedule(id,name,code) VALUES (1,'Schedule','SCHEDULE')");
		update(
				"INSERT INTO employee(id,employee_number,name,surname,email,schedule_id) VALUES (1,'EMP-1','Name','Surname','user@example.test',1)");
		update("INSERT INTO worksite(id,name,code,time_zone,scope) VALUES (1,'Worksite','WORKSITE','UTC','GLOBAL')");
		update(
				"INSERT INTO app_user(email,keycloak_subject,locale,time_format,default_timezone) VALUES ('user@example.test','subject','en','H24','UTC')");
		update("INSERT INTO schedule_rule(id,name,schedule_id) VALUES (1,'Rule',1)");
		update(
				"INSERT INTO day_of_week_time_range(schedule_rule_id,day_of_week,start_time,end_time,effective_work_hours) VALUES (1,'MONDAY','09:00','17:00',28800)");
		update(
				"INSERT INTO clock_out_without_clock_in_event(employee_id,worksite_id,exit_time,detected_at) VALUES (1,1,TIMESTAMP '2026-01-01 12:00:00',TIMESTAMP '2026-01-01 12:00:00')");
	}

	@AfterEach
	void closeDatabase() throws Exception {
		this.connection.close();
	}

	private void update(final String sql, final Object... values) throws SQLException {
		try (final PreparedStatement statement = this.connection.prepareStatement(sql)) {
			for (int i = 0; i < values.length; i++) {
				statement.setObject(i + 1, values[i]);
			}
			statement.executeUpdate();
		}
	}

	static Stream<Arguments> unrestrictedText() {
		return Stream.of(
				Arguments.of("schedule", "name"),
				Arguments.of("schedule_rule", "name"),
				Arguments.of("employee", "name"),
				Arguments.of("employee", "surname"),
				Arguments.of("worksite", "name"),
				Arguments.of("worksite", "description"),
				Arguments.of("worksite", "address"),
				Arguments.of("clock_out_without_clock_in_event", "reason"),
				Arguments.of("app_user", "locale"),
				Arguments.of("app_user", "default_timezone"),
				Arguments.of("worksite", "time_zone"));
	}

	@ParameterizedTest
	@MethodSource("unrestrictedText")
	void storesTextBeyondTheOldColumnLimit(final String table, final String column) throws Exception {
		final String value = "á😀".repeat(300);
		update("UPDATE " + table + " SET " + column + " = ?", value);
		try (final Statement statement = this.connection.createStatement();
				final ResultSet result = statement.executeQuery("SELECT " + column + " FROM " + table)) {
			assertThat(result.next()).isTrue();
			assertThat(result.getString(1)).isEqualTo(value);
		}
	}

	static Stream<Arguments> invalidDomainValues() {
		return Stream.of(
				Arguments.of("schedule", "code", "", "CK_SCHEDULE_CODE_LENGTH"),
				Arguments.of("schedule", "code", "a".repeat(51), "CK_SCHEDULE_CODE_LENGTH"),
				Arguments.of("worksite", "code", "a".repeat(51), "CK_WORKSITE_CODE_LENGTH"),
				Arguments.of("employee", "employee_number", "a".repeat(51), "CK_EMPLOYEE_NUMBER_LENGTH"),
				Arguments.of("employee", "email", "a".repeat(255), "CK_EMPLOYEE_EMAIL_LENGTH"),
				Arguments.of("app_user", "email", "a".repeat(255), "CK_APP_USER_EMAIL_LENGTH"),
				Arguments.of("app_user", "keycloak_subject", "", "CK_APP_USER_SUBJECT_LENGTH"),
				Arguments.of("app_user", "keycloak_subject", "a".repeat(256), "CK_APP_USER_SUBJECT_LENGTH"),
				Arguments.of("worksite", "scope", "UNKNOWN", "CK_WORKSITE_SCOPE"),
				Arguments.of("app_user", "time_format", "H99", "CK_APP_USER_TIME_FORMAT"),
				Arguments.of("app_user", "theme", "UNKNOWN", "Check constraint violation"),
				Arguments.of("day_of_week_time_range", "day_of_week", "UNKNOWN", "CK_DOWTR_DAY"));
	}

	@ParameterizedTest
	@MethodSource("invalidDomainValues")
	void rejectsDomainViolationsThroughSql(
			final String table,
			final String column,
			final String value,
			final String constraint) {
		assertThatThrownBy(() -> update("UPDATE " + table + " SET " + column + " = ?", value))
				.isInstanceOf(SQLException.class).hasMessageContaining(constraint);
	}

	@Test
	void acceptsDomainLengthBoundaries() throws Exception {
		update("UPDATE schedule SET code = ?", "a".repeat(50));
		update("UPDATE worksite SET code = ?", "a".repeat(50));
		update("UPDATE employee SET employee_number = ?, email = ?", "a".repeat(50), "a".repeat(254));
		update("UPDATE app_user SET keycloak_subject = ?, email = ?", "a".repeat(255), "a".repeat(254));
	}
}
