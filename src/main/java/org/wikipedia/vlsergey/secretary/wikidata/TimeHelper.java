package org.wikipedia.vlsergey.secretary.wikidata;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.DataValue;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.TimeValue;

@Component
public class TimeHelper {

	private static final OffsetDateTime BORDER = OffsetDateTime.parse("1582-10-15T00:00:00Z");

	/**
	 * Convert the parsed date represented in Julian calendar into one
	 * represented by Julian
	 */
	public static TemporalAccessor fixJulianToGrigorian(TemporalAccessor dateTime) {
		GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		calendar.setGregorianChange(new Date(Long.MAX_VALUE));
		calendar.set(dateTime.get(ChronoField.YEAR), dateTime.get(ChronoField.MONTH_OF_YEAR) - 1,
				dateTime.get(ChronoField.DAY_OF_MONTH));
		calendar.setGregorianChange(new Date(Long.MIN_VALUE));
		return calendar.toZonedDateTime();
	}

	public List<DataValue> parse(String t) {
		t = t.trim();
		boolean appendJulian = false;
		if (t.matches("^[\\.0-9]+\\s*\\([\\.0-9]+\\)$")) {
			appendJulian = true;
			t = StringUtils.substringBefore(t, "(");
			t = t.trim();
		}

		try {
			final DateTimeFormatter parser = DateTimeFormatter.ofPattern("d.M.u").withZone(ZoneOffset.UTC);
			TemporalAccessor parsed = parser.parse(t);
			final TimeValue timeValue = new TimeValue(TimeValue.PRECISION_DAY, parsed);
			if (appendJulian) {
				timeValue.setCalendarModel(TimeValue.CALENDAR_JULIAN);
			} else {
				OffsetDateTime dateTime = OffsetDateTime.from(timeValue.getTime());
				if (dateTime.isBefore(BORDER)) {
					// date is obviously is julian
					timeValue.setTime(fixJulianToGrigorian(timeValue.getTime()));
					timeValue.setCalendarModel(TimeValue.CALENDAR_JULIAN);
				}
			}
			return Collections.singletonList(timeValue);
		} catch (DateTimeParseException exc) {
		}

		try {
			final DateTimeFormatter parser = DateTimeFormatter.ofPattern("u");
			TemporalAccessor date = parser.parse(t);
			final TimeValue timeValue = new TimeValue(TimeValue.PRECISION_YEAR, date);
			if (appendJulian) {
				timeValue.setCalendarModel(TimeValue.CALENDAR_JULIAN);
			} else {
				OffsetDateTime dateTime = OffsetDateTime.from(timeValue.getTime());
				if (dateTime.isBefore(BORDER)) {
					timeValue.setCalendarModel(TimeValue.CALENDAR_JULIAN);
				}
			}
			return Collections.singletonList(timeValue);
		} catch (DateTimeParseException exc) {
		}

		throw new UnsupportedParameterValue(t);
	}
}
