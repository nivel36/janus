package es.nivel36.janus.web;

import java.util.Objects;

import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Inject;

@FacesConverter(forClass = Employee.class, managed = true)
public class EmployeeConverter implements Converter<Employee> {

	private @Inject EmployeeService employeeService;

	@Override
	public Employee getAsObject(FacesContext context, UIComponent component, String value) {
		if (value == null) {
			return null;
		}
		try {
			long employeeId = Long.parseLong(value);
			return employeeService.findEmployeeById(employeeId);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Employee value) {
		if (value == null) {
			return null;
		}
		return String.valueOf(value.getId());
	}

	public void setEmployeeService(EmployeeService employeeService) {
		this.employeeService = Objects.requireNonNull(employeeService);
	}
}
