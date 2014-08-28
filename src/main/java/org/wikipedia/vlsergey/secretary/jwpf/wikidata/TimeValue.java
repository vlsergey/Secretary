package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import java.util.Calendar;
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

	private static final String KEY_AFTER = "after";
	private static final String KEY_BEFORE = "before";
	private static final String KEY_CALENDAT_MODEL = "calendarmodel";
	private static final String KEY_PRECISION = "precision";
	private static final String KEY_TIME = "time";
	private static final String KEY_TIMEZONE = "timezone";

	protected TimeValue(JSONObject jsonObject) {
		super(jsonObject);
	}

	public long floor() throws Exception {
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		calendar.setTimeInMillis(getTime());

		int precision = getPrecision();
		if (precision < 14)
			calendar.set(Calendar.SECOND, 0);
		if (precision < 13)
			calendar.set(Calendar.MINUTE, 0);
		if (precision < 12)
			calendar.set(Calendar.HOUR_OF_DAY, 0);
		if (precision < 11)
			calendar.set(Calendar.DAY_OF_MONTH, 1);
		if (precision < 10)
			calendar.set(Calendar.MONTH, 0);

		if (precision < 9) {
			int year = calendar.get(Calendar.YEAR);
			int power = 9 - precision;
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
		return jsonObject.getInt(KEY_PRECISION);
	}

	public long getTime() throws Exception {
		String stringTime = jsonObject.getString("time");
		int precision = jsonObject.getInt(KEY_PRECISION);
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

	public void setPrecision(int value) {
		jsonObject.put(KEY_PRECISION, value);
	}

	public void setTimeString(String value) {
		jsonObject.put(KEY_TIME, value);
	}
}
