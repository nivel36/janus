package es.nivel36.janus.web.core.component;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import es.nivel36.janus.service.schedule.TimeRange;
import es.nivel36.janus.service.timelog.TimeLog;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;
import jakarta.faces.render.FacesRenderer;
import jakarta.faces.render.Renderer;

/*
* Custom JSF renderer for the WorkShiftComponent. This renderer is responsible
* for generating HTML output to visually represent work shifts, including time logs,
* start/end times, and pauses between segments.
*
* The renderer produces a structure with two main parts:
* - A bar representing the time segments.
* - A sidebar showing start and end times for the segments.
*/
@FacesRenderer(componentFamily = "es.nivel36.components", rendererType = "es.nivel36.WorkShiftRenderer")
public class WorkShiftRenderer extends Renderer<WorkShiftComponent> {

	private static final String DIV = "div";
	private static final String SPAN = "span";
	private static final String STYLE = "style";
	private static final String CLASS = "class";

	/**
	 * Encodes the beginning of the component, generating HTML for the work shift.
	 *
	 * @param context   the FacesContext for the request
	 * @param workShift the WorkShiftComponent being rendered
	 * @throws IOException if an error occurs during writing
	 */
	public void encodeBegin(final FacesContext context, final WorkShiftComponent workShift) throws IOException {
		final ResponseWriter writer = context.getResponseWriter();
		writer.startElement(DIV, workShift);
		writer.writeAttribute(CLASS, "work-shift", null);

		writer.startElement(DIV, workShift);
		writer.writeAttribute(CLASS, "work-shift-bar", null);
		final List<TimeLog> segments = workShift.getWorkShift().getTimeLogs();
		final TimeRange timeRange = getTimeRange(workShift, segments);
		if (!segments.isEmpty()) {
			for (int i = 0; i < segments.size(); i++) {
				final TimeLog segment = segments.get(i);
				this.writeWorkSegment(workShift, writer, timeRange, i, segment);
				if (i < segments.size() - 1) {
					this.writePauseSegment(workShift, writer, segments, timeRange, i, segment);
				}
			}
		}
		writer.endElement(DIV);

		writer.startElement(DIV, workShift);
		writer.writeAttribute(CLASS, "work-shift-sidebar", null);
		writer.startElement(DIV, workShift);
		writer.writeAttribute(CLASS, "segment", null);
		writer.writeAttribute(STYLE, "flex: 0 0 100%", null);
		this.writeStartHour(workShift, writer);
		this.writeEndHour(workShift, writer);
		writer.endElement(DIV);
		writer.endElement(DIV);
		writer.endElement(DIV);
	}

	private TimeRange getTimeRange(final WorkShiftComponent workShift, final List<TimeLog> segments) {
		final TimeRange timeRange = workShift.getTimeRange();
		if (timeRange == null && !segments.isEmpty()) {
			// We are in an off day without time range. Time range is the working hours
			return new TimeRange(segments.getFirst().getEntryTime().toLocalTime(),
					segments.getLast().getExitTime().toLocalTime());
		}
		return timeRange;
	}

	private void writePauseSegment(final WorkShiftComponent workShift, final ResponseWriter writer,
			final List<TimeLog> segments, final TimeRange timeRange, final int i, final TimeLog segment)
			throws IOException {
		final int percentage = this.calculatePercentage(segment, segments.get(i + 1), timeRange);
		this.writeSegment(workShift, writer, percentage, "segment pause-segment");
	}

	private int calculatePercentage(final TimeLog timeLog, final TimeRange timeRange) {
		return this.calculatePercentage(timeLog.getEntryTime(), timeLog.getExitTime(), timeRange);
	}

	private int calculatePercentage(final TimeLog timeLog1, final TimeLog timeLog2, final TimeRange timeRange) {
		return this.calculatePercentage(timeLog1.getExitTime(), timeLog2.getEntryTime(), timeRange);
	}

	private int calculatePercentage(final LocalDateTime start, final LocalDateTime end, final TimeRange timeRange) {
		final Duration a = Duration.between(timeRange.getStartTime(), timeRange.getEndTime());
		final Duration b = Duration.between(start, end);
		if (b.getSeconds() == 0) {
			return 0;
		}
		return (int) (100 * b.getSeconds() / a.getSeconds());
	}
	
	private void writeSegment(final WorkShiftComponent workShift, final ResponseWriter writer, final Integer percentage,
			final String styleClass) throws IOException {
		writer.startElement(DIV, workShift);
		writer.writeAttribute(CLASS, styleClass, null);
		writer.writeAttribute(STYLE, String.format("flex: 0 0 %2d%%", percentage), null);
		writer.endElement(DIV);
	}

	private void writeWorkSegment(final WorkShiftComponent workShift, final ResponseWriter writer,
			final TimeRange timeRange, final int i, final TimeLog segment) throws IOException {
		writer.startElement(DIV, workShift);
		writer.writeAttribute(CLASS, "segment work-segment", null);
		final int percentage = this.calculatePercentage(segment, timeRange);
		writer.writeAttribute(STYLE, String.format("flex: 0 0 %2d%%;", percentage), null);
		this.writeStartTimeLogHour(workShift, segment, writer);
		this.writeSeparatorTimeLogHour(workShift, segment, writer);
		this.writeEndTimeLogHour(workShift, segment, writer);
		writer.endElement(DIV);
	}

	private void writeStartTimeLogHour(final WorkShiftComponent workShift, final TimeLog timeLog,
			final ResponseWriter writer) throws IOException {
		if (workShift.getTimeRange() != null) {
			writer.startElement(SPAN, workShift);
			writer.writeAttribute(CLASS, "start-timelog-hour", null);
			writer.write(timeLog.getEntryTime().format(DateTimeFormatter.ofPattern("HH:mm")));
			writer.endElement(SPAN);
		}
	}
	
	private void writeSeparatorTimeLogHour(final WorkShiftComponent workShift, final TimeLog timeLog,
			final ResponseWriter writer) throws IOException {
		if (workShift.getTimeRange() != null) {
			writer.startElement(SPAN, workShift);
			writer.writeAttribute(CLASS, "separator-timelog-hour", null);
			writer.write(" - ");
			writer.endElement(SPAN);
		}
	}

	private void writeEndTimeLogHour(final WorkShiftComponent workShift, final TimeLog timeLog,
			final ResponseWriter writer) throws IOException {
		if (workShift.getTimeRange() != null) {
			writer.startElement(SPAN, workShift);
			writer.writeAttribute(CLASS, "end-timelog-hour", null);
			writer.write(timeLog.getExitTime().format(DateTimeFormatter.ofPattern("HH:mm")));
			writer.endElement(SPAN);
		}
	}

	private void writeStartHour(final WorkShiftComponent workShift, final ResponseWriter writer) throws IOException {
		if (workShift.getTimeRange() != null) {
			writer.startElement(SPAN, workShift);
			writer.writeAttribute(CLASS, "start-hour", null);
			writer.write(workShift.getTimeRange().getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")));
			writer.endElement(SPAN);
		}
	}

	private void writeEndHour(final WorkShiftComponent workShift, final ResponseWriter writer) throws IOException {
		if (workShift.getTimeRange() != null) {
			writer.startElement(SPAN, workShift);
			writer.writeAttribute(CLASS, "end-hour", null);
			writer.write(workShift.getTimeRange().getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")));
			writer.endElement(SPAN);
		}
	}
}
