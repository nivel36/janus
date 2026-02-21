package es.nivel36.janus.util;

/**
 * Utility class providing common {@link String}-related validation helpers.
 *
 * <p>
 * This class cannot be instantiated and exposes only static utility methods.
 * </p>
 */
public class Strings {

	/**
	 * Private constructor to prevent instantiation.
	 */
	private Strings() {
	}

	/**
	 * Ensures that the given {@link String} is neither {@code null} nor blank.
	 *
	 * <p>
	 * A string is considered blank if it is empty or contains only whitespace
	 * characters, as defined by {@link String#isBlank()}.
	 * </p>
	 *
	 * @param value   the string value to validate
	 * @param message the exception message to use if validation fails
	 *
	 * @return the validated {@code value}
	 *
	 * @throws NullPointerException     if {@code value} is {@code null}
	 * @throws IllegalArgumentException if {@code value} is blank
	 */
	public static String requireNonBlank(String value, String message) {
		if (value == null) {
			throw new NullPointerException(message);
		}
		if (value.isBlank()) {
			throw new IllegalArgumentException(message);
		}
		return value;
	}
}
