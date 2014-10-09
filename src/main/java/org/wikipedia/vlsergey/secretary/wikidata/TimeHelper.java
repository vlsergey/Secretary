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
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Snak;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Properties;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.SnakType;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.TimeValue;

import com.frequal.romannumerals.Converter;

@Component
public class TimeHelper extends AbstractHelper {

	private static final OffsetDateTime BORDER = OffsetDateTime.parse("1582-10-15T00:00:00Z");

	private static final DateTimeFormatter PARSER_DATE = DateTimeFormatter.ofPattern("d.M.u").withZone(ZoneOffset.UTC);

	private static final DateTimeFormatter PARSER_YEAR = DateTimeFormatter.ofPattern("u");

	private static final Converter ROMAN_CONVERTER = new Converter();

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
		return ValueWithQualifiers.fromSnak(Snak.newSnak(property, timeValue));
	}

	@Override
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

	private Integer getMonth(String group) {
		switch (group.toLowerCase()) {
		case "январь":
			return 1;
		case "февраль":
			return 2;
		case "март":
			return 3;
		case "апрель":
			return 4;
		case "май":
			return 5;
		case "июнь":
			return 6;
		case "июль":
			return 7;
		case "август":
			return 8;
		case "сентябрь":
			return 9;
		case "октябрь":
			return 10;
		case "ноябрь":
			return 11;
		case "декабрь":
			return 12;
		}
		return null;
	}

	private String normalizeWithPrefix(String t, String prefixRegexp, String normalizedPrefix) {
		if (t.matches("^(" + prefixRegexp + ")\\s*\\[\\[([0-9]+) год\\|\\2\\]\\]$")) {
			t = t.replaceAll("^(" + prefixRegexp + ")\\s*\\[\\[([0-9]+) год\\|\\2\\]\\]$", normalizedPrefix + " $2");
		}
		if (t.matches("^(" + prefixRegexp + ")\\s*\\[\\[([0-9]+) год\\|\\2 года\\]\\]$")) {
			t = t.replaceAll("^(" + prefixRegexp + ")\\s*\\[\\[([0-9]+) год\\|\\2 года\\]\\]$", normalizedPrefix
					+ " $2");
		}
		if (t.matches("^(" + prefixRegexp + ")\\s*\\[\\[([0-9]+)\\]\\]$")) {
			t = t.replaceAll("^(" + prefixRegexp + ") \\[\\[([0-9]+)\\]\\]$", normalizedPrefix + " $2");
		}
		if (t.matches("^(" + prefixRegexp + ")\\s*\\[\\[([0-9]+) год\\]\\]а$")) {
			t = t.replaceAll("^(" + prefixRegexp + ") \\[\\[([0-9]+) год\\]\\]а$", normalizedPrefix + " $2");
		}
		if (t.matches("^(" + prefixRegexp + ")\\s*([0-9]+)$")) {
			t = t.replaceAll("^(" + prefixRegexp + ")\\s*([0-9]+)$", normalizedPrefix + " $2");
		}
		return t;
	}

	public List<ValueWithQualifiers> parse(EntityId property, String t) {
		t = t.trim();

		if (UNKNOWN.contains(t.toLowerCase())) {
			return ValueWithQualifiers.fromSnak(Snak.newSnak(property, SnakType.somevalue));
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

		{
			t = normalizeWithPrefix(t, "около|ок\\.", "около");

			try {
				if (t.matches("^около ([0-9]+)$")) {
					final String strYear = t.replaceAll("^около ([0-9]+)$", "$1");

					TemporalAccessor temporalAccessor = PARSER_YEAR.parse(strYear);
					final TimeValue timeValue = new TimeValue(TimeValue.PRECISION_YEAR, temporalAccessor);
					return Collections.singletonList(new ValueWithQualifiers(Snak.newSnak(property, timeValue),
							CIRCUMSTANCES_CIRCA));
				}
			} catch (NumberFormatException exc) {
			}
		}

		{
			t = normalizeWithPrefix(t, "до", "до");
			try {
				if (t.matches("^до ([0-9]+)$")) {
					String strYear = t.replaceAll("^до ([0-9]+)$", "$1");
					strYear = "" + (Integer.parseInt(strYear) - 1);
					TemporalAccessor temporalAccessor = PARSER_YEAR.parse(strYear);
					final TimeValue timeValue = new TimeValue(TimeValue.PRECISION_YEAR, temporalAccessor);
					return Collections.singletonList(new ValueWithQualifiers(Snak.newSnak(property,
							SnakType.somevalue), Collections.singletonList(Snak.newSnak(Properties.LATEST_DATE,
							timeValue))));
				}
			} catch (NumberFormatException exc) {
			}
		}
		{
			t = normalizeWithPrefix(t, "не ранее", "не ранее");
			try {
				if (t.matches("^не ранее ([0-9]+)$")) {
					final String strYear = t.replaceAll("^не ранее ([0-9]+)$", "$1");
					TemporalAccessor temporalAccessor = PARSER_YEAR.parse(strYear);
					final TimeValue timeValue = new TimeValue(TimeValue.PRECISION_YEAR, temporalAccessor);
					return Collections.singletonList(new ValueWithQualifiers(Snak.newSnak(property,
							SnakType.somevalue), Collections.singletonList(Snak.newSnak(Properties.EARLIEST_DATE,
							timeValue))));
				}
			} catch (NumberFormatException exc) {
			}
		}
		{
			t = normalizeWithPrefix(t, "после", "после");
			try {
				if (t.matches("^после ([0-9]+)$")) {
					String strYear = t.replaceAll("^после ([0-9]+)$", "$1");
					strYear = "" + (Integer.parseInt(strYear) + 1);
					TemporalAccessor temporalAccessor = PARSER_YEAR.parse(strYear);
					final TimeValue timeValue = new TimeValue(TimeValue.PRECISION_YEAR, temporalAccessor);
					return Collections.singletonList(new ValueWithQualifiers(Snak.newSnak(property,
							SnakType.somevalue), Collections.singletonList(Snak.newSnak(Properties.EARLIEST_DATE,
							timeValue))));
				}
			} catch (NumberFormatException exc) {
			}
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
			TemporalAccessor parsed = PARSER_DATE.parse(t);
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
			return ValueWithQualifiers.fromSnak(Snak.newSnak(property, timeValue));
		} catch (DateTimeParseException exc) {
		}

		try {
			final Matcher matcher = Pattern.compile("^\\[\\[([а-я]+)\\]\\]\\s+\\[\\[([0-9]+)\\s+год\\]\\]а$")
					.matcher(t);
			if (matcher.matches()) {
				Integer month = getMonth(matcher.group(1));
				if (month != null) {
					t = "1." + month + "." + matcher.group(2);

					TemporalAccessor parsed = PARSER_DATE.parse(t);
					final TimeValue timeValue = new TimeValue(TimeValue.PRECISION_MONTH, parsed);
					if (appendJulian) {
						timeValue.setCalendarModel(TimeValue.CALENDAR_JULIAN);
					} else {
						OffsetDateTime dateTime = OffsetDateTime.from(timeValue.getTime());
						if (dateTime.isBefore(BORDER)) {
							timeValue.setCalendarModel(TimeValue.CALENDAR_JULIAN);
						}
					}
					return ValueWithQualifiers.fromSnak(Snak.newSnak(property, timeValue));
				}
			}
		} catch (DateTimeParseException exc) {
		}

		try {
			if (t.matches("^\\[\\[([0-9]+) год\\]\\] или \\[\\[([0-9]+) год\\]\\]$")) {
				String y1 = StringUtils.substringBetween(t, "[[", " год]] или [[");
				String y2 = StringUtils.substringBetween(t, " год]] или [[", " год]]");

				final TimeValue timeValue1 = new TimeValue(TimeValue.PRECISION_YEAR, PARSER_YEAR.parse(y1));
				final TimeValue timeValue2 = new TimeValue(TimeValue.PRECISION_YEAR, PARSER_YEAR.parse(y2));

				if (OffsetDateTime.from(timeValue1.getTime()).isBefore(BORDER)) {
					timeValue1.setCalendarModel(TimeValue.CALENDAR_JULIAN);
				}
				if (OffsetDateTime.from(timeValue2.getTime()).isBefore(BORDER)) {
					timeValue2.setCalendarModel(TimeValue.CALENDAR_JULIAN);
				}

				return Arrays.asList(
						new ValueWithQualifiers(Snak.newSnak(property, timeValue1), Collections.emptyList()),
						new ValueWithQualifiers(Snak.newSnak(property, timeValue2), Collections.emptyList()));
			}
		} catch (DateTimeParseException exc) {
		}

		try {
			final TimeValue timeValue = new TimeValue(TimeValue.PRECISION_YEAR, PARSER_YEAR.parse(t));
			if (appendJulian) {
				timeValue.setCalendarModel(TimeValue.CALENDAR_JULIAN);
			} else {
				if (OffsetDateTime.from(timeValue.getTime()).isBefore(BORDER)) {
					timeValue.setCalendarModel(TimeValue.CALENDAR_JULIAN);
				}
			}
			return ValueWithQualifiers.fromSnak(Snak.newSnak(property, timeValue));
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
