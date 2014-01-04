package org.wikipedia.vlsergey.secretary.trust;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.wikipedia.vlsergey.secretary.utils.StringUtils;

public class Month {

	public static final Month MONTH_OF_2013_06 = new Month(null, parseDateUnasafe("2013-06-01T00:00:00.000+0000"),
			"июне 2013 года");

	public static final Month MONTH_OF_2013_07 = new Month(MONTH_OF_2013_06,
			parseDateUnasafe("2013-07-01T00:00:00.000+0000"), "июле 2013 года");

	public static final Month MONTH_OF_2013_08 = new Month(MONTH_OF_2013_06,
			parseDateUnasafe("2013-08-01T00:00:00.000+0000"), "августе 2013 года");

	public static final Month[] MONTHES_ALL = { MONTH_OF_2013_06, MONTH_OF_2013_07, MONTH_OF_2013_08 };

	public static Date parseDateUnasafe(String date) {
		try {
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse(date);
		} catch (ParseException exc) {
			throw new ExceptionInInitializerError(exc);
		}
	}

	private final Date begin;

	private final Date end;

	private final String pageCountsFile;

	private final String prepositional;

	private final Month previous;

	private final String yearMinusMonth;

	public Month(Month previous, Date begin, String prepositional) {
		super();
		this.begin = begin;
		this.previous = previous;
		this.prepositional = prepositional;

		Calendar calendar = Calendar.getInstance();
		calendar.setTimeZone(TimeZone.getTimeZone("GMT+0"));
		calendar.setTime(begin);

		this.yearMinusMonth = StringUtils.leftPad("" + calendar.get(Calendar.YEAR), 4, '0') + "-"
				+ StringUtils.leftPad("" + (calendar.get(Calendar.MONTH) + 1), 2, '0');

		this.pageCountsFile = "stats/stats-" + yearMinusMonth + ".txt";

		calendar.add(Calendar.MONTH, 1);
		this.end = calendar.getTime();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Month other = (Month) obj;
		if (begin == null) {
			if (other.begin != null)
				return false;
		} else if (!begin.equals(other.begin))
			return false;
		return true;
	}

	public Date getEnd() {
		return end;
	}

	public String getPagesCountsFile() {
		return pageCountsFile;
	}

	public String getPrepositional() {
		return prepositional;
	}

	public Month getPrevious() {
		return previous;
	}

	public String getYearMinusMonth() {
		return yearMinusMonth;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((begin == null) ? 0 : begin.hashCode());
		return result;
	}

}
