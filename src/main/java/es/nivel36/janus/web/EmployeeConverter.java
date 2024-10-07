package es.nivel36.janus.web;

import java.util.Objects;

import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Inject;

/**
 * Converter for handling {@link Employee} objects in JSF. This converter allows
 * the conversion between an {@link Employee} object and its String
 * representation (the employee ID).
 *
 * The converter is registered for the {@link Employee} class and is managed by
 * the CDI container.
 *
 * <p>
 * Conversion logic:<br/>
 * - When converting from String to {@link Employee} (`getAsObject`), it expects
 * the string to be a valid numeric ID. <br/>
 * - When converting from {@link Employee} to String (`getAsString`), it uses
 * the employee's ID.
 * </p>
 *
 */
@FacesConverter(forClass = Employee.class, managed = true)
public class EmployeeConverter implements Converter<Employee> {

	private @Inject EmployeeService employeeService;

	/**
	 * Converts a string (employee ID) into an {@link Employee} object by looking it
	 * up using the {@link EmployeeService}.
	 *
	 * @param context   the {@link FacesContext} for the current request
	 * @param component the {@link UIComponent} associated with this conversion
	 * @param value     the string representation of the employee ID
	 * @return the {@link Employee} object corresponding to the given ID, or null if
	 *         the input is invalid
	 */
	@Override
	public Employee getAsObject(final FacesContext context, final UIComponent component, final String value) {
		if (value == null) {
			return null;
		}
		try {
			final long employeeId = Long.parseLong(value);
			return this.employeeService.findEmployeeById(employeeId);
		} catch (final NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Converts an {@link Employee} object into its string representation, which is
	 * the employee's ID.
	 *
	 * @param context   the {@link FacesContext} for the current request
	 * @param component the {@link UIComponent} associated with this conversion
	 * @param value     the {@link Employee} object to be converted
	 * @return the string representation of the employee ID, or null if the input is
	 *         null
	 */
	@Override
	public String getAsString(final FacesContext context, final UIComponent component, final Employee value) {
		if (value == null) {
			return null;
		}
		return String.valueOf(value.getId());
	}

	/**
	 * Sets the {@link EmployeeService} used to look up employees by ID.
	 *
	 * @param employeeService the service to be used for employee lookup
	 * @throws NullPointerException if the provided service is null
	 */
	public void setEmployeeService(final EmployeeService employeeService) {
		this.employeeService = Objects.requireNonNull(employeeService);
	}
}
