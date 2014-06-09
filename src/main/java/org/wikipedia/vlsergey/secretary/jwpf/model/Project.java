package org.wikipedia.vlsergey.secretary.jwpf.model;

import java.util.Locale;

public class Project {

	public static final Project RUWIKI = new Project(ProjectType.wiki, new Locale("ru-RU"));

	public static final Project UKWIKI = new Project(ProjectType.wiki, new Locale("uk-UK"));

	public static final Project WIKIDATA = new Project(ProjectType.wikidata, null);

	private final String code;

	private final Locale locale;

	private final ProjectType type;

	private Project(ProjectType type, Locale locale) {
		super();
		this.type = type;
		this.locale = locale;

		if (locale == null) {
			this.code = type.name();
		} else {
			this.code = locale.getLanguage() + type.name();
		}

	}

	public String getCode() {
		return code;
	}

	public Locale getLocale() {
		return locale;
	}

	public ProjectType getType() {
		return type;
	}

}
