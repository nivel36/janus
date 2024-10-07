package es.nivel36.janus.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.employee.EmployeeService;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;

class EmployeeConverterTest {

    private @Mock EmployeeService employeeService;
    private @InjectMocks  EmployeeConverter employeeConverter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAsObjectValidId() {
        // Arrange
        String employeeId = "1";
        Employee expectedEmployee = new Employee();
        expectedEmployee.setId(1L);

        when(employeeService.findEmployeeById(1L)).thenReturn(expectedEmployee);

        // Act
        Employee result = employeeConverter.getAsObject(mock(FacesContext.class), mock(UIComponent.class), employeeId);

        // Assert
        assertEquals(expectedEmployee, result);
    }

    @Test
    void testGetAsObjectInvalidId() {
        // Arrange
        String invalidId = "invalid";

        // Act
        Employee result = employeeConverter.getAsObject(mock(FacesContext.class), mock(UIComponent.class), invalidId);

        // Assert
        assertNull(result);
    }

    @Test
    void testGetAsObjectNullValue() {
        // Act
        Employee result = employeeConverter.getAsObject(mock(FacesContext.class), mock(UIComponent.class), null);

        // Assert
        assertNull(result);
    }

    @Test
    void testGetAsStringValidEmployee() {
        // Arrange
        Employee employee = new Employee();
        employee.setId(1L);

        // Act
        String result = employeeConverter.getAsString(mock(FacesContext.class), mock(UIComponent.class), employee);

        // Assert
        assertEquals("1", result);
    }

    @Test
    void testGetAsStringNullEmployee() {
        // Act
        String result = employeeConverter.getAsString(mock(FacesContext.class), mock(UIComponent.class), null);

        // Assert
        assertNull(result);
    }
}
