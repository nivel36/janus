/*
 * Copyright 2026 Abel Ferrer Jiménez
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package es.nivel36.janus.service.schedule;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import es.nivel36.janus.service.ResourceAlreadyExistsException;
import es.nivel36.janus.service.ResourceNotFoundException;
import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.util.LikePatterns;
import es.nivel36.janus.validation.EmployeeNumber;
import es.nivel36.janus.validation.NonNegativeDuration;
import es.nivel36.janus.validation.ScheduleCode;
import es.nivel36.janus.validation.SearchQuery;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Transactional entry point for schedule creation, lookup, search, replacement
 * and deletion, and employee working-time lookup. Callers authorize operations
 * before invoking this service.
 */
@Validated
@Service
public class ScheduleService {

	private static final Logger logger = LoggerFactory.getLogger(ScheduleService.class);

	private final ScheduleRepository scheduleRepository;
	private final int maxPageSize;

	/**
	 * Creates a service without accessing persistence.
	 *
	 * @param  scheduleRepository       nonnull schedule repository
	 * @param  maxPageSize              positive limit from
	 *                                  spring.data.rest.max-page-size
	 * @throws NullPointerException     if the repository is null
	 * @throws IllegalArgumentException if maxPageSize is not positive
	 */
	public ScheduleService(
		final ScheduleRepository scheduleRepository,
		final @Value("${spring.data.rest.max-page-size}") int maxPageSize) {
		this.scheduleRepository = Objects.requireNonNull(scheduleRepository, "scheduleRepository can't be null");
		if (maxPageSize < 1) {
			throw new IllegalArgumentException("maxPageSize must be positive");
		}
		this.maxPageSize = maxPageSize;
	}

	/**
	 * Creates and persists a schedule with a unique immutable business code. Code
	 * must match [A-Za-z0-9_-]{1,50}; name must contain 1-250 letters, digits,
	 * spaces or basic punctuation and must not be blank. Tolerances are nonnull and
	 * nonnegative. Rules and their elements are nonnull; empty rules are allowed.
	 * Rule dates, day uniqueness and working durations are checked while building
	 * the aggregate before persistence.
	 *
	 * @param  code                           unique business code, used without
	 *                                        trimming
	 * @param  name                           schedule display name, used without
	 *                                        trimming
	 * @param  entryTolerance                 allowed entry deviation
	 * @param  exitTolerance                  allowed exit deviation
	 * @param  rules                          complete initial rule definitions
	 * @return                                persisted schedule including its rules
	 *                                        and time ranges
	 * @throws ConstraintViolationException   if parameter validation fails through
	 *                                        the Spring proxy
	 * @throws IllegalArgumentException       if a rule definition is inconsistent
	 * @throws NullPointerException           if a nested definition is null
	 * @throws ResourceAlreadyExistsException if the code already exists
	 */
	@Transactional
	public Schedule createSchedule(
			final @NotBlank @ScheduleCode String code,
			final @NotBlank @Pattern(regexp = "^[\\p{L}0-9 _'.,-]{1,250}$") String name,
			final @NotNull @NonNegativeDuration Duration entryTolerance,
			final @NotNull @NonNegativeDuration Duration exitTolerance,
			final @NotNull List<@NotNull ScheduleRuleDefinition> rules) {
		logger.debug("Creating schedule with code {}", code);
		this.assertScheduleCodeIsUnique(code);

		final Schedule schedule = this.buildSchedule(code, name, entryTolerance, exitTolerance, rules);
		final Schedule savedSchedule = this.scheduleRepository.save(schedule);
		logger.debug("Schedule created: scheduleId={}, scheduleCode={}", savedSchedule.getId(), code);
		return savedSchedule;
	}

	private void assertScheduleCodeIsUnique(final String code) {
		final boolean existsByCode = this.scheduleRepository.existsByCode(code);
		if (existsByCode) {
			throw new ResourceAlreadyExistsException("Schedule already exists with code " + code);
		}
	}

	private Schedule buildSchedule(
			final String code,
			final String name,
			final Duration entryTolerance,
			final Duration exitTolerance,
			final List<ScheduleRuleDefinition> rules) {
		final Schedule schedule = new Schedule(code, name, entryTolerance, exitTolerance);
		for (final ScheduleRuleDefinition ruleDefinition : rules) {
			final ScheduleRule rule = this.buildRule(schedule, ruleDefinition);
			schedule.addRule(rule);
		}
		return schedule;
	}

	private ScheduleRule buildRule(final Schedule schedule, final ScheduleRuleDefinition ruleDefinition) {
		final ScheduleRule rule = new ScheduleRule(ruleDefinition.name(), schedule);
		rule.setActivePeriod(ruleDefinition.startDate(), ruleDefinition.endDate());

		final List<ScheduleRuleTimeRangeDefinition> dayOfWeekDefinitionRanges = ruleDefinition.dayOfWeekRanges();
		for (final ScheduleRuleTimeRangeDefinition timeRangeDefinition : dayOfWeekDefinitionRanges) {
			final DayOfWeekTimeRange dayOfWeekTimeRange = this.buildDayOfWeekTimeRange(rule, timeRangeDefinition);
			rule.addRange(dayOfWeekTimeRange);
		}
		return rule;
	}

	private DayOfWeekTimeRange buildDayOfWeekTimeRange(
			final ScheduleRule rule,
			final ScheduleRuleTimeRangeDefinition timeRangeDefinition) {
		final DayOfWeek dayOfWeek = timeRangeDefinition.dayOfWeek();
		final LocalTime startTime = timeRangeDefinition.startTime();
		final LocalTime endTime = timeRangeDefinition.endTime();
		final TimeRange timeRange = new TimeRange(startTime, endTime);
		final Duration effectiveWorkHours = timeRangeDefinition.effectiveWorkHours();
		return new DayOfWeekTimeRange(rule, dayOfWeek, timeRange, effectiveWorkHours);
	}

	/**
	 * Replaces an existing schedule's name, tolerances and complete rule set.
	 * Arguments have the same constraints as creation. All replacement rules are
	 * built before mutation. The code and employee assignments are preserved;
	 * removed rules and time ranges are deleted on transaction commit.
	 *
	 * @param  code                         exact immutable business code of the
	 *                                      target
	 * @param  name                         replacement display name
	 * @param  entryTolerance               nonnull, nonnegative entry deviation
	 * @param  exitTolerance                nonnull, nonnegative exit deviation
	 * @param  rules                        nonnull complete replacement; empty
	 *                                      removes all rules
	 * @return                              updated schedule with replacement rules
	 *                                      and time ranges
	 * @throws ConstraintViolationException if parameter validation fails through
	 *                                      the Spring proxy
	 * @throws IllegalArgumentException     if a rule definition is inconsistent
	 * @throws NullPointerException         if a nested definition is null
	 * @throws ResourceNotFoundException    if the code does not exist
	 */
	@Transactional
	public Schedule updateSchedule(
			final @NotBlank @ScheduleCode String code,
			final @NotBlank @Pattern(regexp = "^[\\p{L}0-9 _'.,-]{1,250}$") String name,
			final @NotNull @NonNegativeDuration Duration entryTolerance,
			final @NotNull @NonNegativeDuration Duration exitTolerance,
			final @NotNull List<@NotNull ScheduleRuleDefinition> rules) {
		logger.atDebug().addKeyValue("scheduleCode", code).addKeyValue("name", name)
				.addKeyValue("entryTolerance", entryTolerance).addKeyValue("exitTolerance", exitTolerance)
				.addKeyValue("ruleCount", rules.size()).log("Updating schedule");

		final Schedule persisted = this.findSchedule(code);
		// Build every replacement rule before changing the managed aggregate.
		final List<ScheduleRule> replacements = rules.stream().map(definition -> this.buildRule(persisted, definition))
				.toList();
		persisted.setName(name);
		persisted.setEntryTolerance(entryTolerance);
		persisted.setExitTolerance(exitTolerance);
		persisted.clearRules();

		replacements.forEach(persisted::addRule);
		return persisted;
	}

	/**
	 * Deletes an existing {@link Schedule}.
	 * <p>
	 * A schedule can only be deleted if it has no employees assigned.
	 * </p>
	 *
	 * @param  code                         exact business code of the schedule to
	 *                                      delete
	 * @throws ResourceNotFoundException    if the schedule does not exist
	 * @throws ConstraintViolationException if code is invalid through the Spring
	 *                                      proxy
	 * @throws IllegalStateException        if the schedule has assigned employees
	 */
	@Transactional
	public void deleteSchedule(final @NotBlank @ScheduleCode String code) {
		logger.debug("Schedule with code {} marked for deletion", code);

		final boolean inUse = this.scheduleRepository.hasEmployees(code);
		if (inUse) {
			throw new IllegalStateException(
					"The schedule " + code + " can't be deleted because it has assigned employees");
		}
		if (this.scheduleRepository.deleteByCode(code) == 0) {
			throw new ResourceNotFoundException("There is no schedule with code " + code);
		}
	}

	/**
	 * Retrieves a {@link Schedule} by its unique code.
	 *
	 * @param  code                         unique schedule code; can't be
	 *                                      {@code null}
	 * @return                              the {@link Schedule} associated with the
	 *                                      given code
	 * @throws ConstraintViolationException if code is null or does not match
	 *                                      [A-Za-z0-9_-]{1,50} through the Spring
	 *                                      proxy
	 * @throws ResourceNotFoundException    if no schedule exists with the given
	 *                                      code
	 */
	@Transactional(readOnly = true)
	public Schedule findScheduleByCode(final @NotBlank @ScheduleCode String code) {
		return this.findSchedule(code);
	}

	private Schedule findSchedule(final String code) {
		final Schedule schedule = this.scheduleRepository.findByCode(code);
		if (schedule == null) {
			throw new ResourceNotFoundException("No schedule found with code " + code);
		}
		return schedule;
	}

	/**
	 * Finds the {@link TimeRange} applicable to an {@link Employee} on a given
	 * {@link LocalDate}.
	 * <p>
	 * If no time range applies for the given date (for example, non-working days),
	 * an empty {@link Optional} is returned.
	 * </p>
	 *
	 * @param  employee                     employee whose working time is
	 *                                      requested; can't be {@code null}
	 * @param  date                         date to evaluate; can't be {@code null}
	 * @return                              an {@link Optional} containing the
	 *                                      applicable {@link TimeRange}, or an
	 *                                      empty {@code Optional} if none applies
	 * @throws ConstraintViolationException if employee or date is null through the
	 *                                      Spring proxy
	 */
	@Transactional(readOnly = true)
	public Optional<TimeRange> findTimeRangeForEmployeeByDate(
			final @NotNull Employee employee,
			final @NotNull LocalDate date) {
		logger.debug("Finding time range for employee with id {} on date {}", employee.getId(), date);

		final DayOfWeek dayOfWeek = date.getDayOfWeek();
		return this.scheduleRepository.findTimeRangeForDate(employee.getId(), date, dayOfWeek);
	}

	/**
	 * Searches schedules without modifying them. The caller supplies an authorized
	 * employee scope and a nonnull, paged request.
	 * <p>
	 * Filters combine with AND. Null or empty query disables text filtering;
	 * matching is literal, partial and case-insensitive against code and name,
	 * without trimming. Employee number matches exactly. Absent filters include
	 * schedules without employees, rules or time ranges. Public sort fields are
	 * code and name; code ascending is the default and is added as a unique
	 * tie-breaker unless explicitly sorted. Page size is capped at
	 * spring.data.rest.max-page-size. Pagination runs in the database without
	 * loading rules or their time ranges.
	 * </p>
	 *
	 * @param  query                        optional single-line fragment of at most
	 *                                      100 characters
	 * @param  employeeNumber               optional number matching
	 *                                      [A-Za-z0-9_-]{1,50}
	 * @param  pageable                     requested page and public ordering
	 * @return                              possibly empty page of schedules for
	 *                                      summary mapping
	 * @throws ConstraintViolationException if a filter is invalid or pageable is
	 *                                      null through the Spring proxy; an empty
	 *                                      query is invalid there
	 * @throws IllegalArgumentException     if pageable is unpaged or a sort field
	 *                                      is unsupported
	 */
	@Transactional(readOnly = true)
	public Page<Schedule> searchSchedules(
			final @SearchQuery String query,
			final @EmployeeNumber String employeeNumber,
			final @NotNull Pageable pageable) {
		final Pageable normalizedPageable = this.normalizePageable(pageable);
		final String escapedQuery = query == null ? "" : LikePatterns.escape(query);
		logger.atDebug().addKeyValue("query", query).addKeyValue("employeeNumber", employeeNumber)
				.addKeyValue("page", normalizedPageable.getPageNumber())
				.addKeyValue("pageSize", normalizedPageable.getPageSize())
				.addKeyValue("sort", normalizedPageable.getSort().toString()).log("Searching schedules");

		return this.scheduleRepository.search(escapedQuery, employeeNumber, normalizedPageable);
	}

	private Pageable normalizePageable(final Pageable pageable) {
		if (pageable.isUnpaged()) {
			throw new IllegalArgumentException("Must be paged");
		}
		final List<Order> orders = new ArrayList<>();
		for (final Order order : pageable.getSort()) {
			switch (order.getProperty()) {
			case "code", "name" -> orders.add(order);
			default -> throw new IllegalArgumentException("Unsupported Schedule sort field: " + order.getProperty());
			}
		}
		if (orders.stream().noneMatch(order -> "code".equals(order.getProperty()))) {
			orders.add(Order.asc("code"));
		}
		return PageRequest
				.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), this.maxPageSize), Sort.by(orders));
	}
}
