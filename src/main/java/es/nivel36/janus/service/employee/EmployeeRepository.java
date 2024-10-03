package es.nivel36.janus.service.employee;

import java.util.Objects;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

/**
 * Repository class for managing {@link Employee} entities.
 */
class EmployeeRepository {

	@PersistenceContext
	private EntityManager entityManager;
	
	/**
	 * Finds an {@link Employee} by its primary key (id).
	 *
	 * @param id the primary key of the employee
	 * @return the employee with the specified id, or null if no employee is found
	 * @throws IllegalArgumentException if the id is negative
	 */
	public Employee findEmployeeById(final long id) {
		if (id < 0) {
			throw new IllegalArgumentException(String.format("Id is %s, but cannot be less than 0.", id));
		}
		return entityManager.find(Employee.class, id);
	}

	/**
	 * Finds an {@link Employee} by email.
	 *
	 * @param email the email of the employee to find
	 * @return the employee with the specified email, or null if no employee is
	 *         found
	 * @throws NullPointerException if the email is null
	 */
	public Employee findEmployeeByEmail(final String email) {
		Objects.requireNonNull(email, "Email cannot be null.");

		final TypedQuery<Employee> query = entityManager.createNamedQuery("Employee.findByEmail", Employee.class);
		query.setParameter("email", email);

		return query.getSingleResult();
	}

	/**
	 * Creates a new {@link Employee} entry in the database.
	 *
	 * @param employee the employee to be created
	 * @return the created employee
	 * @throws NullPointerException if the employee is null
	 */
	public Employee createEmployee(final Employee employee) {
		Objects.requireNonNull(employee, "Employee cannot be null.");
		entityManager.persist(employee);
		return employee;
	}

	/**
	 * Updates an existing {@link Employee} entry in the database.
	 *
	 * @param employee the employee to be updated
	 * @return the updated employee
	 * @throws NullPointerException if the employee is null
	 */
	public Employee updateEmployee(final Employee employee) {
		Objects.requireNonNull(employee, "Employee cannot be null.");
		return entityManager.merge(employee);
	}

	/**
	 * Deletes an {@link Employee} entry from the database.
	 *
	 * @param employee the employee to be deleted
	 * @throws NullPointerException if the employee is null
	 */
	public void deleteEmployee(final Employee employee) {
		Objects.requireNonNull(employee, "Employee cannot be null.");
		final Employee reference = entityManager.getReference(Employee.class, employee.getId());
		entityManager.remove(reference);
	}
	
	public void setEntityManager(final EntityManager entityManager) {
		this.entityManager = Objects.requireNonNull(entityManager, "Entity manager can't be null");
	}
}
