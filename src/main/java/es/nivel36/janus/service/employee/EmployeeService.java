package es.nivel36.janus.service.employee;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

/**
 * Service class responsible for managing {@link Employee} entities and
 * interacting with the {@link EmployeeRepository}.
 */
@Stateless
public class EmployeeService {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeService.class);

    private @Inject EmployeeRepository employeeRepository;

    /**
     * Finds an {@link Employee} by its primary key (Id).
     * 
     * @param id the ID of the employee to find
     * @return the employee with the specified Id, or null if no employee is found
     * @throws IllegalArgumentException if the Id is negative
     */
    public Employee findEmployeeById(final long id) {
        if (id < 0) {
            throw new IllegalArgumentException(String.format("Id is %s, but cannot be less than 0.", id));
        }
        logger.debug("Finding Employee by id: {}", id);
        return employeeRepository.findEmployeeById(id);
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
        logger.debug("Finding Employee by email: {}", email);
        return employeeRepository.findEmployeeByEmail(email);
    }

    /**
     * Creates a new {@link Employee}.
     * 
     * @param employee the employee to be created
     * @return the created employee
     * @throws NullPointerException if the employee is null
     */
    public Employee createEmployee(final Employee employee) {
        Objects.requireNonNull(employee, "Employee cannot be null.");
        logger.debug("Creating new Employee: {}", employee);
        return employeeRepository.createEmployee(employee);
    }

    /**
     * Updates an existing {@link Employee}.
     * 
     * @param employee the employee to be updated
     * @return the updated employee
     * @throws NullPointerException if the employee is null
     */
    public Employee updateEmployee(final Employee employee) {
        Objects.requireNonNull(employee, "Employee cannot be null.");
        logger.debug("Updating Employee: {}", employee);
        return employeeRepository.updateEmployee(employee);
    }

    /**
     * Deletes an existing {@link Employee}.
     * 
     * @param employee the employee to be deleted
     * @throws NullPointerException if the employee is null
     */
    public void deleteEmployee(final Employee employee) {
        Objects.requireNonNull(employee, "Employee cannot be null.");
        logger.debug("Deleting Employee: {}", employee);
        employeeRepository.deleteEmployee(employee);
    }

    /**
     * Sets the {@link EmployeeService}.
     * 
     * @param employeeRepository the repository used to manage employee records
     * @throws NullPointerException if the employeeRepository is null
     */
    public void setEmployeeService(final EmployeeRepository employeeRepository) {
        this.employeeRepository = Objects.requireNonNull(employeeRepository, "EmployeeRepository cannot be null.");
    }
}
