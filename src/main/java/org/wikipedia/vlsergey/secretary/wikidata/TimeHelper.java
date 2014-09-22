package org.wikipedia.vlsergey.secretary.wikidata;

import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiSnak;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.SnakType;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.TimeValue;

import com.frequal.romannumerals.Converter;

@Component
public class TimeHelper {

	private static final OffsetDateTime BORDER = OffsetDateTime.parse("1582-10-15T00:00:00Z");

	private static final DateTimeFormatter PARSER_YEAR = DateTimeFormatter.ofPattern("u");

	private static final Converter ROMAN_CONVERTER = new Converter();

	private static final Set<String> UNKNOWN = new HashSet<>(Arrays.asList("?", "неизвестно", "неизвестна"));

	/**
	 * Convert the parsed date represented in Julian calendar into one
	 * represented by Gregorian
	 */
	public static TemporalAccessor fixJulianToGrigorian(TemporalAccessor dateTime) {
		GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		calendar.setGregorianChange(new Date(Long.MAX_VALUE));
		calendar.clear();
		calendar.set(dateTime.get(ChronoField.YEAR), dateTime.get(ChronoField.MONTH_OF_YEAR) - 1,
				dateTime.get(ChronoField.DAY_OF_MONTH));
		calendar.setGregorianChange(new Date(Long.MIN_VALUE));
		return calendar.toZonedDateTime();
	}

	private List<ValueWithQualifiers> asCentury(EntityId property, int century) {
		TemporalAccessor temporalAccessor = PARSER_YEAR.parse((century * 100) + "");
		final TimeValue timeValue = new TimeValue(TimeValue.PRECISION_CENTURY, temporalAccessor);
		return ValueWithQualifiers.fromSnak(ApiSnak.newSnak(property, timeValue));
	}

	public ReconsiliationAction getAction(Collection<ValueWithQualifiers> wikipedia,
			Collection<ValueWithQualifiers> wikidata) {
		if (wikipedia.size() > 1) {
			return ReconsiliationAction.report_difference;
		}

		if (wikipedia.isEmpty()) {
			return ReconsiliationAction.remove_from_wikipedia;
		}

		if (wikidata.isEmpty()) {
			return ReconsiliationAction.set;
		}

		if (wikidata.containsAll(wikipedia)) {
			return ReconsiliationAction.remove_from_wikipedia;
		}

		// if (wikipedia.size() == 1 && wikidata.size() == 1) {
		// final ApiSnak snakP = wikipedia.iterator().next().getValue();
		// final ApiSnak snakD = wikidata.iterator().next().getValue();
		// if (snakP.hasValue() && !snakD.hasValue()) {
		// return ReconsiliationAction.replace;
		// }
		// if (!snakP.hasValue() && snakD.hasValue()) {
		// return ReconsiliationAction.remove_from_wikipedia;
		// }
		// if (snakP.hasValue() && snakP.hasValue()) {
		// TimeValue p = snakP.getTimeValue();
		// TimeValue d = snakD.getTimeValue();
		// if (p.morePreciseThan(d)) {
		// return ReconsiliationAction.replace;
		// }
		// if (d.morePreciseThan(p)) {
		// return ReconsiliationAction.remove_from_wikipedia;
		// }
		// if (p.getAfter() == d.getAfter() && p.getBefore() == d.getBefore()
		// && StringUtils.equals(p.getTimeString(), d.getTimeString())
		// && p.getPrecision() == d.getPrecision() && p.getTimezone() ==
		// d.getTimezone()
		// && TimeValue.CALENDAR_JULIAN.equals(p.getCalendarModel())
		// && TimeValue.CALENDAR_GRIGORIAN.equals(d.getCalendarModel())) {
		// return ReconsiliationAction.replace;
		// }
		// if (p.getAfter() == d.getAfter()
		// && p.getBefore() == d.getBefore()
		// && p.getPrecision() == d.getPrecision()
		// && p.getTimezone() == d.getTimezone()
		// && TimeValue.CALENDAR_JULIAN.equals(p.getCalendarModel())
		// && TimeValue.CALENDAR_GRIGORIAN.equals(d.getCalendarModel())
		// &&
		// OffsetDateTime.from(TimeHelper.fixJulianToGrigorian(d.getTime())).equals(
		// OffsetDateTime.from(p.getTime()))) {
		// // mistake on Wikidata -- Julian date entered as gregorian
		// return ReconsiliationAction.replace;
		// }
		// }
		// }

		return ReconsiliationAction.report_difference;
	}

	public List<ValueWithQualifiers> parse(EntityId property, String t) {
		t = t.trim();

		if (UNKNOWN.contains(t.toLowerCase())) {
			return ValueWithQualifiers.fromSnak(ApiSnak.newSnak(property, SnakType.somevalue));
		}

		boolean appendJulian = false;
		if (t.matches("^[\\.0-9]+\\s*\\([\\.0-9]+\\)$")) {
			appendJulian = true;
			t = StringUtils.substringBefore(t, "(");
			t = t.trim();
		}

		try {
			if (t.matches("^([0-9]+|[XIV]+)\\s*век$")) {
				final String strCentury = StringUtils.substringBefore(t, "век").trim();
				int century = parseCentury(strCentury);
				return asCentury(property, century);
			}
		} catch (NumberFormatException exc) {
		} catch (ParseException exc) {
		}

		try {
			if (t.matches("^\\[\\[\\s*([0-9]+|[XIV]+)\\s*век\\s*\\]\\]$")) {
				final String strCentury = StringUtils.substringBetween(t, "[[", "век").trim();
				int century = parseCentury(strCentury);
				return asCentury(property, century);
			}
		} catch (NumberFormatException exc) {
		} catch (ParseException exc) {
		}

		try {
			if (t.matches("^([0-9]+|[XIV]+)\\s*век\\s+до\\s+н\\.?\\s*э\\.?$")) {
				final String strCentury = StringUtils.substringBefore(t, "век").trim();
				int century = parseCentury(strCentury);
				return asCentury(property, -century);
			}
		} catch (NumberFormatException exc) {
		} catch (ParseException exc) {
		}

		try {
			if (t.matches("^\\[\\[\\s*([0-9]+|[XIV]+)\\s*век\\s+до\\s+н\\.?\\s*э\\.?\\s*\\]\\]$")) {
				final String strCentury = StringUtils.substringBetween(t, "[[", "век").trim();
				int century = parseCentury(strCentury);
				return asCentury(property, -century);
			}
		} catch (NumberFormatException exc) {
		} catch (ParseException exc) {
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
					// date is obviously is Julian
					timeValue.setTime(fixJulianToGrigorian(timeValue.getTime()));
					timeValue.setCalendarModel(TimeValue.CALENDAR_JULIAN);
				}
			}
			return ValueWithQualifiers.fromSnak(ApiSnak.newSnak(property, timeValue));
		} catch (DateTimeParseException exc) {
		}

		try {
			final DateTimeFormatter parser = PARSER_YEAR;
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
			return ValueWithQualifiers.fromSnak(ApiSnak.newSnak(property, timeValue));
		} catch (DateTimeParseException exc) {
		}

		throw new CantParseValueException(t);
	}

	private int parseCentury(final String strCentury) throws ParseException {
		if (StringUtils.isNumeric(strCentury)) {
			return Integer.parseInt(strCentury);
		} else {
			return ROMAN_CONVERTER.toNumber(strCentury);
		}
	}
}
