package es.nivel36.janus.service.timelog;

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
	 * @return the time log with the specified id, or null if no time log is found
	 * @throws IllegalArgumentException if the id is null
	 */
	public TimeLog findTimeLogById(final long id) {
		if (id < 0) {
			throw new IllegalArgumentException(String.format("Id is %s, but cannot be less than 0.", id));
		}
		
		return entityManager.find(TimeLog.class, id);
	}

	/**
	 * Finds the last {@link TimeLog} for the specified employee.
	 * 
	 * @param employee the employee whose last time log is to be found
	 * @return the last time log of the employee
	 * @throws NullPointerException if the employee is null
	 */
	public Optional<TimeLog> findLastTimeLogByEmployee(final Employee employee) {
		Objects.requireNonNull(employee, "Employee can't be null");
		
		final TypedQuery<TimeLog> query = entityManager.createNamedQuery("TimeLog.findTimeLogsByEmployee",
				TimeLog.class);
		query.setParameter("employee", employee);
		query.setMaxResults(1);
		
		return query.getResultStream().findFirst();
	}

	/**
	 * Retrieves all {@link TimeLog} entries for a given employee, with pagination.
	 * 
	 * @param employee      the employee whose time logs are to be retrieved
	 * @param startPosition the initial position of the search from which to start
	 *                      returning values.
	 * @param pageSize      the number of entries per page
	 * @return a list of time logs for the specified employee
	 * @throws NullPointerException     if the employee is null
	 * @throws IllegalArgumentException if the page is less than 0 or pageSize is
	 *                                  less than 1
	 */
	public List<TimeLog> findTimeLogsByEmployee(final Employee employee, int startPosition, int pageSize) {
		Objects.requireNonNull(employee, "Employee can't be null");

		if (startPosition < 0) {
			throw new IllegalArgumentException(
					String.format("Start position is %s, but cannot be less than 0.", startPosition));
		}

		if (pageSize < 1) {
			throw new IllegalArgumentException(String.format("Page size is %s, but must be greater than 0.", pageSize));
		}

		final TypedQuery<TimeLog> query = entityManager.createNamedQuery("TimeLog.findTimeLogsByEmployee",
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
	 * @throws NullPointerException if the employee is null
	 */
	public long countTimeLogsByEmployee(final Employee employee) {
		Objects.requireNonNull(employee, "Employee can't be null");

		final TypedQuery<Long> query = entityManager.createNamedQuery("TimeLog.countTimeLogsByEmployee", Long.class);
		query.setParameter("employee", employee);

		return query.getSingleResult();
	}

	/**
	 * Creates a new {@link TimeLog} entry in the database.
	 * 
	 * @param timeLog the time log to be created
	 * @return the created time log
	 * @throws NullPointerException if the time log is null
	 */
	public TimeLog createTimeLog(final TimeLog timeLog) {
		Objects.requireNonNull(timeLog, "TimeLog can't be null");
		entityManager.persist(timeLog);
		return timeLog;
	}

	/**
	 * Updates an existing {@link TimeLog} entry in the database.
	 * 
	 * @param timeLog the time log to be updated
	 * @return the updated time log
	 * @throws NullPointerException if the time log is null
	 */
	public TimeLog updateTimeLog(final TimeLog timeLog) {
		Objects.requireNonNull(timeLog);
		return entityManager.merge(timeLog);
	}

	/**
	 * Deletes a {@link TimeLog} entry from the database.
	 * 
	 * @param timeLog the time log to be deleted
	 * @throws NullPointerException if the time log is null
	 */
	public void deleteTimeLog(final TimeLog timeLog) {
		Objects.requireNonNull(timeLog);
		// Get a reference to the entity, assuming it exists in the database
		final TimeLog reference = entityManager.getReference(TimeLog.class, timeLog.getId());
		entityManager.remove(reference);
	}

	/**
	 * Sets the entity manager to be used as persistence context
	 * @param entityManager to be used as persistence context
	 * @throws NullPointerException if entityManager is null
	 */
	public void setEntityManager(final EntityManager entityManager) {
		this.entityManager = Objects.requireNonNull(entityManager, "Entity manager can't be null");
	}
}
