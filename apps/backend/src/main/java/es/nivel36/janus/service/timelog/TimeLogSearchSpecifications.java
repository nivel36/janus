/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.service.timelog;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

/** Builds the same mandatory restriction for result and count queries. */
final class TimeLogSearchSpecifications {

	private TimeLogSearchSpecifications() {
	}

	static Specification<TimeLog> within(final TimeLogSearchScope scope) {
		return switch (scope) {
		case final TimeLogSearchScope.All ignored -> (root, query, builder) -> builder.conjunction();
		case final TimeLogSearchScope.Employee employee ->
			(root, query, builder) -> builder.equal(root.get("employee").get("id"), employee.employeeId());
		case final TimeLogSearchScope.None ignored -> (root, query, builder) -> builder.disjunction();
		};
	}

	static Specification<TimeLog> matching(final TimeLogSearchCriteria criteria) {
		return (root, query, builder) -> {
			Predicate predicate = builder.conjunction();
			if (criteria.employeeEmail() != null) {
				predicate = builder.and(predicate,
						builder.equal(root.get("employee").get("email"), criteria.employeeEmail()));
			}
			if (criteria.fromInstant() != null) {
				predicate = builder.and(predicate,
						builder.greaterThanOrEqualTo(root.get("entryTime"), criteria.fromInstant()),
						builder.lessThan(root.get("entryTime"), criteria.toInstant()));
			}
			return predicate;
		};
	}
}
