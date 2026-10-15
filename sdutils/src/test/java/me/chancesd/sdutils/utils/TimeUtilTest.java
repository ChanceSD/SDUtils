package me.chancesd.sdutils.utils;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

class TimeUtilTest {

	private static final Instant START = Instant.EPOCH;
	private static final TimeUtil.TimeLangProvider LANG = new TimeUtil.TimeLangProvider() {
		@Override
		public String getTime(final ChronoUnit time) {
			switch (time) {
			case DAYS:
				return "d";
			case HOURS:
				return "h";
			case MINUTES:
				return "m";
			case SECONDS:
				return "s";
			case FOREVER:
				return "now";
			default:
				return "";
			}
		}
	};

	@Test
	void formatsTheFinalSecondAsDecimalSeconds() {
		assertAll(
				() -> assertEquals("0.1s", formatUntil(1)),
				() -> assertEquals("0.1s", formatUntil(50)),
				() -> assertEquals("0.1s", formatUntil(51)),
				() -> assertEquals("0.5s", formatUntil(500)),
				() -> assertEquals("0.9s", formatUntil(999)),
				() -> assertEquals("1s", formatUntil(1000)),
				() -> assertEquals("1s", formatUntil(1499)),
				() -> assertEquals("2s", formatUntil(1500)),
				() -> assertEquals("2s", formatUntil(1999)),
				() -> assertEquals("2s", formatUntil(2000)));
	}

	@Test
	void treatsPastTimesAsNow() {
		assertEquals("now", TimeUtil.getDiffMsg(LANG, START, START.minusMillis(1)));
	}

	@Test
	void preservesComponentFormattingForLongerDurations() {
		assertEquals("1m 1s", formatUntil(61_000));
	}

	@Test
	void roundsComponentValuesToTheNearestSecond() {
		assertAll(
				() -> assertEquals("59s", formatUntil(59_499)),
				() -> assertEquals("1m", formatUntil(59_500)),
				() -> assertEquals("1m 59s", formatUntil(119_499)),
				() -> assertEquals("2m", formatUntil(119_500)));
	}

	private static String formatUntil(final long millis) {
		return TimeUtil.getDiffMsg(LANG, START, START.plus(millis, ChronoUnit.MILLIS));
	}
}
