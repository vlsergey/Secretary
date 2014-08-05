package org.wikipedia.vlsergey.secretary.jwpf.model;

import java.util.Locale;

public class Project {

	private static final Locale LOCALE_RUSSIA = new Locale("ru-RU");

	private static final Locale LOCALE_UKRAINE = new Locale("uk-UK");

	public static final Project RUWIKIPEDIA = new Project(ProjectType.wiki, LOCALE_RUSSIA);

	public static final Project RUWIKISOURCE = new Project(ProjectType.wikisource, LOCALE_RUSSIA);

	public static final Project UKWIKIPEDIA = new Project(ProjectType.wiki, LOCALE_UKRAINE);

	public static final Project UKWIKISOURCE = new Project(ProjectType.wikisource, LOCALE_UKRAINE);

	public static final Project WIKIDATA = new Project(ProjectType.wikidata, null, false);

	private static String getLanguageCode(Locale locale) {
		if (locale.getLanguage().equals(LOCALE_RUSSIA.getLanguage())) {
			return "ru";
		}
		if (locale.getLanguage().equals(LOCALE_UKRAINE.getLanguage())) {
			return "uk";
		}
		throw new IllegalArgumentException("Unknown locale: " + locale);
	}

	private final String code;

	private final String languageCode;

	private final Locale locale;

	private final boolean mainNamespaceHasXmlRepresentation;

	private final ProjectType type;

	private Project(ProjectType type, Locale locale) {
		super();
		this.type = type;
		this.locale = locale;
		this.mainNamespaceHasXmlRepresentation = true;

		if (locale == null) {
			this.languageCode = null;
			this.code = type.name();
		} else {
			this.languageCode = getLanguageCode(locale);
			this.code = this.languageCode + type.name();
		}
	}

	private Project(ProjectType type, Locale locale, boolean mainNamespaceHasXmlRepresentation) {
		super();
		this.type = type;
		this.locale = locale;
		this.mainNamespaceHasXmlRepresentation = mainNamespaceHasXmlRepresentation;

		if (locale == null) {
			this.languageCode = null;
			this.code = type.name();
		} else {
			this.languageCode = getLanguageCode(locale);
			this.code = this.languageCode + type.name();
		}
	}

	public String getCode() {
		return code;
	}

	public String getLanguageCode() {
		return languageCode;
	}

	public Locale getLocale() {
		return locale;
	}

	public ProjectType getType() {
		return type;
	}

	public boolean isMainNamespaceHasXmlRepresentation() {
		return mainNamespaceHasXmlRepresentation;
	}

	@Override
	public String toString() {
		return "Project[ " + code + "; " + locale + "; " + type + "]";
	}

}
