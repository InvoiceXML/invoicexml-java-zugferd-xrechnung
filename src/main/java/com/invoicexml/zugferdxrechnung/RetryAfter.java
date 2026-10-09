package com.invoicexml.zugferdxrechnung;

import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Parses the {@code Retry-After} header: delay seconds or an HTTP date. */
final class RetryAfter {

    private RetryAfter() {
    }

    /**
     * Returns the delay, or {@code null} when the header is absent or unusable.
     */
    static Duration parse(String value, Clock clock) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String v = value.trim();
        try {
            long seconds = Long.parseLong(v);
            return seconds < 0 ? null : Duration.ofSeconds(seconds);
        } catch (NumberFormatException ignored) {
            // not delay-seconds, try an HTTP date
        }
        try {
            ZonedDateTime at = ZonedDateTime.parse(v, DateTimeFormatter.RFC_1123_DATE_TIME);
            Duration d = Duration.between(clock.instant(), at.toInstant());
            return d.isNegative() ? Duration.ZERO : d;
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
