package es.nivel36.janus.web;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;

import es.nivel36.janus.service.employee.Employee;
import es.nivel36.janus.service.timelog.TimeLog;
import es.nivel36.janus.service.timelog.TimeLogService;

/**
 * Lazy loading model for paginating and filtering {@link TimeLog} entries
 * associated with an {@link Employee}. This class is used in conjunction with
 * PrimeFaces DataTable to handle server-side pagination, sorting, and filtering
 * of time log records for a specific employee.
 *
 * <p>
 * It extends {@link LazyDataModel} to override methods that support pagination
 * and loading of data on demand.
 * </p>
 *
 * <p>
 * It relies on {@link TimeLogService} for retrieving the data from the backend.
 * </p>
 *
 * @see LazyDataModel
 * @see TimeLogService
 */
public class LazyTimeLogDataModel extends LazyDataModel<TimeLog> {

	private static final long serialVersionUID = 1L;

	private transient TimeLogService timeLogService;
	private final Employee employee;

	/**
	 * Constructs a new {@code LazyTimeLogDataModel} for the given {@code employee}
	 * and {@code timeLogService}.
	 *
	 * @param timeLogService the service responsible for handling time log
	 *                       operations. It cannot be null.
	 * @param employee       the employee whose time logs will be displayed. It
	 *                       cannot be null.
	 * @throws NullPointerException if {@code timeLogService} or {@code employee} is
	 *                              null.
	 */
	public LazyTimeLogDataModel(final TimeLogService timeLogService, final Employee employee) {
		this.timeLogService = Objects.requireNonNull(timeLogService, "TimeLogService can't be null");
		this.employee = Objects.requireNonNull(employee, "Employee can't be null");
	}

	/**
	 * Counts the total number of {@link TimeLog} entries for the current
	 * {@link Employee}.
	 *
	 * @param filterBy the filter criteria to apply when counting the time logs. Not
	 *                 used in this implementation.
	 * @return the total number of time logs associated with the employee.
	 */
	@Override
	public int count(final Map<String, FilterMeta> filterBy) {
		return (int) this.timeLogService.countTimeLogsByEmployee(this.employee);
	}

	/**
	 * Loads a subset of {@link TimeLog} entries for the current {@link Employee}
	 * with pagination and sorting.
	 *
	 * @param first    the index of the first record to load (zero-based).
	 * @param pageSize the number of records to load per page.
	 * @param sortBy   the sorting criteria to apply when loading the data. Not used
	 *                 in this implementation.
	 * @param filterBy the filtering criteria to apply when loading the data. Not
	 *                 used in this implementation.
	 * @return a list of {@link TimeLog} entries for the specified employee.
	 * @throws IllegalArgumentException if {@code first} is less than 0 or
	 *                                  {@code pageSize} is less than 1.
	 */
	@Override
	public List<TimeLog> load(final int first, final int pageSize, final Map<String, SortMeta> sortBy,
			final Map<String, FilterMeta> filterBy) {
		if (first < 0) {
			throw new IllegalArgumentException(
					String.format("First position is %s, but cannot be less than 0.", first));
		}

		if (pageSize < 1) {
			throw new IllegalArgumentException(String.format("Page size is %s, but must be greater than 0.", pageSize));
		}

		return this.timeLogService.findTimeLogsByEmployee(this.employee, first, pageSize);
	}
}
