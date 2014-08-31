package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.wikipedia.vlsergey.secretary.dom.Content;
import org.wikipedia.vlsergey.secretary.dom.Text;

public class TimeValue extends DataValue {

	// "datavalue": {
	// "value": {
	// "time": "+00000001990-10-03T00:00:00Z",
	// "timezone": 0,
	// "before": 0,
	// "after": 0,
	// "precision": 11,
	// "calendarmodel": "http://www.wikidata.org/entity/Q1985727"
	// },
	// "type": "time"
	// }

	public static final String CALENDAR_GRIGORIAN = "http://www.wikidata.org/entity/Q1985727";
	public static final String CALENDAR_JULIAN = "http://www.wikidata.org/entity/Q1985786";

	private static final DateTimeFormatter formatDay = DateTimeFormatter.ofPattern("d LLLL uuuu GG");
	private static final DateTimeFormatter formatMonth = DateTimeFormatter.ofPattern("LLLL uuuu GG");
	private static final DateTimeFormatter formatter = DateTimeFormatter
			.ofPattern("uuuuuuuuuu'-'MM'-'dd'T'HH':'mm':'ssX");
	private static final DateTimeFormatter formatYear = DateTimeFormatter.ofPattern("uuuu GG");

	private static final String KEY_AFTER = "after";
	private static final String KEY_BEFORE = "before";
	private static final String KEY_CALENDAT_MODEL = "calendarmodel";
	private static final String KEY_PRECISION = "precision";
	private static final String KEY_TIME = "time";
	private static final String KEY_TIMEZONE = "timezone";

	private static final DateTimeFormatter parser = DateTimeFormatter
			.ofPattern("['+']uuuuuuuuuuu'-'MM'-'dd'T'HH':'mm':'ssX");

	public static final int PRECISION_DAY = 11;
	public static final int PRECISION_HOUR = 12;
	public static final int PRECISION_MINUTE = 13;
	public static final int PRECISION_MONTH = 10;
	public static final int PRECISION_SECOND = 14;
	public static final int PRECISION_YEAR = 9;

	private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

	public static TemporalAccessor fromISO(String date) {
		return parser.parse(date);
	}

	@Deprecated
	public static String toISO(Date date) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
		df.setTimeZone(UTC);
		String result = "+0000000" + df.format(new Date());
		return result;
	}

	public static String toISO(TemporalAccessor date) {

		OffsetDateTime dateTime = OffsetDateTime.parse("2000-01-01T00:00:00.00Z");
		dateTime = dateTime.with(ChronoField.YEAR, date.getLong(ChronoField.YEAR));
		for (ChronoField field : Arrays.asList(ChronoField.MONTH_OF_YEAR, ChronoField.DAY_OF_MONTH,
				ChronoField.HOUR_OF_DAY, ChronoField.MINUTE_OF_HOUR, ChronoField.SECOND_OF_MINUTE)) {
			if (date.isSupported(field)) {
				dateTime = dateTime.with(field, date.getLong(field));
			}
		}

		String result = formatter.format(dateTime);
		if (result.startsWith("-")) {
			result = "-0" + result.substring(1);
		} else {
			result = "+0" + result;
		}
		return result;
	}

	@Deprecated
	public TimeValue(int precision, Date date) {
		super(new JSONObject());

		jsonObject.put(KEY_TYPE, ValueType.time.toString());
		jsonObject.put(KEY_VALUE, new JSONObject());

		setTimeString(toISO(date));
		jsonObject.getJSONObject(KEY_VALUE).put(KEY_TIMEZONE, 0);
		jsonObject.getJSONObject(KEY_VALUE).put(KEY_BEFORE, 0);
		jsonObject.getJSONObject(KEY_VALUE).put(KEY_AFTER, 0);
		setPrecision(precision);
		setCalendarModel(CALENDAR_GRIGORIAN);
	}

	public TimeValue(int precision, TemporalAccessor date) {
		super(new JSONObject());

		jsonObject.put(KEY_TYPE, ValueType.time.toString());
		jsonObject.put(KEY_VALUE, new JSONObject());

		setTimeString(toISO(date));
		jsonObject.getJSONObject(KEY_VALUE).put(KEY_TIMEZONE, 0);
		jsonObject.getJSONObject(KEY_VALUE).put(KEY_BEFORE, 0);
		jsonObject.getJSONObject(KEY_VALUE).put(KEY_AFTER, 0);
		setPrecision(precision);
		jsonObject.getJSONObject(KEY_VALUE).put(KEY_CALENDAT_MODEL, CALENDAR_GRIGORIAN);
	}

	protected TimeValue(JSONObject jsonObject) {
		super(jsonObject);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof TimeValue))
			return false;

		TimeValue a = this;
		TimeValue b = (TimeValue) obj;
		try {
			if (a.getPrecision() == b.getPrecision()) {
				if (a.floor().equals(b.floor())) {
					if (a.getPrecision() < PRECISION_DAY) {
						return true;
					} else {
						return StringUtils.equals(a.getCalendarModel(), b.getCalendarModel());
					}
				}
			}
		} catch (Exception exc) {
		}

		return super.equals(obj);
	}

	public OffsetDateTime floor() {
		OffsetDateTime dateTime = OffsetDateTime.from(getTime());

		int precision = getPrecision();
		if (precision < PRECISION_SECOND)
			dateTime = dateTime.with(ChronoField.SECOND_OF_MINUTE, 0);
		if (precision < PRECISION_MINUTE)
			dateTime = dateTime.with(ChronoField.MINUTE_OF_HOUR, 0);
		if (precision < PRECISION_HOUR)
			dateTime = dateTime.with(ChronoField.HOUR_OF_DAY, 0);
		if (precision < PRECISION_DAY)
			dateTime = dateTime.with(ChronoField.DAY_OF_MONTH, 1);
		if (precision < PRECISION_MONTH)
			dateTime = dateTime.with(ChronoField.MONTH_OF_YEAR, 1);

		if (precision < PRECISION_YEAR) {
			int year = dateTime.get(ChronoField.YEAR);
			int power = PRECISION_YEAR - precision;
			int multiplier = (int) Math.pow(10, power);
			if (year < 0) {
				year = (int) Math.ceil(year / multiplier) * multiplier;
			} else {
				year = (int) Math.floor((Math.abs(year) - 1) / multiplier) * multiplier + 1;
			}
			dateTime = dateTime.with(ChronoField.YEAR, year);
		}
		return dateTime;
	}

	public int getAfter() {
		return jsonObject.getJSONObject(KEY_VALUE).getInt(KEY_AFTER);
	}

	public int getBefore() {
		return jsonObject.getJSONObject(KEY_VALUE).getInt(KEY_BEFORE);
	}

	public String getCalendarModel() {
		return jsonObject.getJSONObject(KEY_VALUE).getString(KEY_CALENDAT_MODEL);
	}

	public int getPrecision() {
		return jsonObject.getJSONObject(KEY_VALUE).getInt(KEY_PRECISION);
	}

	public TemporalAccessor getTime() {
		return fromISO(getTimeString());
	}

	public String getTimeString() {
		return jsonObject.getJSONObject(KEY_VALUE).getString(KEY_TIME);
	}

	public int getTimezone() {
		return jsonObject.getJSONObject(KEY_VALUE).getInt(KEY_TIMEZONE);
	}

	@Override
	public int hashCode() {
		return getTimeString().hashCode();
	}

	public void setCalendarModel(String value) {
		jsonObject.getJSONObject(KEY_VALUE).put(KEY_CALENDAT_MODEL, value);
	}

	public void setPrecision(int value) {
		jsonObject.getJSONObject(KEY_VALUE).put(KEY_PRECISION, value);

	}

	public void setTime(TemporalAccessor time) {
		setTimeString(toISO(time));
	}

	public void setTimeString(String value) {
		jsonObject.getJSONObject(KEY_VALUE).put(KEY_TIME, value);
	}

	@Override
	public Content toWiki() {

		try {
			final TemporalAccessor time = getTime();
			switch (getPrecision()) {
			case 9:
				return new Text(formatYear.format(time) + (getCalendarModel() == CALENDAR_GRIGORIAN ? "/G" : "/J"));
			case 10:
				return new Text(formatMonth.format(time) + (getCalendarModel() == CALENDAR_GRIGORIAN ? "/G" : "/J"));
			case 11:
				return new Text(formatDay.format(time) + (getCalendarModel() == CALENDAR_GRIGORIAN ? "/G" : "/J"));
			}
		} catch (Exception exc) {
		}

		return new Text(getTimeString() + (getCalendarModel() == CALENDAR_GRIGORIAN ? "/G" : "/J"));
	}
}
