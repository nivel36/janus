package es.nivel36.janus.service.employee;

import java.util.Objects;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

/**
 * Repository class for managing {@link Employee} entities.
 */
class EmployeeRepository {

	private @PersistenceContext EntityManager entityManager;

	/**
	 * Finds an {@link Employee} by its primary key (id).
	 *
	 * @param id the primary key of the employee
	 * @return the employee with the specified id, or {@code null} if no employee is
	 *         found
	 * @throws IllegalArgumentException if the id is negative
	 */
	public Employee findEmployeeById(final long id) {
		if (id < 0) {
			throw new IllegalArgumentException(String.format("Id is %s, but cannot be less than 0.", id));
		}
		return this.entityManager.find(Employee.class, id);
	}

	/**
	 * Finds an {@link Employee} by email.
	 *
	 * @param email the email of the employee to find
	 * @return the employee with the specified email, or {@code null} if no employee
	 *         is found
	 */
	public Employee findEmployeeByEmail(final String email) {
		final TypedQuery<Employee> query = this.entityManager.createNamedQuery("Employee.findByEmail", Employee.class);
		query.setParameter("email", email);

		return query.getSingleResult();
	}

	/**
	 * Creates a new {@link Employee} entry in the database.
	 *
	 * @param employee the employee to be created
	 * @return the created employee
	 */
	public Employee createEmployee(final Employee employee) {
		this.entityManager.persist(employee);
		return employee;
	}

	/**
	 * Updates an existing {@link Employee} entry in the database.
	 *
	 * @param employee the employee to be updated
	 * @return the updated employee
	 */
	public Employee updateEmployee(final Employee employee) {
		return this.entityManager.merge(employee);
	}

	/**
	 * Deletes an {@link Employee} entry from the database.
	 *
	 * @param employee the employee to be deleted
	 */
	public void deleteEmployee(final Employee employee) {
		final Employee reference = this.entityManager.getReference(Employee.class, employee.getId());
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
