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
		case final TimeLogSearchScope.All _ -> (_, _, builder) -> builder.conjunction();
		case TimeLogSearchScope.Employee(Long employeeId) ->
			(root, _, builder) -> builder.equal(root.get("employee").get("id"), employeeId);
		case final TimeLogSearchScope.None _ -> (_, _, builder) -> builder.disjunction();
		};
	}

	static Specification<TimeLog> matching(final TimeLogSearchCriteria criteria) {
		return (root, _, builder) -> {
			Predicate predicate = builder.conjunction();
			if (criteria.employeeNumber() != null) {
				predicate = builder.and(predicate,
						builder.equal(root.get("employee").get("employeeNumber"), criteria.employeeNumber()));
			}
			if (criteria.start() != null) {
				predicate = builder.and(predicate,
						builder.greaterThanOrEqualTo(root.get("entryTime"), criteria.start()),
						builder.lessThan(root.get("entryTime"), criteria.end()));
			}
			return predicate;
		};
	}
}
