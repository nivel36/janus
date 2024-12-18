package es.nivel36.janus.service.employee;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import es.nivel36.janus.service.schedule.Schedule;
import es.nivel36.janus.service.timelog.TimeLog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

/**
 * Entity representing an employee. This entity contains personal information
 * about the employee and maintains a list of time logs associated with the
 * employee.
 *
 * <p>
 * Each employee is uniquely identified by their email address. In addition to
 * time logs, the employee also has an associated work schedule represented by a
 * {@link Schedule} entity, which is mandatory and cannot be null.
 * </p>
 *
 * @see TimeLog
 */
@Entity
@Table(indexes = { //
		@Index(name = "idx_employee_email", columnList = "email") //
}, uniqueConstraints = { //
		@UniqueConstraint(name = "uk_employee_email", columnNames = { "email" }) //
})
public class Employee implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Unique identifier for the employee. Auto-generated.
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * The first name of the employee.
	 */
	private String name;

	/**
	 * The surname of the employee.
	 */
	private String surname;

	/**
	 * The email of the employee. It must be unique and cannot be null.
	 */
	@NotNull
	@Column(nullable = false, unique = true)
	private String email;

	/**
	 * The list of time logs associated with the employee. Represents the time
	 * entries recorded by the employee.
	 */
	@OneToMany(mappedBy = "employee", orphanRemoval = true)
	private List<TimeLog> timeLogs;

	/**
	 * The work schedule associated with this employee. This field is mandatory and
	 * cannot be null.
	 */
	@NotNull
	@ManyToOne
	@JoinColumn(name = "schedule_id", nullable = false)
	private Schedule schedule;
	
	public Employee() {
	}
	
	public Employee(final String email, final Schedule schedule) {
		this.email = Objects.requireNonNull(email);
		this.schedule = Objects.requireNonNull(schedule);
	}

	/**
	 * Gets the unique identifier of the employee.
	 *
	 * @return the id of the employee
	 */
	public Long getId() {
		return this.id;
	}

	/**
	 * Sets the unique identifier for the employee.
	 *
	 * @param id the id to set for the employee
	 */
	public void setId(final Long id) {
		this.id = id;
	}

	/**
	 * Gets the name of the employee.
	 *
	 * @return the name of the employee
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sets the name of the employee.
	 *
	 * @param name the name to set for the employee
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * Gets the surname of the employee.
	 *
	 * @return the surname of the employee
	 */
	public String getSurname() {
		return this.surname;
	}

	/**
	 * Sets the surname of the employee.
	 *
	 * @param surname the surname to set for the employee
	 */
	public void setSurname(final String surname) {
		this.surname = surname;
	}

	/**
	 * Gets the email of the employee.
	 *
	 * @return the email of the employee
	 */
	public String getEmail() {
		return this.email;
	}

	/**
	 * Sets the email of the employee.
	 *
	 * @param email the email to set for the employee
	 */
	public void setEmail(final String email) {
		this.email = email;
	}

	/**
	 * Gets the list of time logs associated with the employee.
	 *
	 * @return the list of time logs for the employee
	 */
	public List<TimeLog> getTimeLogs() {
		return this.timeLogs;
	}

	/**
	 * Sets the list of time logs for the employee.
	 *
	 * @param timeLogs the time logs to associate with the employee
	 */
	public void setTimeLogs(final List<TimeLog> timeLogs) {
		this.timeLogs = timeLogs;
	}

	/**
	 * Gets the work schedule associated with the employee.
	 *
	 * @return the schedule associated with the employee
	 */
	public Schedule getSchedule() {
		return this.schedule;
	}

	/**
	 * Sets the work schedule for the employee. The schedule cannot be null.
	 *
	 * @param schedule the schedule to set for the employee
	 */
	public void setSchedule(final Schedule schedule) {
		this.schedule = schedule;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.email);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (this.getClass() != obj.getClass())) {
			return false;
		}
		final Employee other = (Employee) obj;
		return Objects.equals(this.email, other.email);
	}

	@Override
	public String toString() {
		return this.email;
	}
}
