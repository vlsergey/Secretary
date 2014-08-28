package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.json.JSONObject;

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

	private static final String CALENDAR_GRIGORIAN = "http://www.wikidata.org/entity/Q1985727";

	private static final String KEY_AFTER = "after";
	private static final String KEY_BEFORE = "before";
	private static final String KEY_CALENDAT_MODEL = "calendarmodel";
	private static final String KEY_PRECISION = "precision";
	private static final String KEY_TIME = "time";
	private static final String KEY_TIMEZONE = "timezone";

	public static final int PRECISION_DAY = 11;
	public static final int PRECISION_HOUR = 12;
	public static final int PRECISION_MINUTE = 13;
	public static final int PRECISION_MONTH = 10;
	public static final int PRECISION_SECOND = 14;
	public static final int PRECISION_YEAR = 9;

	private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

	public static String toISO(Date date) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
		df.setTimeZone(UTC);
		String result = "+0000000" + df.format(new Date());
		return result;
	}

	protected TimeValue(int precision, Date date) {
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

	public long floor() throws Exception {
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		calendar.setTimeInMillis(getTime());

		int precision = getPrecision();
		if (precision < PRECISION_SECOND)
			calendar.set(Calendar.SECOND, 0);
		if (precision < PRECISION_MINUTE)
			calendar.set(Calendar.MINUTE, 0);
		if (precision < PRECISION_HOUR)
			calendar.set(Calendar.HOUR_OF_DAY, 0);
		if (precision < PRECISION_DAY)
			calendar.set(Calendar.DAY_OF_MONTH, 1);
		if (precision < PRECISION_MONTH)
			calendar.set(Calendar.MONTH, 0);

		if (precision < PRECISION_YEAR) {
			int year = calendar.get(Calendar.YEAR);
			int power = PRECISION_YEAR - precision;
			int multiplier = (int) Math.pow(10, power);
			if (year < 0) {
				year = (int) Math.ceil(year / multiplier) * multiplier;
			} else {
				year = (int) Math.floor((Math.abs(year) - 1) / multiplier) * multiplier + 1;
			}
			calendar.set(Calendar.YEAR, year);
		}
		return calendar.getTimeInMillis();
	}

	public int getPrecision() {
		return jsonObject.getJSONObject(KEY_VALUE).getInt(KEY_PRECISION);
	}

	public long getTime() throws Exception {
		String stringTime = getTimeString();
		String[] substrings = stringTime.split("(?<!\\A)[\\-\\:TZ]");

		// get the components of the date
		long year = Long.parseLong(substrings[0]);
		byte month = Byte.parseByte(substrings[1]);
		byte day = Byte.parseByte(substrings[2]);
		byte hour = Byte.parseByte(substrings[3]);
		byte minute = Byte.parseByte(substrings[4]);
		byte second = Byte.parseByte(substrings[5]);

		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		calendar.clear();
		calendar.set(Calendar.YEAR, (int) year);
		calendar.set(Calendar.MONTH, month - 1);
		calendar.set(Calendar.DAY_OF_MONTH, day);
		calendar.set(Calendar.HOUR_OF_DAY, hour);
		calendar.set(Calendar.MINUTE, minute);
		calendar.set(Calendar.SECOND, second);
		return calendar.getTimeInMillis();
	}

	public String getTimeString() {
		return jsonObject.getJSONObject(KEY_VALUE).getString(KEY_TIME);
	}

	@Override
	public int hashCode() {
		return getTimeString().hashCode();
	}

	public void setPrecision(int value) {
		jsonObject.getJSONObject(KEY_VALUE).put(KEY_PRECISION, value);

	}

	public void setTimeString(String value) {
		jsonObject.getJSONObject(KEY_VALUE).put(KEY_TIME, value);
	}
}
