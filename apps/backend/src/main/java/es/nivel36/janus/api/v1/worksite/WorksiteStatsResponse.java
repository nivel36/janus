package es.nivel36.janus.api.v1.worksite;

import java.time.Instant;

/**
 * Statistics for a worksite over an inclusive time interval.
 *
 * @param worksiteCode                         unique code of the worksite
 * @param startInclusive                       start of the interval, included
 * @param endInclusive                         end of the interval, included
 * @param employeesWhoClockedIn               number of distinct employees with
 *                                            a time log in the interval
 * @param erroneousTimeLogs                   number of open time logs in the
 *                                            interval
 * @param totalTimeLogs                       total number of time logs in the
 *                                            interval
 * @param employeesAllowedToClockIn           number of employees assigned to
 *                                            the worksite
 * @param distinctSchedulesFromEmployeesWhoClockedIn number of distinct
 *                                                  schedules among employees
 *                                                  with a time log in the
 *                                                  interval
 */
public record WorksiteStatsResponse(String worksiteCode, Instant startInclusive, Instant endInclusive,
		long employeesWhoClockedIn, long erroneousTimeLogs, long totalTimeLogs, long employeesAllowedToClockIn,
		long distinctSchedulesFromEmployeesWhoClockedIn) {
}
