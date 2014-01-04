package org.wikipedia.vlsergey.secretary.utils;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

@Component
public class DateNormalizer {

	private static final Log log = LogFactory.getLog(DateNormalizer.class);

	public static final String NORMALIZED_DATE_LIKE_TEMPLATE = "%-%-%";

	private static final SimpleDateFormat sdf_ddMMMMMyyyy_EN = new SimpleDateFormat("dd MMMMM yyyy", Locale.US);

	private static final SimpleDateFormat sdf_ddMMMMMyyyy_RU = new SimpleDateFormat("dd MMMMM yyyy", Locales.RU_RU);

	private static final SimpleDateFormat sdf_ddMMMMMyyyy_UK = new SimpleDateFormat("dd MMMMM yyyy", Locales.UK_UK);

	private static final SimpleDateFormat sdf_ddMMyyyy = new SimpleDateFormat("dd.MM.yyyy", Locale.ROOT);

	private static final SimpleDateFormat sdf_MMMMMddyyyy_EN = new SimpleDateFormat("MMMMM dd, yyyy", Locale.US);

	private static final SimpleDateFormat yyyyMMMMdd = new SimpleDateFormat("yyyy-MM-dd");
	private static final SimpleDateFormat yyyyMMMMdd2 = new SimpleDateFormat("yyyy=MM-dd");
	private static final SimpleDateFormat yyyyMMMMdd3 = new SimpleDateFormat("yyyy-MM=dd");

	static {
		DateFormatSymbols ru = new DateFormatSymbols(Locales.RU_RU);
		ru.setMonths(new String[] { "января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа",
				"сентября", "октября", "ноября", "декабря" });

		DateFormatSymbols uk = new DateFormatSymbols(Locales.UK_UK);
		uk.setMonths(new String[] { "січня", "лютого", "березня", "квітня", "травня", "червня", "липня", "серпня",
				"вересня", "жовтня", "листопада", "грудня" });

		sdf_ddMMMMMyyyy_RU.setDateFormatSymbols(ru);
		sdf_ddMMMMMyyyy_UK.setDateFormatSymbols(uk);
	}

	public String normalizeDate(String originalNormilizedDate) {
		String nonNormilizedDate = originalNormilizedDate;
		if (StringUtils.isBlank(nonNormilizedDate)) {
			return StringUtils.EMPTY;
		}

		nonNormilizedDate = StringUtils.trimToEmpty(nonNormilizedDate);
		nonNormilizedDate = nonNormilizedDate.replaceAll("\\[", "");
		nonNormilizedDate = nonNormilizedDate.replaceAll("\\]", "");
		nonNormilizedDate = nonNormilizedDate.replaceAll("\t", " ");
		nonNormilizedDate = nonNormilizedDate.replaceAll("\r", " ");
		nonNormilizedDate = nonNormilizedDate.replaceAll("\n", " ");
		nonNormilizedDate = nonNormilizedDate.replaceAll("&nbsp;", " ");
		nonNormilizedDate = nonNormilizedDate.replaceAll("  ", " ");

		nonNormilizedDate = nonNormilizedDate.replaceAll("\\{\\{[Nn]obr\\|([^\\}\\|]*)\\}\\}", "$1");
		nonNormilizedDate = nonNormilizedDate.replaceAll("\\{\\{[Nn]owrap\\|([^\\}\\|]*)\\}\\}", "$1");
		nonNormilizedDate = nonNormilizedDate.replaceAll("\\{\\{[Ss]\\|([^\\}\\|]*)\\}\\}", "$1");

		nonNormilizedDate = nonNormilizedDate.replaceAll(
				"\\{\\{[Ss]tart date\\|([^\\}\\|]*)\\|([^\\}\\|]*)\\|([^\\}\\|]*)(\\|df=y)?\\}\\}", "$1-$2-$3");
		nonNormilizedDate = nonNormilizedDate.replaceAll(
				"\\{\\{[Dd]ate\\|([^\\}\\|]*)\\|([^\\}\\|]*)\\|([^\\}\\|]*)\\}\\}", "$3-$2-$1");
		nonNormilizedDate = nonNormilizedDate.replaceAll(
				"\\{\\{[Пп]роверено\\|([^\\}\\|]*)\\|([^\\}\\|]*)\\|([^\\}\\|]*)\\}\\}", "$3-$2-$1");

		if (nonNormilizedDate.matches("^\\s*[0-9][0-9][0-9][0-9]\\-[0-9][0-9]?\\-[0-9][0-9]?\\s*$")) {
			try {
				return yyyyMMMMdd.format(yyyyMMMMdd.parse(StringUtils.trim(nonNormilizedDate)));
			} catch (ParseException e) {
				return nonNormilizedDate;
			}
		}

		if (nonNormilizedDate.matches("^\\s*[0-9][0-9][0-9][0-9]\\=[0-9][0-9]?\\-[0-9][0-9]?\\s*$")) {
			try {
				return yyyyMMMMdd.format(yyyyMMMMdd2.parse(StringUtils.trim(nonNormilizedDate)));
			} catch (ParseException e) {
				return nonNormilizedDate;
			}
		}

		if (nonNormilizedDate.matches("^\\s*[0-9][0-9][0-9][0-9]\\-[0-9][0-9]?\\=[0-9][0-9]?\\s*$")) {
			try {
				return yyyyMMMMdd.format(yyyyMMMMdd3.parse(StringUtils.trim(nonNormilizedDate)));
			} catch (ParseException e) {
				return nonNormilizedDate;
			}
		}

		try {
			final Date parsed = sdf_ddMMyyyy.parse(nonNormilizedDate);
			final int parsedYear = 1900 + parsed.getYear();
			if (parsedYear >= 90 && parsedYear <= 99) {
				parsed.setYear(1900 + parsedYear - 1900);
			}
			if (parsedYear >= 0 && parsedYear < 16) {
				parsed.setYear(2000 + parsedYear - 1900);
			}
			return yyyyMMMMdd.format(parsed);
		} catch (ParseException exc) {
			// ignore
			log.trace("Unable to parse date '" + nonNormilizedDate + "' with 'dd.MM.yyyy' template from pos "
					+ exc.getErrorOffset());
		}

		try {
			return yyyyMMMMdd.format(sdf_ddMMMMMyyyy_EN.parse(nonNormilizedDate));
		} catch (ParseException exc) {
			// ignore
			log.trace("Unable to parse date '" + nonNormilizedDate + "' with 'dd MMMMM yyyy' (EN) template from pos "
					+ exc.getErrorOffset());
		}

		try {
			return yyyyMMMMdd.format(sdf_ddMMMMMyyyy_RU.parse(nonNormilizedDate));
		} catch (ParseException exc) {
			// ignore
			log.trace("Unable to parse date '" + nonNormilizedDate + "' with 'dd MMMMM yyyy' (RU) template from pos "
					+ exc.getErrorOffset());
		}

		try {
			return yyyyMMMMdd.format(sdf_ddMMMMMyyyy_UK.parse(nonNormilizedDate));
		} catch (ParseException exc) {
			// ignore
			log.trace("Unable to parse date '" + nonNormilizedDate + "' with 'dd MMMMM yyyy' (UK) template from pos "
					+ exc.getErrorOffset());
		}

		try {
			return yyyyMMMMdd.format(sdf_MMMMMddyyyy_EN.parse(nonNormilizedDate));
		} catch (ParseException exc) {
			// ignore
			log.trace("Unable to parse date '" + nonNormilizedDate + "' with 'MMMMM dd, yyyy' (EN) template from pos "
					+ exc.getErrorOffset());
		}

		log.warn("Unable to normilize date: " + nonNormilizedDate);
		return originalNormilizedDate;
	}
}
