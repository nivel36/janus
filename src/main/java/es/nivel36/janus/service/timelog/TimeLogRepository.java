package es.nivel36.janus.service.timelog;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import es.nivel36.janus.service.employee.Employee;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

/**
 * Repository class for managing {@link TimeLog} entities.
 */
class TimeLogRepository {

	private @PersistenceContext EntityManager entityManager;

	/**
	 * Finds a {@link TimeLog} by its primary key (id).
	 *
	 * @param id the primary key of the time log
	 * @return the time log with the specified id, or {@code null} if no time log is
	 *         found
	 */
	public TimeLog findTimeLogById(final long id) {
		return this.entityManager.find(TimeLog.class, id);
	}

	/**
	 * Finds the last {@link TimeLog} for the specified employee.
	 *
	 * @param employee the employee whose last time log is to be found
	 * @return an {@link Optional} containing the last time log of the employee if
	 *         present
	 */
	public Optional<TimeLog> findLastTimeLogByEmployee(final Employee employee) {
		final TypedQuery<TimeLog> query = this.entityManager.createNamedQuery("TimeLog.findTimeLogsByEmployee",
				TimeLog.class);
		query.setParameter("employee", employee);
		query.setMaxResults(1);

		return query.getResultStream().findFirst();
	}

	/**
	 * Retrieves all {@link TimeLog} entries for a given employee within a specified
	 * date range.
	 *
	 * @param employee  the employee whose time logs are to be retrieved
	 * @param startDate the start date of the range (inclusive)
	 * @param endDate   the end date of the range (inclusive)
	 * @return a list of time logs for the specified employee within the date range
	 */
	public List<TimeLog> findTimeLogsByEmployeeAndDateRange(final Employee employee, final LocalDate startDate,
			final LocalDate endDate) {
		final TypedQuery<TimeLog> query = this.entityManager
				.createNamedQuery("TimeLog.findTimeLogsByEmployeeAndDateRange", TimeLog.class);
		query.setParameter("employee", employee);
		query.setParameter("startDate", startDate);
		query.setParameter("endDate", endDate);

		return query.getResultList();
	}

	/**
	 * Retrieves all {@link TimeLog} entries for a given employee, with pagination.
	 *
	 * @param employee      the employee whose time logs are to be retrieved
	 * @param startPosition the initial position of the search from which to start
	 *                      returning values.
	 * @param pageSize      the number of entries per page
	 * @return a list of time logs for the specified employee
	 */
	public List<TimeLog> findTimeLogsByEmployee(final Employee employee, final int startPosition, final int pageSize) {
		final TypedQuery<TimeLog> query = this.entityManager.createNamedQuery("TimeLog.findTimeLogsByEmployee",
				TimeLog.class);
		query.setParameter("employee", employee);
		query.setFirstResult(startPosition);
		query.setMaxResults(pageSize);

		return query.getResultList();
	}

	/**
	 * Counts all {@link TimeLog} entries for a given employee.
	 *
	 * @param employee the employee whose time logs are to be retrieved
	 * @return the number of time logs for the specified employee
	 */
	public long countTimeLogsByEmployee(final Employee employee) {
		final TypedQuery<Long> query = this.entityManager.createNamedQuery("TimeLog.countTimeLogsByEmployee",
				Long.class);
		query.setParameter("employee", employee);

		return query.getSingleResult();
	}

	/**
	 * Creates a new {@link TimeLog} entry in the database.
	 *
	 * @param timeLog the time log to be created
	 * @return the created time log
	 */
	public TimeLog createTimeLog(final TimeLog timeLog) {
		this.entityManager.persist(timeLog);
		return timeLog;
	}

	/**
	 * Updates an existing {@link TimeLog} entry in the database.
	 *
	 * @param timeLog the time log to be updated
	 * @return the updated time log
	 */
	public TimeLog updateTimeLog(final TimeLog timeLog) {
		return this.entityManager.merge(timeLog);
	}

	/**
	 * Deletes a {@link TimeLog} entry from the database.
	 *
	 * @param timeLog the time log to be deleted
	 */
	public void deleteTimeLog(final TimeLog timeLog) {
		// Get a reference to the entity, assuming it exists in the database
		final TimeLog reference = this.entityManager.getReference(TimeLog.class, timeLog.getId());
		this.entityManager.remove(reference);
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
