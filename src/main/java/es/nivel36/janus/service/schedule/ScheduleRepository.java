package es.nivel36.janus.service.schedule;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import es.nivel36.janus.service.employee.Employee;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

/**
 * Repository class responsible for interacting with the persistence layer to
 * manage schedule-related data.
 */
class ScheduleRepository {

	private @PersistenceContext EntityManager entityManager;

	/**
	 * Finds the {@link TimeRange} for a given {@link Employee} on a specific
	 * {@link LocalDate}.
	 *
	 * @param employee the employee whose time range is to be found. Cannot be
	 *                 {@code null}.
	 * @param date     the date for which the time range is to be found. Cannot be
	 *                 {@code null}.
	 * @return an {@code Optional} containing the {@link TimeRange} if one is
	 *         defined, or an empty {@code Optional} if no time range exists.
	 * @throws NullPointerException if either {@code employee} or {@code date} is
	 *                              {@code null}.
	 */
	public Optional<TimeRange> findTimeRangeForEmployeeByDate(final Employee employee, final LocalDate date) {
		final TypedQuery<TimeRange> query = this.entityManager
				.createNamedQuery("TimeRange.findTimeRangeForEmployeeByDate", TimeRange.class);
		query.setParameter("employee", employee);
		query.setParameter("date", date);
		query.setParameter("dayOfWeek", date.getDayOfWeek());

		return query.getResultStream().findFirst();
	}

	/**
	 * Sets the entity manager to be used as the persistence context.
	 *
	 * @param entityManager the entity manager to be used as the persistence
	 *                      context.
	 * @throws NullPointerException if the {@code entityManager} is {@code null}.
	 */
	public void setEntityManager(final EntityManager entityManager) {
		this.entityManager = Objects.requireNonNull(entityManager, "Entity manager can't be null");
	}
}
