package es.nivel36.janus.web;

import java.time.Duration;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

/**
 * Converter for handling {@link java.time.Duration} objects in JSF. This
 * converter allows bidirectional conversion between a {@link Duration} object
 * and a formatted string representing the duration in "HH:mm:ss" (hours,
 * minutes, and seconds) format.
 *
 * Example: - Input string: "02:30:45" will be converted to a Duration of 2
 * hours, 30 minutes, and 45 seconds. - Output string: A {@link Duration} of 2
 * hours, 30 minutes, and 45 seconds will be formatted as "02:30:45".
 *
 * The converter is registered with the ID "durationConverter" and can be used
 * in JSF pages by specifying the converter ID in the corresponding component.
 *
 * <pre>{@code
 * <h:outputText value="#{bean.duration}">
 *     <f:converter converterId="durationConverter" />
 * </h:outputText>
 * }</pre>
 *
 * Note: This converter assumes the input format is always in "HH:mm:ss". If the
 * input is not properly formatted, an {@link IllegalArgumentException} will be
 * thrown.
 *
 */
@FacesConverter("durationConverter")
public class DurationConverter implements Converter<Duration> {

	/**
	 * Converts a String to a {@link Duration} object. The input string should be in
	 * the format "HH:mm:ss" where "HH" represents hours, "mm" represents minutes,
	 * and "ss" represents seconds.
	 *
	 * @param context   the {@link FacesContext} for the request being processed
	 * @param component the {@link UIComponent} associated with this conversion
	 * @param value     the input String value to be converted
	 * @return a {@link Duration} object representing the input value, or null if
	 *         the input is empty or null
	 * @throws IllegalArgumentException if the input string is not in the expected
	 *                                  "HH:mm:ss" format
	 */
	@Override
	public Duration getAsObject(final FacesContext context, final UIComponent component, final String value) {
		if (value == null || value.trim().isEmpty()) {
			return null;
		}

		try {
			final String[] parts = value.split(":");
			final long hours = Long.parseLong(parts[0]);
			final long minutes = Long.parseLong(parts[1]);
			final long seconds = Long.parseLong(parts[2]);
			return Duration.ofHours(hours).plusMinutes(minutes).plusSeconds(seconds);
		} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
			throw new IllegalArgumentException("Invalid format for Duration. Expected format: HH:mm:ss", e);
		}
	}

	/**
	 * Converts a {@link Duration} object to a formatted string. The output format
	 * is "HH:mm:ss", where "HH" represents hours, "mm" represents minutes, and "ss"
	 * represents seconds.
	 *
	 * @param context   the {@link FacesContext} for the request being processed
	 * @param component the {@link UIComponent} associated with this conversion
	 * @param duration  the {@link Duration} object to be converted
	 * @return a String in the format "HH:mm:ss" representing the {@link Duration},
	 *         or null if the input is null
	 */
	@Override
	public String getAsString(final FacesContext context, final UIComponent component, final Duration duration) {
		if (duration == null) {
			return null;
		}
		final long hours = duration.toHours();
		final long minutes = duration.toMinutes() % 60;
		final long seconds = duration.getSeconds() % 60;
		return String.format("%d:%02d:%02d", hours, minutes, seconds);
	}
}
