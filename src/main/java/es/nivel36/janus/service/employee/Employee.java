package es.nivel36.janus.service.employee;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import es.nivel36.janus.service.timelog.TimeLog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

/**
 * Entity representing an employee. This entity contains personal information
 * about the employee and maintains a list of time logs associated with the
 * employee.
 */
@Entity
@Table(indexes = { //
		@Index(name = "idx_employee_email", columnList = "email") //
}, uniqueConstraints = { //
		@UniqueConstraint(name = "uk_employee_email", columnNames = { "email" }) //
})
public class Employee implements Serializable {
	
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;

	private String surname;

	@NotNull
	@Column(nullable = false, unique = true)
	private String email;

	@OneToMany(mappedBy = "employee", orphanRemoval = true)
	private List<TimeLog> timeLogs;

	/**
	 * Gets the unique identifier of the employee.
	 *
	 * @return the id of the employee
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Sets the unique identifier for the employee.
	 *
	 * @param id the id to set for the employee
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Gets the name of the employee.
	 *
	 * @return the name of the employee
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of the employee.
	 *
	 * @param name the name to set for the employee
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the surname of the employee.
	 *
	 * @return the surname of the employee
	 */
	public String getSurname() {
		return surname;
	}

	/**
	 * Sets the surname of the employee.
	 *
	 * @param surname the surname to set for the employee
	 */
	public void setSurname(String surname) {
		this.surname = surname;
	}

	/**
	 * Gets the email of the employee.
	 *
	 * @return the email of the employee
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * Sets the email of the employee.
	 *
	 * @param email the email to set for the employee.
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Gets the list of time logs associated with the employee.
	 *
	 * @return the list of time logs for the employee
	 */
	public List<TimeLog> getTimeLogs() {
		return timeLogs;
	}

	/**
	 * Sets the list of time logs for the employee.
	 *
	 * @param timeLogs the time logs to associate with the employee
	 */
	public void setTimeLogs(List<TimeLog> timeLogs) {
		this.timeLogs = timeLogs;
	}

	@Override
	public int hashCode() {
		return Objects.hash(email);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Employee other = (Employee) obj;
		return Objects.equals(email, other.email);
	}
}
