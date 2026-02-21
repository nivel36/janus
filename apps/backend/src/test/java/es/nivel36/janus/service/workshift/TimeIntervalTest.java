package es.nivel36.janus.service.workshift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class TimeIntervalTest {

	@Test
	void testCreateTimeIntervalSuccess() {
		final Instant start = Instant.parse("2025-01-01T10:00:00Z");
		final Instant end = Instant.parse("2025-01-01T11:00:00Z");

		final TimeInterval interval = new TimeInterval(start, end);

		assertEquals(start, interval.startsAt());
		assertEquals(end, interval.endsAt());
		assertEquals(Duration.ofHours(1), interval.duration());
	}

	@Test
	void testCreateTimeIntervalWithNullStartFailure() {
		final Instant end = Instant.now();
		assertThrows(NullPointerException.class, () -> new TimeInterval(null, end));
	}

	@Test
	void testCreateTimeIntervalWithNullEndFailure() {
		final Instant start = Instant.now();
		assertThrows(NullPointerException.class, () -> new TimeInterval(start, null));
	}

	@Test
	void testCreateTimeIntervalWithEndBeforeStartFailure() {
		final Instant start = Instant.parse("2025-01-01T11:00:00Z");
		final Instant end = Instant.parse("2025-01-01T10:00:00Z");

		assertThrows(IllegalArgumentException.class, () -> new TimeInterval(start, end));
	}

	@Test
	void testOverlapsSuccess() {
		final TimeInterval a = new TimeInterval(
				Instant.parse("2025-01-01T10:00:00Z"),
				Instant.parse("2025-01-01T11:00:00Z")
		);
		final TimeInterval b = new TimeInterval(
				Instant.parse("2025-01-01T10:30:00Z"),
				Instant.parse("2025-01-01T11:30:00Z")
		);

		assertTrue(a.overlaps(b));
		assertTrue(b.overlaps(a));
	}

	@Test
	void testOverlapsAdjacentIntervalsFailure() {
		final TimeInterval a = new TimeInterval(
				Instant.parse("2025-01-01T10:00:00Z"),
				Instant.parse("2025-01-01T11:00:00Z")
		);
		final TimeInterval b = new TimeInterval(
				Instant.parse("2025-01-01T11:00:00Z"),
				Instant.parse("2025-01-01T12:00:00Z")
		);

		assertFalse(a.overlaps(b));
		assertFalse(b.overlaps(a));
	}

	@Test
	void testOverlapsWithNullFailure() {
		final TimeInterval interval = new TimeInterval(Instant.now(), Instant.now().plusSeconds(1));
		assertThrows(NullPointerException.class, () -> interval.overlaps(null));
	}

	@Test
	void testTouchesSuccess() {
		final TimeInterval a = new TimeInterval(
				Instant.parse("2025-01-01T10:00:00Z"),
				Instant.parse("2025-01-01T11:00:00Z")
		);
		final TimeInterval b = new TimeInterval(
				Instant.parse("2025-01-01T11:00:00Z"),
				Instant.parse("2025-01-01T12:00:00Z")
		);

		assertTrue(a.touches(b));
		assertTrue(b.touches(a));
	}

	@Test
	void testTouchesOverlappingIntervalsFailure() {
		final TimeInterval a = new TimeInterval(
				Instant.parse("2025-01-01T10:00:00Z"),
				Instant.parse("2025-01-01T11:30:00Z")
		);
		final TimeInterval b = new TimeInterval(
				Instant.parse("2025-01-01T11:00:00Z"),
				Instant.parse("2025-01-01T12:00:00Z")
		);

		assertFalse(a.touches(b));
	}

	@Test
	void testOverlapsOrTouchesWithOverlapSuccess() {
		final TimeInterval a = new TimeInterval(
				Instant.parse("2025-01-01T10:00:00Z"),
				Instant.parse("2025-01-01T11:00:00Z")
		);
		final TimeInterval b = new TimeInterval(
				Instant.parse("2025-01-01T10:30:00Z"),
				Instant.parse("2025-01-01T11:30:00Z")
		);

		assertTrue(a.overlapsOrTouches(b));
	}

	@Test
	void testOverlapsOrTouchesWithTouchSuccess() {
		final TimeInterval a = new TimeInterval(
				Instant.parse("2025-01-01T10:00:00Z"),
				Instant.parse("2025-01-01T11:00:00Z")
		);
		final TimeInterval b = new TimeInterval(
				Instant.parse("2025-01-01T11:00:00Z"),
				Instant.parse("2025-01-01T12:00:00Z")
		);

		assertTrue(a.overlapsOrTouches(b));
	}

	@Test
	void testOverlapsOrTouchesDisjointFailure() {
		final TimeInterval a = new TimeInterval(
				Instant.parse("2025-01-01T10:00:00Z"),
				Instant.parse("2025-01-01T11:00:00Z")
		);
		final TimeInterval b = new TimeInterval(
				Instant.parse("2025-01-01T12:00:00Z"),
				Instant.parse("2025-01-01T13:00:00Z")
		);

		assertFalse(a.overlapsOrTouches(b));
	}

	@Test
	void testMergeWithOverlappingIntervalsSuccess() {
		final TimeInterval a = new TimeInterval(
				Instant.parse("2025-01-01T10:00:00Z"),
				Instant.parse("2025-01-01T11:00:00Z")
		);
		final TimeInterval b = new TimeInterval(
				Instant.parse("2025-01-01T10:30:00Z"),
				Instant.parse("2025-01-01T12:00:00Z")
		);

		final TimeInterval merged = a.mergeWith(b);

		assertEquals(Instant.parse("2025-01-01T10:00:00Z"), merged.startsAt());
		assertEquals(Instant.parse("2025-01-01T12:00:00Z"), merged.endsAt());
	}

	@Test
	void testMergeWithTouchingIntervalsSuccess() {
		final TimeInterval a = new TimeInterval(
				Instant.parse("2025-01-01T10:00:00Z"),
				Instant.parse("2025-01-01T11:00:00Z")
		);
		final TimeInterval b = new TimeInterval(
				Instant.parse("2025-01-01T11:00:00Z"),
				Instant.parse("2025-01-01T12:00:00Z")
		);

		final TimeInterval merged = a.mergeWith(b);

		assertEquals(Instant.parse("2025-01-01T10:00:00Z"), merged.startsAt());
		assertEquals(Instant.parse("2025-01-01T12:00:00Z"), merged.endsAt());
	}

	@Test
	void testMergeWithDisjointIntervalsFailure() {
		final TimeInterval a = new TimeInterval(
				Instant.parse("2025-01-01T10:00:00Z"),
				Instant.parse("2025-01-01T11:00:00Z")
		);
		final TimeInterval b = new TimeInterval(
				Instant.parse("2025-01-01T12:00:00Z"),
				Instant.parse("2025-01-01T13:00:00Z")
		);

		assertThrows(IllegalArgumentException.class, () -> a.mergeWith(b));
	}

	@Test
	void testIntersectOverlappingIntervalsSuccess() {
		final TimeInterval a = new TimeInterval(
				Instant.parse("2025-01-01T10:00:00Z"),
				Instant.parse("2025-01-01T12:00:00Z")
		);
		final TimeInterval b = new TimeInterval(
				Instant.parse("2025-01-01T11:00:00Z"),
				Instant.parse("2025-01-01T13:00:00Z")
		);

		final TimeInterval intersection = a.intersect(b);

		assertNotNull(intersection);
		assertEquals(Instant.parse("2025-01-01T11:00:00Z"), intersection.startsAt());
		assertEquals(Instant.parse("2025-01-01T12:00:00Z"), intersection.endsAt());
	}

	@Test
	void testIntersectNonOverlappingIntervalsReturnsNull() {
		final TimeInterval a = new TimeInterval(
				Instant.parse("2025-01-01T10:00:00Z"),
				Instant.parse("2025-01-01T11:00:00Z")
		);
		final TimeInterval b = new TimeInterval(
				Instant.parse("2025-01-01T11:00:00Z"),
				Instant.parse("2025-01-01T12:00:00Z")
		);

		assertNull(a.intersect(b));
	}

	@Test
	void testExpandBySuccess() {
		final TimeInterval interval = new TimeInterval(
				Instant.parse("2025-01-01T10:00:00Z"),
				Instant.parse("2025-01-01T11:00:00Z")
		);

		final TimeInterval expanded = interval.expandBy(Duration.ofMinutes(15));

		assertEquals(Instant.parse("2025-01-01T09:45:00Z"), expanded.startsAt());
		assertEquals(Instant.parse("2025-01-01T11:15:00Z"), expanded.endsAt());
	}

	@Test
	void testEndsAtOrBeforeSuccess() {
		final TimeInterval interval = new TimeInterval(
				Instant.parse("2025-01-01T10:00:00Z"),
				Instant.parse("2025-01-01T11:00:00Z")
		);

		assertTrue(interval.endsAtOrBefore(Instant.parse("2025-01-01T12:00:00Z")));
		assertFalse(interval.endsAtOrBefore(Instant.parse("2025-01-01T10:30:00Z")));
	}

	@Test
	void testStartsAtOrAfterSuccess() {
		final TimeInterval interval = new TimeInterval(
				Instant.parse("2025-01-01T10:00:00Z"),
				Instant.parse("2025-01-01T11:00:00Z")
		);

		assertTrue(interval.startsAtOrAfter(Instant.parse("2025-01-01T09:00:00Z")));
		assertFalse(interval.startsAtOrAfter(Instant.parse("2025-01-01T10:30:00Z")));
	}
}
