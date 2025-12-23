package es.nivel36.janus.service.timelog;

import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Immutable value object representing a validated, ordered collection of closed
 * {@link TimeLog} entries.
 *
 * <p>
 * This class guarantees by construction that:
 * </p>
 * <ul>
 * <li>The collection is not {@code null}</li>
 * <li>No {@link TimeLog} is {@code null}</li>
 * <li>All time logs are closed</li>
 * <li>The collection is ordered by {@code entryTime} ascending</li>
 * <li>No time ranges overlap</li>
 * </ul>
 */
public final class TimeLogs implements Iterable<TimeLog> {

	private final static TimeLogs EMPTY = new TimeLogs(List.of());

	private final List<TimeLog> timeLogs;

	/**
	 * Creates a {@code TimeLogs} instance from the given collection.
	 *
	 * @param timeLogs the collection of {@link TimeLog} entries
	 * @throws NullPointerException     if the collection or any element is
	 *                                  {@code null}
	 * @throws IllegalArgumentException if any time log is open or overlaps with
	 *                                  another
	 */
	public TimeLogs(final Collection<TimeLog> timeLogs) {
		Objects.requireNonNull(timeLogs, "timeLogs cannot be null");

		final List<TimeLog> sorted = timeLogs.stream()
				.peek(t -> Objects.requireNonNull(t, "TimeLog element cannot be null")).filter(TimeLog::isClosed)
				.sorted(Comparator.comparing(TimeLog::getEntryTime)).toList();

		if (sorted.size() != timeLogs.size()) {
			throw new IllegalArgumentException("All TimeLogs must be closed");
		}
		assertNoOverlaps(sorted);
		this.timeLogs = List.copyOf(sorted);
	}

	private static void assertNoOverlaps(final List<TimeLog> logs) {
		for (int i = 1; i < logs.size(); i++) {
			final TimeLog previous = logs.get(i - 1);
			final TimeLog current = logs.get(i);
			if (!previous.getExitTime().isBefore(current.getEntryTime())) {
				throw new IllegalArgumentException(
						String.format("Overlapping TimeLogs detected: %s and %s", previous, current));
			}
		}
	}

	/**
	 * Returns an unmodifiable view of the underlying time logs.
	 */
	public List<TimeLog> asList() {
		return this.timeLogs;
	}

	/**
	 * Returns the index of the specified {@link TimeLog} in this collection.
	 *
	 * <p>
	 * Equality is determined using {@link TimeLog#equals(Object)}.
	 * </p>
	 *
	 * @param timeLog the {@link TimeLog} to locate; must not be {@code null}
	 * @return the index of the time log, or {@code -1} if not present
	 * @throws NullPointerException if {@code timeLog} is {@code null}
	 */
	public int indexOf(final TimeLog timeLog) {
		Objects.requireNonNull(timeLog, "timeLog cannot be null");
		return this.timeLogs.indexOf(timeLog);
	}

	/**
	 * Returns the total worked duration across all time logs.
	 */
	public Duration getTotalDuration() {
		return timeLogs.stream().map(TimeLog::getWorkDuration).reduce(Duration.ZERO, Duration::plus);
	}

	@Override
	public Iterator<TimeLog> iterator() {
		return timeLogs.iterator();
	}

	public boolean isEmpty() {
		return timeLogs.isEmpty();
	}

	public int size() {
		return timeLogs.size();
	}

	/**
	 * Creates a new {@code TimeLogs} instance containing the elements in the
	 * specified range.
	 *
	 * @param fromIndex the starting index (inclusive)
	 * @param toIndex   the ending index (exclusive)
	 * @return a new {@code TimeLogs} instance
	 * @throws IndexOutOfBoundsException if indices are out of range
	 * @throws IllegalArgumentException  if {@code fromIndex > toIndex}
	 */
	public TimeLogs slice(final int fromIndex, final int toIndex) {
		if (fromIndex < 0 || toIndex > this.timeLogs.size()) {
			throw new IndexOutOfBoundsException(
					String.format("Indexs out of range: from=%d, to=%d", fromIndex, toIndex));
		}
		if (fromIndex > toIndex) {
			throw new IllegalArgumentException(
					String.format("fromIndex (%d) must be <= toIndex (%d)", fromIndex, toIndex));
		}
		return new TimeLogs(this.timeLogs.subList(fromIndex, toIndex));
	}

	/**
	 * Returns an empty {@code TimeLogs} instance.
	 *
	 * <p>
	 * The returned instance represents a valid, immutable collection with no
	 * {@link TimeLog} elements.
	 * </p>
	 *
	 * @return an empty {@code TimeLogs}
	 */
	public static TimeLogs empty() {
		return EMPTY;
	}
}
