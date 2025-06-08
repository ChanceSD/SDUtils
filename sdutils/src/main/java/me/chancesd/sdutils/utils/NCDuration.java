package me.chancesd.sdutils.utils;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class that adds functionality to java's Duration
 * <br>
 * Since we're forced to use java 8 for now, this class replicates some methods added in java 9
 *
 */
public class NCDuration {

	private static final Pattern pattern = Pattern.compile("(\\d+)([smhd])");
	private static final int HOURS_PER_DAY = 24;
	private static final int MINUTES_PER_HOUR = 60;
	private static final int SECONDS_PER_MINUTE = 60;
	private static final int SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR;
	private static final int SECONDS_PER_DAY = SECONDS_PER_HOUR * HOURS_PER_DAY;
	private final Duration duration;

	private NCDuration(final Duration duration) {
		this.duration = duration;
	}
	public static NCDuration between(final Temporal startInclusive, final Temporal endExclusive) {
		return new NCDuration(Duration.between(startInclusive, endExclusive));
	}

	public static NCDuration ofMillis(final long millis) {
		return new NCDuration(Duration.ofMillis(millis));
	}

	/**
	 * Parse a duration string into an NCDuration object.
	 * Supports formats like "30s", "5m", "2h", "1d", or combinations like "1h30m15s"
	 *
	 * @param durationString the duration string to parse
	 * @return the parsed NCDuration
	 * @throws IllegalArgumentException if the duration string is invalid
	 */
	public static NCDuration parseDuration(final String durationString) {
		if (durationString == null || durationString.trim().isEmpty()) {
			throw new IllegalArgumentException("Duration string cannot be null or empty");
		}

		final Matcher matcher = pattern.matcher(durationString.toLowerCase());

		long totalMillis = 0;
		boolean foundMatch = false;

		while (matcher.find()) {
			foundMatch = true;
			final long value = Long.parseLong(matcher.group(1));
			final String unit = matcher.group(2);

			switch (unit) {
				case "s":
					totalMillis += value * 1000;
					break;
				case "m":
					totalMillis += value * 60 * 1000;
					break;
				case "h":
					totalMillis += value * 60 * 60 * 1000;
					break;
				case "d":
					totalMillis += value * 24 * 60 * 60 * 1000;
					break;
				default:
					throw new IllegalArgumentException("Invalid time unit: " + unit);
			}
		}

		if (!foundMatch) {
			throw new IllegalArgumentException("Invalid duration format: " + durationString + ". Expected format like '30s', '5m', '2h', '1d' or combinations like '1h30m'");
		}

		return ofMillis(totalMillis);
	}

	public int toDaysPart() {
		return (int) (duration.getSeconds() / SECONDS_PER_DAY);
	}

	public int toHoursPart() {
		return (int) (duration.toHours() % 24);
	}

	public int toMinutesPart() {
		return (int) (duration.toMinutes() % MINUTES_PER_HOUR);
	}

	public int toSecondsPart() {
		return (int) (duration.getSeconds() % SECONDS_PER_MINUTE);
	}

	public int toMillisPart() {
		return duration.getNano() / 1000_000;
	}

	public NCDuration plusMillis(final long milis) {
		return new NCDuration(duration.plusMillis(milis));
	}

	public int get(final ChronoUnit unit) {
		switch (unit) {
		case DAYS:
			return toDaysPart();
		case HOURS:
			return toHoursPart();
		case MILLIS:
			return toMillisPart();
		case MINUTES:
			return toMinutesPart();
		case SECONDS:
			return toSecondsPart();
		default:
			return 0;
		}
	}

	public Duration getDuration() {
		return duration;
	}

}
