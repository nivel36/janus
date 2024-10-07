package es.nivel36.janus.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DurationConverterTest {

    private DurationConverter durationConverter;

    @BeforeEach
    void setUp() {
        this.durationConverter = new DurationConverter();
    }

    @Test
    void testGetAsStringValidDuration() {
        // Arrange
        final Duration duration = Duration.ofHours(5).plusMinutes(30).plusSeconds(15);

        // Act
        final String result = this.durationConverter.getAsString(null, null, duration);

        // Assert
        assertEquals("5:30:15", result);
    }

    @Test
    void testGetAsStringNullDuration() {
        // Act
        final String result = this.durationConverter.getAsString(null, null, null);

        // Assert
        assertNull(result);
    }

    @Test
    void testGetAsObjectValidString() {
        // Act
        final Duration result = this.durationConverter.getAsObject(null, null, "5:30:15");

        // Assert
        assertEquals(Duration.ofHours(5).plusMinutes(30).plusSeconds(15), result);
    }

    @Test
    void testGetAsObjectNullString() {
        // Act
        final Duration result = this.durationConverter.getAsObject(null, null, null);

        // Assert
        assertNull(result);
    }

    @Test
    void testGetAsObjectInvalidFormat() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            this.durationConverter.getAsObject(null, null, "invalid");
        });
    }

    @Test
    void testGetAsObjectEmptyString() {
        // Act
        final Duration result = this.durationConverter.getAsObject(null, null, "");

        // Assert
        assertNull(result);
    }

    @Test
    void testGetAsObjectPartialFormat() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            this.durationConverter.getAsObject(null, null, "5:30");
        });
    }
}
